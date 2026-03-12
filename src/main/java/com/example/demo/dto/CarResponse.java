package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarResponse {
    private Integer carId;
    private String name;
    private String brandName;
    private Integer brandId;
    private String categoryName;
    private Integer categoryId;
    private String ownerName;
    private Integer ownerId;
    private String color;
    private Integer year;
    private String licensePlate;
    private String description;
    private String locationCity;
    private String locationDistrict;
    private String status;
    private LocalDateTime createdAt;

    // Pricing
    private BigDecimal priceHour;
    private BigDecimal priceDay;
    private BigDecimal priceMonth;

    // Images
    private List<String> imageUrls;

    // Average rating
    private Double averageRating;
    private Integer totalFeedbacks;
}
