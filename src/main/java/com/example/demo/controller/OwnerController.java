package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.CarSchedule;
import com.example.demo.repository.CarScheduleRepository;
import com.example.demo.repository.CarRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class OwnerController {

    private final CarService carService;
    private final BookingService bookingService;
    private final CarScheduleRepository carScheduleRepository;
    private final CarRepository carRepository;

    @GetMapping("/cars")
    public ResponseEntity<List<CarResponse>> getMyCars(Authentication auth) {
        return ResponseEntity.ok(carService.getCarsByOwner(auth.getName()));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getMyCarBookings(Authentication auth) {
        return ResponseEntity.ok(bookingService.getBookingsByOwner(auth.getName()));
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenue(Authentication auth) {
        List<BookingResponse> bookings = bookingService.getBookingsByOwner(auth.getName());

        BigDecimal totalRevenue = bookings.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .map(BookingResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalBookings = bookings.size();
        long completedBookings = bookings.stream().filter(b -> "COMPLETED".equals(b.getStatus())).count();
        long pendingBookings = bookings.stream().filter(b -> "PENDING".equals(b.getStatus())).count();

        Map<String, Object> revenue = new HashMap<>();
        revenue.put("totalRevenue", totalRevenue);
        revenue.put("totalBookings", totalBookings);
        revenue.put("completedBookings", completedBookings);
        revenue.put("pendingBookings", pendingBookings);

        return ResponseEntity.ok(revenue);
    }

    @PostMapping("/cars/{carId}/schedules")
    public ResponseEntity<CarSchedule> addSchedule(@PathVariable Integer carId,
                                                     @RequestBody ScheduleRequest request,
                                                     Authentication auth) {
        var car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));
        if (!car.getOwner().getEmail().equals(auth.getName())) {
            throw new RuntimeException("Not your car");
        }

        CarSchedule schedule = CarSchedule.builder()
                .car(car)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : "AVAILABLE")
                .build();

        return ResponseEntity.ok(carScheduleRepository.save(schedule));
    }
}
