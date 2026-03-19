package com.example.demo.services;

import com.example.demo.dto.FeedbackRequest;
import com.example.demo.dto.FeedbackResponse;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Feedback;
import com.example.demo.entity.User;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.FeedbackRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public FeedbackResponse createFeedback(FeedbackRequest request, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Only allow feedback when booking is COMPLETED
        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new RuntimeException("You can only leave feedback for completed bookings");
        }

        // Check if customer is the booking owner
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("You can only leave feedback for your own bookings");
        }

        // Check if feedback already exists
        if (feedbackRepository.existsByBookingBookingId(booking.getBookingId())) {
            throw new RuntimeException("Feedback already submitted for this booking");
        }

        Feedback feedback = Feedback.builder()
                .booking(booking)
                .car(booking.getCar())
                .customer(customer)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        feedback = feedbackRepository.save(feedback);
        return mapToFeedbackResponse(feedback);
    }

    public List<FeedbackResponse> getFeedbacksByCarId(Integer carId) {
        return feedbackRepository.findByCarCarId(carId).stream()
                .map(this::mapToFeedbackResponse)
                .collect(Collectors.toList());
    }

    private FeedbackResponse mapToFeedbackResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .bookingId(feedback.getBooking().getBookingId())
                .carId(feedback.getCar().getCarId())
                .customerName(feedback.getCustomer().getFullName())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
