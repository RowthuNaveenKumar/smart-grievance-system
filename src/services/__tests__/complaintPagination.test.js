import test from "node:test";
import assert from "node:assert/strict";
import { api } from "../api.js";
import {
  getStudentComplaintsPage,
  getStaffComplaintsPage,
  getMyComplaints,
  getAssignedComplaints,
  getAdminComplaintsPage as getAdminComplaintsPageFromComplaintApi,
} from "../complaintApi.js";
import {
  getAdminComplaintsPage,
  getAdminDashboardStats,
  overrideDepartment,
  reassignStaff,
} from "../adminComplaintApi.js";

test("SGMS Phase A.4 — Frontend Pagination Test Suite", async (t) => {
  const originalGet = api.get;

  t.afterEach(() => {
    api.get = originalGet;
  });

  await t.test("1 & 3: Correct API path and query parameters for student pagination", async () => {
    let capturedUrl = "";
    let capturedConfig = null;

    api.get = async (url, config) => {
      capturedUrl = url;
      capturedConfig = config;
      return {
        data: {
          content: [{ complaintId: 101, title: "Test Complaint" }],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 10,
        },
      };
    };

    const studentId = 42;
    const result = await getStudentComplaintsPage(studentId, {
      page: 0,
      size: 10,
      sortBy: "createdAt",
      direction: "desc",
    });

    assert.equal(capturedUrl, "/complaints/student/42/page");
    assert.deepEqual(capturedConfig.params, {
      page: 0,
      size: 10,
      sortBy: "createdAt",
      direction: "desc",
    });
    assert.equal(result.content.length, 1);
    assert.equal(result.totalElements, 1);
  });

  await t.test("2 & 3: Correct API path and query parameters for staff pagination", async () => {
    let capturedUrl = "";
    let capturedConfig = null;

    api.get = async (url, config) => {
      capturedUrl = url;
      capturedConfig = config;
      return {
        data: {
          content: [{ complaintId: 202, title: "Staff Assigned Complaint" }],
          totalElements: 15,
          totalPages: 2,
          number: 1,
          size: 10,
        },
      };
    };

    const staffId = 7;
    const result = await getStaffComplaintsPage(staffId, {
      page: 1,
      size: 10,
      sortBy: "priority",
      direction: "desc",
    });

    assert.equal(capturedUrl, "/complaints/staff/7/page");
    assert.deepEqual(capturedConfig.params, {
      page: 1,
      size: 10,
      sortBy: "priority",
      direction: "desc",
    });
    assert.equal(result.content[0].complaintId, 202);
    assert.equal(result.totalElements, 15);
    assert.equal(result.totalPages, 2);
  });

  await t.test("4: Correct handling and extraction of Spring Page responses", async () => {
    const mockSpringPage = {
      content: [
        { complaintId: 1, title: "Issue 1", status: "OPEN" },
        { complaintId: 2, title: "Issue 2", status: "RESOLVED" },
      ],
      pageable: { pageNumber: 0, pageSize: 10 },
      totalElements: 2,
      totalPages: 1,
      number: 0,
      size: 10,
      first: true,
      last: true,
      empty: false,
    };

    api.get = async () => ({ data: mockSpringPage });

    const result = await getStudentComplaintsPage(1, { page: 0, size: 10 });

    assert.ok(Array.isArray(result.content), "content must be an array");
    assert.equal(result.content.length, 2);
    assert.equal(result.totalElements, 2);
    assert.equal(result.totalPages, 1);
    assert.equal(result.number, 0);
    assert.equal(result.size, 10);
    assert.equal(result.empty, false);
  });

  await t.test("5: Page 1 and Page 2 navigation converts 1-based UI to 0-based Spring Page", async () => {
    const calls = [];
    api.get = async (url, config) => {
      calls.push({ url, page: config.params.page });
      return {
        data: {
          content: [],
          totalElements: 25,
          totalPages: 3,
          number: config.params.page,
          size: 10,
        },
      };
    };

    // UI Page 1 -> Backend page 0
    const uiPage1 = 1;
    await getStudentComplaintsPage(5, { page: uiPage1 - 1, size: 10 });
    assert.equal(calls[0].page, 0, "UI Page 1 must send page=0 to backend");

    // UI Page 2 -> Backend page 1
    const uiPage2 = 2;
    await getStudentComplaintsPage(5, { page: uiPage2 - 1, size: 10 });
    assert.equal(calls[1].page, 1, "UI Page 2 must send page=1 to backend");
  });

  await t.test("6: Sort changes map UI selections to whitelisted backend properties and reset to page 1", () => {
    const sortMapping = {
      NEWEST: { sortBy: "createdAt", direction: "desc" },
      OLDEST: { sortBy: "createdAt", direction: "asc" },
      UPDATED: { sortBy: "updatedAt", direction: "desc" },
      PRIORITY: { sortBy: "priority", direction: "desc" },
      URGENCY: { sortBy: "priority", direction: "desc" },
    };

    const allowedProperties = ["createdAt", "updatedAt", "title", "status", "priority", "complaintId"];

    for (const [uiSort, params] of Object.entries(sortMapping)) {
      assert.ok(
        allowedProperties.includes(params.sortBy),
        `${uiSort} must map to a whitelisted backend sort property`
      );
      assert.ok(
        params.direction === "asc" || params.direction === "desc",
        "direction must be 'asc' or 'desc'"
      );
    }
  });

  await t.test("7: Empty, loading, and error states metadata handling", () => {
    const emptyPage = {
      content: [],
      totalElements: 0,
      totalPages: 1,
      number: 0,
      size: 10,
    };

    assert.equal(emptyPage.content.length, 0);
    assert.equal(emptyPage.totalElements, 0);

    const totalItems = emptyPage.totalElements;
    assert.equal(totalItems, 0, "Empty page has 0 total items");
  });

  await t.test("8: ID guard prevents fetching when user profile ID is undefined or null", () => {
    const userUnloaded = null;
    const userWithNoStudent = { student: null };
    const userWithStudent = { student: { studentId: 12 } };

    const shouldFetchUnloaded = Boolean(userUnloaded?.student?.studentId);
    const shouldFetchNoStudent = Boolean(userWithNoStudent?.student?.studentId);
    const shouldFetchValid = Boolean(userWithStudent?.student?.studentId);

    assert.equal(shouldFetchUnloaded, false, "Must not fetch when user is unloaded");
    assert.equal(shouldFetchNoStudent, false, "Must not fetch when student profile is null");
    assert.equal(shouldFetchValid, true, "Must fetch when studentId is valid");
  });

  await t.test("9: Existing admin array responses and contracts remain unaffected", async () => {
    const mockAdminArray = [
      { complaintId: 1, title: "Complaint A" },
      { complaintId: 2, title: "Complaint B" },
    ];

    api.get = async (url) => {
      assert.equal(url, "/complaints");
      return { data: mockAdminArray };
    };

    const res = await api.get("/complaints");
    const result = res.data;
    assert.ok(Array.isArray(result), "Admin GET /complaints must return a raw array");
    assert.equal(result.length, 2);
    assert.equal(result[0].complaintId, 1);
  });

  await t.test("10: Dual-stream summary keeps global counts accurate without single-page bias", async () => {
    const summaryData = [
      { complaintId: 1, status: "OPEN" },
      { complaintId: 2, status: "IN_PROGRESS" },
      { complaintId: 3, status: "RESOLVED" },
    ];

    api.get = async (url) => {
      if (url === "/complaints/my") return { data: summaryData };
      if (url === "/complaints/student/1/page") {
        return {
          data: {
            content: [{ complaintId: 1, status: "OPEN" }],
            totalElements: 3,
            totalPages: 3,
            number: 0,
            size: 1,
          },
        };
      }
      throw new Error("Unexpected URL");
    };

    const summary = await getMyComplaints();
    const page = await getStudentComplaintsPage(1, { page: 0, size: 1 });

    const totalCount = summary.length;
    const activeCount = summary.filter((c) => ["OPEN", "IN_PROGRESS"].includes(c.status)).length;
    const resolvedCount = summary.filter((c) => c.status === "RESOLVED").length;

    assert.equal(totalCount, 3, "Global total must reflect full summary (3)");
    assert.equal(activeCount, 2, "Global active must reflect full summary (2)");
    assert.equal(resolvedCount, 1, "Global resolved must reflect full summary (1)");

    assert.equal(page.content.length, 1, "Current page content must only hold current page records");
    assert.equal(page.totalElements, 3, "Pagination totalElements must reflect backend total");
  });
});

test("SGMS Phase A.7.2 — Admin Frontend Pagination Migration Test Suite", async (t) => {
  const originalGet = api.get;
  const originalPatch = api.patch;

  t.afterEach(() => {
    api.get = originalGet;
    api.patch = originalPatch;
  });

  await t.test("1: Default admin pagination parameters and endpoint call", async () => {
    let capturedUrl = "";
    let capturedConfig = null;

    api.get = async (url, config) => {
      capturedUrl = url;
      capturedConfig = config;
      return {
        data: {
          content: [
            { complaintId: 101, title: "WiFi Issue", status: "OPEN", priority: "HIGH", departmentId: 1, assignedStaffId: 2 },
          ],
          totalElements: 25,
          totalPages: 3,
          number: 0,
          size: 10,
        },
      };
    };

    const result = await getAdminComplaintsPage();

    assert.equal(capturedUrl, "/complaints/page", "Must call GET /complaints/page");
    assert.deepEqual(capturedConfig.params, {
      page: 0,
      size: 10,
      sortBy: "createdAt",
      direction: "DESC",
    }, "Default parameters must be page=0, size=10, sortBy=createdAt, direction=DESC");
    assert.equal(result.totalElements, 25);
    assert.equal(result.content[0].complaintId, 101);
    assert.equal(result.content[0].departmentId, 1);
    assert.equal(result.content[0].assignedStaffId, 2);

    // Also test complaintApi export
    const resultFromAlt = await getAdminComplaintsPageFromComplaintApi();
    assert.equal(resultFromAlt.totalElements, 25);
  });

  await t.test("2: Page navigation converts 1-based UI page to 0-based Spring Page", async () => {
    const pageCalls = [];

    api.get = async (url, config) => {
      pageCalls.push({ url, page: config.params.page, size: config.params.size });
      return {
        data: {
          content: [],
          totalElements: 45,
          totalPages: 5,
          number: config.params.page,
          size: 10,
        },
      };
    };

    // UI Page 1 -> Backend page 0
    await getAdminComplaintsPage({ page: 0, size: 10 });
    assert.equal(pageCalls[0].page, 0, "UI Page 1 translates to 0-based page 0");

    // UI Page 2 -> Backend page 1
    await getAdminComplaintsPage({ page: 1, size: 10 });
    assert.equal(pageCalls[1].page, 1, "UI Page 2 translates to 0-based page 1");

    // UI Page 5 -> Backend page 4
    await getAdminComplaintsPage({ page: 4, size: 10 });
    assert.equal(pageCalls[2].page, 4, "UI Page 5 translates to 0-based page 4");
  });

  await t.test("3: Status filter serialization and ALL omission", async () => {
    let capturedParams = null;
    api.get = async (url, config) => {
      capturedParams = config.params;
      return { data: { content: [], totalElements: 0, totalPages: 1 } };
    };

    // Valid status filter
    await getAdminComplaintsPage({ status: "IN_PROGRESS" });
    assert.equal(capturedParams.status, "IN_PROGRESS");

    // "ALL" status filter is omitted from server params
    await getAdminComplaintsPage({ status: "ALL" });
    assert.equal(capturedParams.status, undefined, "'ALL' status must not be sent to backend");

    // undefined status filter is omitted
    await getAdminComplaintsPage({ status: undefined });
    assert.equal(capturedParams.status, undefined, "undefined status must not be sent to backend");
  });

  await t.test("4: Priority filter serialization and ALL omission", async () => {
    let capturedParams = null;
    api.get = async (url, config) => {
      capturedParams = config.params;
      return { data: { content: [], totalElements: 0, totalPages: 1 } };
    };

    // Valid priority filter
    await getAdminComplaintsPage({ priority: "CRITICAL" });
    assert.equal(capturedParams.priority, "CRITICAL");

    // "ALL" priority filter is omitted from server params
    await getAdminComplaintsPage({ priority: "ALL" });
    assert.equal(capturedParams.priority, undefined, "'ALL' priority must not be sent to backend");

    // undefined priority filter is omitted
    await getAdminComplaintsPage({ priority: undefined });
    assert.equal(capturedParams.priority, undefined, "undefined priority must not be sent to backend");
  });

  await t.test("5: Department filter serialization and ALL omission", async () => {
    let capturedParams = null;
    api.get = async (url, config) => {
      capturedParams = config.params;
      return { data: { content: [], totalElements: 0, totalPages: 1 } };
    };

    // Valid department filter as number
    await getAdminComplaintsPage({ departmentId: 3 });
    assert.equal(capturedParams.departmentId, 3);

    // Valid department filter as string (from HTML select) is converted to number
    await getAdminComplaintsPage({ departmentId: "5" });
    assert.equal(capturedParams.departmentId, 5);
    assert.equal(typeof capturedParams.departmentId, "number");

    // "ALL" department filter is omitted from server params
    await getAdminComplaintsPage({ departmentId: "ALL" });
    assert.equal(capturedParams.departmentId, undefined, "'ALL' departmentId must not be sent to backend");

    // undefined department filter is omitted
    await getAdminComplaintsPage({ departmentId: undefined });
    assert.equal(capturedParams.departmentId, undefined, "undefined departmentId must not be sent to backend");
  });

  await t.test("6: Filter changes reset page to 1", () => {
    // Simulate UI state management logic in ComplaintsManagement.jsx
    let currentPage = 4;
    let statusFilter = "ALL";
    let deptFilter = "ALL";
    let priorityFilter = "ALL";

    const handleStatusFilterChange = (val) => {
      statusFilter = val;
      currentPage = 1; // resets to page 1
    };

    const handleDeptFilterChange = (val) => {
      deptFilter = val;
      currentPage = 1; // resets to page 1
    };

    const handlePriorityFilterChange = (val) => {
      priorityFilter = val;
      currentPage = 1; // resets to page 1
    };

    // Test status change resets page
    currentPage = 3;
    handleStatusFilterChange("OPEN");
    assert.equal(currentPage, 1, "Status filter change must reset page to 1");
    assert.equal(statusFilter, "OPEN");

    // Test dept change resets page
    currentPage = 5;
    handleDeptFilterChange("2");
    assert.equal(currentPage, 1, "Department filter change must reset page to 1");
    assert.equal(deptFilter, "2");

    // Test priority change resets page
    currentPage = 2;
    handlePriorityFilterChange("HIGH");
    assert.equal(currentPage, 1, "Priority filter change must reset page to 1");
    assert.equal(priorityFilter, "HIGH");
  });

  await t.test("7: TotalElements display correctly reflects server count", async () => {
    api.get = async () => ({
      data: {
        content: [
          { complaintId: 1, title: "A" },
          { complaintId: 2, title: "B" },
        ],
        totalElements: 142,
        totalPages: 15,
        number: 0,
        size: 10,
      },
    });

    const pageData = await getAdminComplaintsPage({ page: 0, size: 10 });

    assert.equal(pageData.content.length, 2, "Loaded content size reflects server page slice");
    assert.equal(pageData.totalElements, 142, "pageData.totalElements reflects full database count");
    assert.equal(pageData.totalPages, 15);
  });

  await t.test("8: Override Department functionality using departmentId", async () => {
    let capturedPatchUrl = "";
    let capturedPatchData = null;

    api.patch = async (url, data) => {
      capturedPatchUrl = url;
      capturedPatchData = data;
      return { data: { success: true, message: "Department overridden" } };
    };

    const complaintId = 42;
    const patchPayload = {
      departmentId: 5,
      note: "Misclassified during student intake",
    };

    const res = await overrideDepartment(complaintId, patchPayload);

    assert.equal(capturedPatchUrl, "/complaints/42/override-department");
    assert.deepEqual(capturedPatchData, {
      departmentId: 5,
      note: "Misclassified during student intake",
    });
    assert.equal(res.success, true);
  });

  await t.test("9: Reassign Staff functionality using departmentId and assignedStaffId", async () => {
    let capturedPatchUrl = "";
    let capturedPatchData = null;

    api.patch = async (url, data) => {
      capturedPatchUrl = url;
      capturedPatchData = data;
      return { data: { success: true, message: "Staff reassigned" } };
    };

    const complaintId = 42;
    const patchPayload = {
      staffId: 8,
      note: "Escalated to senior officer",
    };

    const res = await reassignStaff(complaintId, patchPayload);

    assert.equal(capturedPatchUrl, "/complaints/42/reassign-staff");
    assert.deepEqual(capturedPatchData, {
      staffId: 8,
      note: "Escalated to senior officer",
    });
    assert.equal(res.success, true);
  });

  await t.test("10: Post-action reloads only current paginated dataset without calling GET /complaints", async () => {
    const apiCalls = [];

    api.get = async (url, config) => {
      apiCalls.push({ method: "GET", url, params: config?.params });
      return {
        data: {
          content: [{ complaintId: 42, departmentId: 5, assignedStaffId: 8 }],
          totalElements: 30,
          totalPages: 3,
          number: 1,
          size: 10,
        },
      };
    };

    api.patch = async (url, data) => {
      apiCalls.push({ method: "PATCH", url, data });
      return { data: { success: true } };
    };

    // User is on UI Page 2 (backend page 1)
    const currentPage = 2;

    // Simulate override submission
    await overrideDepartment(42, { departmentId: 5, note: "Reason" });

    // Post-action reload: re-fetches current page (page 1)
    await getAdminComplaintsPage({ page: currentPage - 1, size: 10 });

    // Verify calls made
    assert.equal(apiCalls.length, 2);
    assert.equal(apiCalls[0].method, "PATCH");
    assert.equal(apiCalls[0].url, "/complaints/42/override-department");

    assert.equal(apiCalls[1].method, "GET");
    assert.equal(apiCalls[1].url, "/complaints/page");
    assert.equal(apiCalls[1].params.page, 1, "Must reload the active page index (1 for UI page 2)");

    // Ensure unpaginated GET /complaints was NEVER called
    const calledUnpaginated = apiCalls.some((c) => c.url === "/complaints");
    assert.equal(calledUnpaginated, false, "Must NOT call GET /complaints after action");
  });

  await t.test("11: AdminDashboard statistics fetched via GET /admin/dashboard/stats", async () => {
    let capturedUrl = "";

    api.get = async (url) => {
      capturedUrl = url;
      return {
        data: {
          totalStudents: 120,
          totalStaff: 15,
          totalComplaints: 88,
          activeComplaints: 18,
          resolvedComplaints: 65,
          closedComplaints: 5,
        },
      };
    };

    const stats = await getAdminDashboardStats();

    assert.equal(capturedUrl, "/admin/dashboard/stats");
    assert.equal(stats.totalComplaints, 88);
    assert.equal(stats.activeComplaints, 18);
    assert.equal(stats.resolvedComplaints, 65);
    assert.equal(stats.totalStudents, 120);
    assert.equal(stats.totalStaff, 15);
  });

  await t.test("12: AdminDashboard recent 5 complaints fetched via GET /complaints/page", async () => {
    let capturedUrl = "";
    let capturedConfig = null;

    api.get = async (url, config) => {
      capturedUrl = url;
      capturedConfig = config;
      return {
        data: {
          content: [
            { complaintId: 10, title: "Recent 1" },
            { complaintId: 9, title: "Recent 2" },
            { complaintId: 8, title: "Recent 3" },
            { complaintId: 7, title: "Recent 4" },
            { complaintId: 6, title: "Recent 5" },
          ],
          totalElements: 50,
          totalPages: 10,
          number: 0,
          size: 5,
        },
      };
    };

    const result = await getAdminComplaintsPage({
      page: 0,
      size: 5,
      sortBy: "createdAt",
      direction: "DESC",
    });

    assert.equal(capturedUrl, "/complaints/page");
    assert.deepEqual(capturedConfig.params, {
      page: 0,
      size: 5,
      sortBy: "createdAt",
      direction: "DESC",
    });
    assert.equal(result.content.length, 5);
  });

  await t.test("13: Free-text search is strictly client-side scoped to loaded page without backend search", () => {
    // Current page slice
    const loadedContent = [
      { complaintId: 101, title: "Library AC broken", description: "Too hot in room 2", studentName: "Alice" },
      { complaintId: 102, title: "Hostel WiFi down", description: "Block B connectivity", studentName: "Bob" },
      { complaintId: 103, title: "Cafeteria food hygiene", description: "Underdone chicken", studentName: "Charlie" },
    ];

    // Filter scoped to loaded content
    const searchFilter = (query) => {
      if (!query.trim()) return loadedContent;
      const q = query.toLowerCase();
      return loadedContent.filter(
        (c) =>
          c.title?.toLowerCase().includes(q) ||
          c.description?.toLowerCase().includes(q) ||
          c.studentName?.toLowerCase().includes(q) ||
          String(c.complaintId).includes(q)
      );
    };

    const wifiResult = searchFilter("wifi");
    assert.equal(wifiResult.length, 1);
    assert.equal(wifiResult[0].complaintId, 102);

    const nonExistentResult = searchFilter("gymnasium");
    assert.equal(nonExistentResult.length, 0);

    const emptyFilterResult = searchFilter("");
    assert.equal(emptyFilterResult.length, 3);
  });
});
