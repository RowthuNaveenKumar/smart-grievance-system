package com.sgms.sgms_backend.security;

import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.Role;
import com.sgms.sgms_backend.model.StaffInfo;
import com.sgms.sgms_backend.model.User;
import com.sgms.sgms_backend.repository.RoleRepository;
import com.sgms.sgms_backend.repository.StaffInfoRepository;
import com.sgms.sgms_backend.repository.UserRepository;
import com.sgms.sgms_backend.service.AdminManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Phase 2.4A: Platform Administrator Deactivation Concurrency Integration Tests")
class AdminLifecycleConcurrencyIntegrationTest {

    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private StaffInfoRepository staffRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    private Role adminRole;
    private final List<Long> createdStaffIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    private static final Long SYSTEM_ADMIN_ROLE_ID = 1L;
    private static final String SYSTEM_ADMIN_ROLE_NAME = "ADMIN";

    @BeforeEach
    void setUp() {
        adminRole = roleRepo.findById(SYSTEM_ADMIN_ROLE_ID)
                .orElseThrow(() -> new IllegalStateException("Platform administrator role (ID 1) not found in database"));

        // Ensure security context for test setup
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@sgms.test", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@sgms.test", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        // Clean up any test staff and users created during the tests
        for (Long staffId : createdStaffIds) {
            try {
                staffRepo.deleteById(staffId);
            } catch (Exception ignored) {}
        }
        for (Long userId : createdUserIds) {
            try {
                userRepo.deleteById(userId);
            } catch (Exception ignored) {}
        }

        // Guarantee baseline system administrator Naveen (staffId = 1) is enabled
        staffRepo.findById(1L).ifPresent(s -> {
            if (s.getUser() != null && !s.getUser().isEnabled()) {
                s.getUser().setEnabled(true);
                userRepo.save(s.getUser());
            }
        });

        SecurityContextHolder.clearContext();
    }

    private StaffInfo createTestAdminStaff(String emailPrefix) {
        String uniqueEmail = emailPrefix + "_" + System.nanoTime() + "@concurrency.test";

        User user = new User();
        user.setEmail(uniqueEmail);
        user.setPassword("concurrency_test_hash");
        user.setAccountType(AccountType.STAFF);
        user.setEnabled(true);
        user = userRepo.save(user);
        createdUserIds.add(Long.valueOf(user.getUserId()));

        StaffInfo staff = new StaffInfo();
        staff.setName("Concurrency Test Admin");
        staff.setEmail(uniqueEmail);
        staff.setUser(user);
        staff.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
        staff = staffRepo.save(staff);
        createdStaffIds.add(Long.valueOf(staff.getStaffId()));

        return staff;
    }

    @Test
    @DisplayName("CC1: Competing deactivation requests cannot both succeed when leaving zero active administrators")
    void testConcurrentDeactivations_neverLeavesZeroActiveAdmins() throws Exception {
        // Create 2 test administrators
        StaffInfo adminA = createTestAdminStaff("admin_conc_a");
        StaffInfo adminB = createTestAdminStaff("admin_conc_b");

        // Identify all currently active platform administrators using non-locking read
        List<StaffInfo> initialActiveAdmins = staffRepo.findActiveStaffByRole(SYSTEM_ADMIN_ROLE_ID, SYSTEM_ADMIN_ROLE_NAME);
        int totalActive = initialActiveAdmins.size();
        assertThat(totalActive).isGreaterThanOrEqualTo(2);

        // We launch parallel threads equal to the total active admin count.
        // Every active admin has one thread attempting to disable them simultaneously.
        int numThreads = totalActive;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedLastAdminCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        for (StaffInfo admin : initialActiveAdmins) {
            Long staffId = Long.valueOf(admin.getStaffId());
            executor.submit(() -> {
                try {
                    // Populate authentication context in the worker thread
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken("admin@sgms.test", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    );

                    startGate.await(); // Synchronize all threads to start at the exact same instant
                    adminManagementService.disableStaff(staffId);
                    successCount.incrementAndGet();
                } catch (ValidationException ve) {
                    if (ve.getMessage() != null && ve.getMessage().contains("Cannot disable the last active platform administrator account")) {
                        rejectedLastAdminCount.incrementAndGet();
                    } else {
                        unexpectedErrors.add(ve);
                    }
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    endGate.countDown();
                }
            });
        }

        // Release the start gate to trigger simultaneous execution
        startGate.countDown();

        boolean completedInTime = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completedInTime).as("All concurrent deactivations must complete without deadlock").isTrue();
        assertThat(unexpectedErrors).as("No unexpected database errors or deadlocks").isEmpty();

        // INVARIANT 1: At least one attempt MUST have been rejected because leaving 0 admins is forbidden
        assertThat(rejectedLastAdminCount.get())
                .as("At least one competing deactivation must be rejected by the last-admin guard")
                .isGreaterThanOrEqualTo(1);

        // INVARIANT 2: Final active admin count in the database MUST remain >= 1
        List<StaffInfo> finalActiveAdmins = staffRepo.findActiveStaffByRole(SYSTEM_ADMIN_ROLE_ID, SYSTEM_ADMIN_ROLE_NAME);
        assertThat(finalActiveAdmins.size())
                .as("The invariant that at least one platform administrator remains enabled must hold")
                .isGreaterThanOrEqualTo(1);

        // INVARIANT 3: Success count + rejected count must equal total threads
        assertThat(successCount.get() + rejectedLastAdminCount.get()).isEqualTo(numThreads);
    }

    @Test
    @DisplayName("CC2: Concurrent deactivation attempts on the same administrator account handle gracefully without corruption")
    void testConcurrentDeactivations_sameAdminAccount() throws Exception {
        StaffInfo admin = createTestAdminStaff("admin_same_account");
        Long targetStaffId = Long.valueOf(admin.getStaffId());

        int numThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);

        AtomicInteger completedCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken("admin@sgms.test", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    );

                    startGate.await();
                    adminManagementService.disableStaff(targetStaffId);
                    completedCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    SecurityContextHolder.clearContext();
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(errors).isEmpty();

        // The target user must be disabled
        StaffInfo updated = staffRepo.findById(targetStaffId).orElseThrow();
        assertThat(updated.getUser().isEnabled()).isFalse();

        // The invariant remains valid: at least 1 administrator remains enabled
        List<StaffInfo> remainingAdmins = staffRepo.findActiveStaffByRole(SYSTEM_ADMIN_ROLE_ID, SYSTEM_ADMIN_ROLE_NAME);
        assertThat(remainingAdmins.size()).isGreaterThanOrEqualTo(1);
    }
}
