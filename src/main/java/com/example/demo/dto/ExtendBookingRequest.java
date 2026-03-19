package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExtendBookingRequest {
    @NotNull(message = "New end date is required")
    private LocalDateTime newEndDate;
}
