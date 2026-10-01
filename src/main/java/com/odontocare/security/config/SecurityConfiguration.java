package com.odontocare.security.config;

import com.odontocare.shared.web.ApiProblems;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/system/installation", "/actuator/health").permitAll()
                .anyRequest().denyAll());
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> writeProblem(mapper, response,
                        HttpStatus.UNAUTHORIZED, "Acceso no autorizado", "Esta operación requiere autorización."))
                .accessDeniedHandler((request, response, exception) -> writeProblem(mapper, response,
                        HttpStatus.FORBIDDEN, "Acceso denegado", "No tienes permiso para esta operación.")));
        return http.build();
    }

    private void writeProblem(ObjectMapper mapper, HttpServletResponse response, HttpStatus status,
            String title, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json;charset=UTF-8");
        mapper.writeValue(response.getWriter(), ApiProblems.create(status, title, detail));
    }
}
