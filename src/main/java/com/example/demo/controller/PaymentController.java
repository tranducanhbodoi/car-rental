package com.example.demo.controller;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request,
                                                           Authentication auth) {
        return ResponseEntity.ok(paymentService.processPayment(request, auth.getName()));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable("bookingId") Integer bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    @GetMapping("/vnpay-url/{bookingId}")
    public ResponseEntity<Map<String, String>> createVNPayUrl(@PathVariable("bookingId") Integer bookingId,
                                                              jakarta.servlet.http.HttpServletRequest request) {
        String ipAddress = com.example.demo.config.VNPayConfig.getIpAddress(request);
        String url = paymentService.createVNPayPaymentUrl(bookingId, ipAddress);
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vnpay-verify")
    public ResponseEntity<String> verifyVNPay(@RequestBody Map<String, String> request) {
        String bookingIdStr = request.get("bookingId");
        String transactionId = request.get("transactionId");
        
        if (bookingIdStr == null || transactionId == null) {
            return ResponseEntity.badRequest().body("Missing parameters");
        }
        
        try {
            Integer bookingId = Integer.parseInt(bookingIdStr);
            paymentService.verifyVNPaySuccess(bookingId, transactionId);
            return ResponseEntity.ok("Success");
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid bookingId format");
        }
    }
}
