package com.sgms.sgms_backend;

import com.sgms.sgms_backend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SgmsBackendApplicationTests {

    @Autowired
    private JwtUtil jwtUtil;

	@Test
	void contextLoads() {
        assertThat(jwtUtil).isNotNull();
        assertThat(jwtUtil.getExpirationMs()).isEqualTo(1800000L);
	}

}
