package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private Integer feedbackId;
    private Integer bookingId;
    private Integer carId;
    private String customerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
