package com.example.demo.controller;

import com.example.demo.dto.FeedbackRequest;
import com.example.demo.dto.FeedbackResponse;
import com.example.demo.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackResponse> createFeedback(@Valid @RequestBody FeedbackRequest request,
                                                            Authentication auth) {
        return ResponseEntity.ok(feedbackService.createFeedback(request, auth.getName()));
    }

    @GetMapping("/car/{carId}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByCarId(@PathVariable Integer carId) {
        return ResponseEntity.ok(feedbackService.getFeedbacksByCarId(carId));
    }
}
