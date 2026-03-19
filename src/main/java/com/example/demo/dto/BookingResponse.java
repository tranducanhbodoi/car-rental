package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Integer bookingId;
    private Integer customerId;
    private String customerName;
    private Integer carId;
    private String carName;
    private String brandName;
    private String licensePlate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalPrice;
    private String bookingType;
    private String status;
    private LocalDateTime createdAt;
    private String paymentStatus;
    private boolean hasFeedback;
}
