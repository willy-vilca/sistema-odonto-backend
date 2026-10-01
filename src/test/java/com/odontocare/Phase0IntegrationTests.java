package com.odontocare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odontocare.installation.repository.InstallationProfileRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class Phase0IntegrationTests {
  @Autowired private MockMvc mvc;

  @Autowired private InstallationProfileRepository repository;

  @Test
  void installationComesFromPostgresqlAndOnlyExposesPublicMetadata() throws Exception {
    var profile = repository.findById((short) 1).orElseThrow();
    var response =
        mvc.perform(get("/api/v1/system/installation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value(profile.getDisplayName()))
            .andExpect(jsonPath("$.timeZone").value(profile.getTimeZone()))
            .andExpect(jsonPath("$.currency").value(profile.getCurrency()))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.createdAt").doesNotExist())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andReturn()
            .getResponse();
    assertThat(UUID.fromString(response.getHeader("X-Request-Id"))).isNotNull();
  }

  @Test
  void unimplementedAndManagementEndpointsAreNotPublic() throws Exception {
    var denied =
        mvc.perform(get("/api/v1/patients"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andReturn()
            .getResponse();
    assertThat(denied.getContentAsString()).contains(denied.getHeader("X-Request-Id"));
    mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
  }

  @Test
  void writesAreDeniedWithoutAuthorizationAndCsrfProtection() throws Exception {
    mvc.perform(
            post("/api/v1/system/installation")
                .contentType("application/json")
                .content("{\"displayName\":\"Otro consultorio\"}"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }
}
