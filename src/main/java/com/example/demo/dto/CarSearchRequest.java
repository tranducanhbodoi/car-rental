package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CarSearchRequest {
    private String city;
    private Integer brandId;
    private Integer categoryId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
