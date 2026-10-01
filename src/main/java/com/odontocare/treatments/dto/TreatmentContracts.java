package com.odontocare.treatments.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class TreatmentContracts {
  private TreatmentContracts() {}

  public record ItemRequest(
      UUID serviceId,
      @NotBlank @Size(max = 300) String description,
      Integer tooth,
      @NotNull @Min(1) @Max(100) Integer quantity,
      @NotNull @Min(1) @Max(100) Integer sessions,
      @NotNull @DecimalMin("0") @DecimalMax("99999999.99") @Digits(integer = 8, fraction = 2)
          BigDecimal unitPrice) {}

  public record PlanRequest(
      @NotNull UUID patientId,
      @NotNull UUID dentistId,
      @NotBlank @Size(max = 160) String title,
      @NotNull @Size(max = 4000) String conditions,
      @NotNull @Size(min = 1, max = 50) List<@Valid ItemRequest> items,
      @NotNull @PositiveOrZero Long version,
      @NotNull UUID requestKey) {}

  public record ActionRequest(
      @NotNull @PositiveOrZero Long version,
      @NotNull UUID requestKey,
      @NotBlank @Size(max = 500) String reason,
      @Size(max = 160) String acceptedBy,
      Boolean releaseUnperformed) {}

  public record AdditionalRequest(
      @NotNull @PositiveOrZero Long version,
      @NotNull UUID requestKey,
      @NotBlank @Size(max = 500) String reason,
      @NotNull @Valid ItemRequest item) {}

  public record PlanResponse(
      UUID id,
      UUID patientId,
      UUID dentistId,
      String dentistName,
      String patientName,
      String code,
      String title,
      String conditions,
      String currency,
      String status,
      String acceptedBy,
      Instant acceptedAt,
      BigDecimal originalTotal,
      BigDecimal currentDebt,
      long completedSessions,
      long totalSessions,
      long version) {}

  public record ItemResponse(
      UUID id,
      UUID planId,
      UUID serviceId,
      String serviceName,
      String description,
      Integer tooth,
      int quantity,
      int sessions,
      BigDecimal unitPrice,
      BigDecimal amount,
      long completedSessions,
      BigDecimal currentDebt,
      String label) {}

  public record SessionResponse(
      UUID id,
      UUID itemId,
      UUID encounterId,
      String description,
      int sessions,
      String actorName,
      Instant createdAt) {}

  public record OperationResponse(
      UUID id,
      UUID planId,
      String action,
      String reason,
      String actorName,
      String summary,
      Instant createdAt) {}
}
