package com.example.demo.services;

import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.BookingResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.CarRepository;
import com.example.demo.repository.CarPriceRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.CarScheduleRepository;
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
    private final CarScheduleRepository carScheduleRepository;

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found"));

        // Validate dates
        LocalDateTime now = LocalDateTime.now();
        // Allow a 5-minute grace period for "past" dates to handle latency/clock drift
        if (request.getStartDate().isBefore(now.minusMinutes(5))) {
            throw new RuntimeException("Không cho phép đặt xe trong quá khứ. Vui lòng chọn thời gian từ hiện tại trở đi.");
        }

        if (request.getEndDate().isBefore(request.getStartDate()) || request.getEndDate().isEqual(request.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }

        // Check for overlapping bookings (prevent double booking)
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                car.getCarId(), request.getStartDate(), request.getEndDate()
        );
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Khung giờ này xe đã có người đặt, yêu cầu chọn thời gian khác.");
        }

        // Check for overlapping UNAVAILABLE schedules
        List<CarSchedule> overlappingSchedules = carScheduleRepository.findOverlappingSchedules(
                car.getCarId(), request.getStartDate(), request.getEndDate()
        );
        if (!overlappingSchedules.isEmpty()) {
            throw new RuntimeException("Xe đang bận hoặc bảo trì trong khung giờ này, vui lòng chọn thời gian khác.");
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

        if (!"PENDING_PAYMENT".equals(booking.getStatus()) && !"CONFIRMED".equals(booking.getStatus())) {
            throw new RuntimeException("Only PENDING_PAYMENT or CONFIRMED bookings can be cancelled");
        }

        // Check 24h cancellation policy
        if (booking.getStartDate() != null) {
            long hoursUntilStart = java.time.Duration.between(java.time.LocalDateTime.now(), booking.getStartDate()).toHours();
            if (hoursUntilStart < 24) {
                throw new RuntimeException("Không thể hủy đơn khi thời gian bắt đầu thuê xe còn dưới 24 giờ");
            }
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
        
        if (!"IN_PROGRESS".equals(booking.getStatus()) && !"CONFIRMED".equals(booking.getStatus())) {
            throw new RuntimeException("Only IN_PROGRESS or CONFIRMED bookings can be completed");
        }

        booking.setStatus("COMPLETED");
        bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    @Transactional
    public BookingResponse handoverBooking(Integer bookingId, String ownerEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCar().getOwner().getEmail().equals(ownerEmail)) {
            throw new RuntimeException("Only car owner can confirm handover");
        }

        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new RuntimeException("Only CONFIRMED bookings can be handed over");
        }

        if (booking.getPayment() == null || !"COMPLETED".equals(booking.getPayment().getPaymentStatus())) {
            throw new RuntimeException("Khách hàng chưa thanh toán, không thể giao xe");
        }

        booking.setStatus("IN_PROGRESS");
        bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    // Auto-cancel pending bookings older than 30 minutes
    @Transactional
    public BookingResponse extendBooking(Integer bookingId, com.example.demo.dto.ExtendBookingRequest request, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomer().getEmail().equals(userEmail)) {
            throw new RuntimeException("You can only extend your own bookings");
        }

        if (request.getNewEndDate().isBefore(booking.getEndDate()) || request.getNewEndDate().isEqual(booking.getEndDate())) {
            throw new RuntimeException("New end date must be after current end date");
        }

        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                booking.getCar().getCarId(), booking.getEndDate(), request.getNewEndDate()
        );
        boolean conflict = overlapping.stream().anyMatch(b -> !b.getBookingId().equals(bookingId));
        if (conflict) {
            throw new RuntimeException("Khung giờ này xe đã có người đặt, yêu cầu chọn thời gian khác.");
        }

        booking.setEndDate(request.getNewEndDate());
        
        BookingRequest tempReq = new BookingRequest();
        tempReq.setStartDate(booking.getStartDate());
        tempReq.setEndDate(booking.getEndDate());
        tempReq.setBookingType(booking.getBookingType());
        booking.setTotalPrice(calculateTotalPrice(booking.getCar(), tempReq));

        bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    @Transactional
    public BookingResponse rejectBooking(Integer bookingId, String ownerEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCar().getOwner().getEmail().equals(ownerEmail)) {
            throw new RuntimeException("Only car owner can reject bookings");
        }

        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new RuntimeException("Only PENDING_PAYMENT bookings can be rejected");
        }

        booking.setStatus("REJECTED");
        bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    @Transactional
    public BookingResponse createOfflineBooking(com.example.demo.dto.OfflineBookingRequest request, String ownerEmail) {
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found"));
        
        if (!car.getOwner().getEmail().equals(ownerEmail)) {
            throw new RuntimeException("Only car owner can create offline bookings for their car");
        }

        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                car.getCarId(), request.getStartDate(), request.getEndDate()
        );
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Khung giờ này xe đã có người đặt, yêu cầu chọn thời gian khác.");
        }

        String offlineEmail = request.getCustomerPhone() + "@offline.com";
        User customer = userRepository.findByEmail(offlineEmail).orElseGet(() -> {
            User newUser = User.builder()
                .email(offlineEmail)
                .fullName(request.getCustomerName())
                .password("OFFLINE")
                .phone(request.getCustomerPhone())
                .build();
            return userRepository.save(newUser);
        });

        BookingRequest tempReq = new BookingRequest();
        tempReq.setStartDate(request.getStartDate());
        tempReq.setEndDate(request.getEndDate());
        tempReq.setBookingType(request.getBookingType() != null ? request.getBookingType() : "DAY");
        BigDecimal totalPrice = calculateTotalPrice(car, tempReq);

        Booking booking = Booking.builder()
                .customer(customer)
                .car(car)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalPrice(totalPrice)
                .bookingType(tempReq.getBookingType())
                .status("COMPLETED")
                .build();

        booking = bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    // Auto-cancel pending bookings older than 15 minutes
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void autoCancelExpiredBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
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
                .hasFeedback(booking.getFeedback() != null)
                .build();
    }
}
