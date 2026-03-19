package com.example.demo.services;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Payment;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String customerEmail) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomer().getEmail().equals(customerEmail)) {
            throw new RuntimeException("You can only pay for your own bookings");
        }

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new RuntimeException("Cannot pay for a cancelled booking");
        }

        // Check if payment already exists
        if (paymentRepository.findByBookingBookingId(booking.getBookingId()).isPresent()) {
            throw new RuntimeException("Payment already processed for this booking");
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalPrice())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("COMPLETED")
                .transactionId(UUID.randomUUID().toString())
                .build();

        payment = paymentRepository.save(payment);

        // Update booking status to CONFIRMED after payment
        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);

        return mapToPaymentResponse(payment);
    }

    public PaymentResponse getPaymentByBookingId(Integer bookingId) {
        Payment payment = paymentRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found for booking: " + bookingId));
        return mapToPaymentResponse(payment);
    }

    @Transactional
    public String createVNPayPaymentUrl(Integer bookingId, String ipAddress) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"PENDING_PAYMENT".equals(booking.getStatus()) && !"CONFIRMED".equals(booking.getStatus())) {
            throw new RuntimeException("Chỉ đơn hàng chờ duyệt hoặc đã duyệt mới có thể thanh toán");
        }

        if (paymentRepository.findByBookingBookingId(bookingId).isPresent()) {
            throw new RuntimeException("Đơn hàng này đã được thanh toán rồi");
        }

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = "Thanh toan don hang: " + bookingId;
        String vnp_OrderType = "other";
        String vnp_TxnRef = String.valueOf(bookingId) + "_" + System.currentTimeMillis();
        String vnp_IpAddr = ipAddress;
        String vnp_TmnCode = com.example.demo.config.VNPayConfig.vnp_TmnCode;

        long amount = (long)(booking.getTotalPrice().doubleValue() * 100); 
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", com.example.demo.config.VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        java.util.Calendar cld = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Etc/GMT+7"));
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(java.util.Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII.toString()));
                    //Build query
                    query.append(java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {}
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = com.example.demo.config.VNPayConfig.hmacSHA512(com.example.demo.config.VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return com.example.demo.config.VNPayConfig.vnp_PayUrl + "?" + queryUrl;
    }

    @Transactional
    public void verifyVNPaySuccess(Integer bookingId, String transactionId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!paymentRepository.findByBookingBookingId(bookingId).isPresent()) {
            Payment payment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getTotalPrice())
                    .paymentMethod("VNPAY")
                    .paymentStatus("COMPLETED")
                    .transactionId(transactionId)
                    .build();
            paymentRepository.save(payment);

            booking.setStatus("CONFIRMED");
            bookingRepository.save(booking);
        }
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBooking().getBookingId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
