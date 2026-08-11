package com.shiftscheduler.server.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShiftCreateRequest(
        @NotBlank @Size(max = 100) String employee_name,
        @NotBlank String shift_date,
        @NotBlank String start_time,
        @NotBlank String end_time,
        @NotBlank @Size(max = 100) String role
) {
}
