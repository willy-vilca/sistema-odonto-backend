package com.odontocare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odontocare.installation.repository.InstallationProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ApiFailureIntegrationTests {
  @Autowired private MockMvc mvc;

  @MockitoBean private InstallationProfileRepository repository;

  @Test
  void databaseFailureReturnsRecoverableProblemWithoutInternalDetails() throws Exception {
    when(repository.findById((short) 1))
        .thenThrow(
            new DataAccessResourceFailureException("SQL internal detail; password=do-not-expose"));
    var response =
        mvc.perform(get("/api/v1/system/installation"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString()).doesNotContain("SQL", "password", "do-not-expose");
    assertThat(response.getContentAsString()).contains(response.getHeader("X-Request-Id"));
  }

  @Test
  void unexpectedFailureIsAlsoSanitized() throws Exception {
    when(repository.findById((short) 1))
        .thenThrow(new IllegalStateException("private internal detail"));
    var response =
        mvc.perform(get("/api/v1/system/installation"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString()).doesNotContain("private internal detail");
  }
}
