package com.example.demo.service;

import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.BookingResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.CarRepository;
import com.example.demo.repository.CarPriceRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CarRepository carRepository;
    private final CarPriceRepository carPriceRepository;
    private final UserRepository userRepository;

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found"));

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate()) || request.getEndDate().isEqual(request.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }

        // Check for overlapping bookings (prevent double booking)
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                car.getCarId(), request.getStartDate(), request.getEndDate()
        );
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Car is not available for the selected dates. There is a conflicting booking.");
        }

        // Calculate total price
        BigDecimal totalPrice = calculateTotalPrice(car, request);

        Booking booking = Booking.builder()
                .customer(customer)
                .car(car)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalPrice(totalPrice)
                .bookingType(request.getBookingType() != null ? request.getBookingType() : "DAY")
                .build();

        booking = bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    public List<BookingResponse> getMyBookings(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return bookingRepository.findByCustomer(customer).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByOwner(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));
        return bookingRepository.findByCarOwnerId(owner.getId()).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse cancelBooking(Integer bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomer().getEmail().equals(userEmail)) {
            throw new RuntimeException("You can only cancel your own bookings");
        }

        if (!"PENDING".equals(booking.getStatus()) && !"CONFIRMED".equals(booking.getStatus())) {
            throw new RuntimeException("Only PENDING or CONFIRMED bookings can be cancelled");
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    @Transactional
    public BookingResponse confirmBooking(Integer bookingId, String ownerEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCar().getOwner().getEmail().equals(ownerEmail)) {
            throw new RuntimeException("Only car owner can confirm bookings");
        }

        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    @Transactional
    public BookingResponse completeBooking(Integer bookingId, String ownerEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCar().getOwner().getEmail().equals(ownerEmail)) {
            throw new RuntimeException("Only car owner can complete bookings");
        }

        booking.setStatus("COMPLETED");
        bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    // Auto-cancel pending bookings older than 30 minutes
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void autoCancelExpiredBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<Booking> expiredBookings = bookingRepository.findPendingBookingsOlderThan(cutoff);
        for (Booking booking : expiredBookings) {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
        }
    }

    private BigDecimal calculateTotalPrice(Car car, BookingRequest request) {
        CarPrice carPrice = car.getCarPrice();
        if (carPrice == null) {
            throw new RuntimeException("Car pricing not set");
        }

        String bookingType = request.getBookingType() != null ? request.getBookingType() : "DAY";

        switch (bookingType.toUpperCase()) {
            case "HOUR":
                long hours = ChronoUnit.HOURS.between(request.getStartDate(), request.getEndDate());
                if (hours < 1) hours = 1;
                return carPrice.getPriceHour().multiply(BigDecimal.valueOf(hours));
            case "MONTH":
                long months = ChronoUnit.MONTHS.between(request.getStartDate(), request.getEndDate());
                if (months < 1) months = 1;
                return carPrice.getPriceMonth().multiply(BigDecimal.valueOf(months));
            case "DAY":
            default:
                long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
                if (days < 1) days = 1;
                return carPrice.getPriceDay().multiply(BigDecimal.valueOf(days));
        }
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .customerId(booking.getCustomer().getId())
                .customerName(booking.getCustomer().getFullName())
                .carId(booking.getCar().getCarId())
                .carName(booking.getCar().getName())
                .brandName(booking.getCar().getBrand().getName())
                .licensePlate(booking.getCar().getLicensePlate())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .totalPrice(booking.getTotalPrice())
                .bookingType(booking.getBookingType())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .paymentStatus(booking.getPayment() != null ? booking.getPayment().getPaymentStatus() : "NOT_PAID")
                .build();
    }
}
