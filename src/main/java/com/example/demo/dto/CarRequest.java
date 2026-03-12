package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CarRequest {
    @NotNull(message = "Brand ID is required")
    private Integer brandId;

    @NotNull(message = "Category ID is required")
    private Integer categoryId;

    @NotBlank(message = "Car name is required")
    private String name;

    private String color;
    private Integer year;

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    private String description;
    private String locationCity;
    private String locationDistrict;

    // Pricing
    private BigDecimal priceHour;
    private BigDecimal priceDay;
    private BigDecimal priceMonth;

    // Image URLs
    private List<String> imageUrls;
}
