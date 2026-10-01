package com.odontocare.appointments.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentRequest(
    @NotNull UUID patientId,
    @NotNull UUID dentistId,
    UUID serviceId,
    @NotNull @Size(max = 160) String reason,
    @Min(1) @Max(1440) Integer durationMinutes,
    @NotNull LocalDateTime localStart,
    @NotNull @Size(max = 1000) String notes,
    @NotNull UUID requestKey) {}
