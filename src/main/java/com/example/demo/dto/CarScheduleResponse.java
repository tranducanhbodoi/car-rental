package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CarScheduleResponse {
    private Integer scheduleId;
    private Integer carId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
}
