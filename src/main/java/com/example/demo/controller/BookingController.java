package com.example.demo.controller;

import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.BookingResponse;
import com.example.demo.services.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request,
                                                          Authentication auth) {
        return ResponseEntity.ok(bookingService.createBooking(request, auth.getName()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication auth) {
        return ResponseEntity.ok(bookingService.getMyBookings(auth.getName()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable("id") Integer id, Authentication auth) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, auth.getName()));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable("id") Integer id, Authentication auth) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, auth.getName()));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<BookingResponse> completeBooking(@PathVariable("id") Integer id, Authentication auth) {
        return ResponseEntity.ok(bookingService.completeBooking(id, auth.getName()));
    }

    @PatchMapping("/{id}/extend")
    public ResponseEntity<BookingResponse> extendBooking(@PathVariable("id") Integer id,
                                                         @Valid @RequestBody com.example.demo.dto.ExtendBookingRequest request,
                                                         Authentication auth) {
        return ResponseEntity.ok(bookingService.extendBooking(id, request, auth.getName()));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<BookingResponse> rejectBooking(@PathVariable("id") Integer id, Authentication auth) {
        return ResponseEntity.ok(bookingService.rejectBooking(id, auth.getName()));
    }

    @PostMapping("/offline")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BookingResponse> createOfflineBooking(@Valid @RequestBody com.example.demo.dto.OfflineBookingRequest request,
                                                                 Authentication auth) {
        return ResponseEntity.ok(bookingService.createOfflineBooking(request, auth.getName()));
    }
}
