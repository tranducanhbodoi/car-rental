package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.CarSchedule;
import com.example.demo.repository.CarScheduleRepository;
import com.example.demo.repository.CarRepository;
import com.example.demo.services.BookingService;
import com.example.demo.services.CarService;
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
        long pendingBookings = bookings.stream().filter(b -> "PENDING_PAYMENT".equals(b.getStatus())).count();

        Map<String, Object> revenue = new HashMap<>();
        revenue.put("totalRevenue", totalRevenue);
        revenue.put("totalBookings", totalBookings);
        revenue.put("completedBookings", completedBookings);
        revenue.put("pendingBookings", pendingBookings);

        return ResponseEntity.ok(revenue);
    }

    @PostMapping("/cars/{carId}/schedules")
    public ResponseEntity<com.example.demo.dto.CarScheduleResponse> addSchedule(@PathVariable("carId") Integer carId,
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
                .status(request.getStatus() != null ? request.getStatus() : "UNAVAILABLE")
                .build();

        // Validate dates
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new RuntimeException("Ngày bắt đầu và kết thúc không được để trống");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        // Check for overlapping schedules
        List<CarSchedule> existingSchedules = carScheduleRepository.findByCarCarId(carId);
        for (CarSchedule existing : existingSchedules) {
            if (request.getStartDate().isBefore(existing.getEndDate()) && request.getEndDate().isAfter(existing.getStartDate())) {
                throw new RuntimeException("Khoảng thời gian bị trùng với lịch hiện tại (" +
                    existing.getStartDate().toString().replace("T", " ") + " - " +
                    existing.getEndDate().toString().replace("T", " ") + ")");
            }
        }

        schedule = carScheduleRepository.save(schedule);
        
        return ResponseEntity.ok(com.example.demo.dto.CarScheduleResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .carId(car.getCarId())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .status(schedule.getStatus())
                .build());
    }

    @GetMapping("/cars/{carId}/schedules")
    public ResponseEntity<List<com.example.demo.dto.CarScheduleResponse>> getSchedules(@PathVariable("carId") Integer carId, Authentication auth) {
        var car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));
        if (!car.getOwner().getEmail().equals(auth.getName())) {
            throw new RuntimeException("Not your car");
        }
        
        List<com.example.demo.dto.CarScheduleResponse> responses = carScheduleRepository.findByCarCarId(carId).stream().map(s -> 
            com.example.demo.dto.CarScheduleResponse.builder()
                .scheduleId(s.getScheduleId())
                .carId(s.getCar().getCarId())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(s.getStatus())
                .build()
        ).toList();
        
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<?> deleteSchedule(@PathVariable("scheduleId") Integer scheduleId, Authentication auth) {
        CarSchedule schedule = carScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        
        if (!schedule.getCar().getOwner().getEmail().equals(auth.getName())) {
            throw new RuntimeException("Not your schedule");
        }
        
        carScheduleRepository.delete(schedule);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bookings/{bookingId}/handover")
    public ResponseEntity<BookingResponse> handoverCar(@PathVariable("bookingId") Integer bookingId, Authentication auth) {
        return ResponseEntity.ok(bookingService.handoverBooking(bookingId, auth.getName()));
    }
}
