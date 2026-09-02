package com.api.manojmobiles.service;

import com.api.manojmobiles.config.PineLabsConfig;
import com.api.manojmobiles.entity.Order;
import com.api.manojmobiles.entity.Payment;
import com.api.manojmobiles.entity.enums.OrderStatus;
import com.api.manojmobiles.entity.enums.PaymentStatus;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.repository.OrderRepository;
import com.api.manojmobiles.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PineLabsPaymentService {

    private final PineLabsConfig pineLabsConfig;
    private final RestTemplate pineLabsRestTemplate;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Creates a Pine Labs order and returns the checkout redirect URL.
     * Amount is sent in paisa (INR smallest unit) as per Pine Labs API spec.
     */
    public String createPaymentOrder(Order order, Payment payment, String returnUrl) {
        try {
            String token = pineLabsConfig.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Request-ID", java.util.UUID.randomUUID().toString());
            headers.set("Request-Timestamp", java.time.Instant.now().toString());

            // Pine Labs expects amount in smallest currency unit (paisa for INR)
            int amountInPaisa = order.getTotalAmount()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .intValue();

            Map<String, Object> orderAmount = new HashMap<>();
            orderAmount.put("value", amountInPaisa);
            orderAmount.put("currency", "INR");

            Map<String, Object> body = new HashMap<>();
            body.put("merchant_order_reference", order.getOrderNumber());
            body.put("order_amount", orderAmount);
            if (returnUrl != null && !returnUrl.isEmpty()) {
                body.put("return_url", returnUrl);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = pineLabsRestTemplate.exchange(
                    pineLabsConfig.getBaseUrl() + "/api/pay/v1/orders",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BadRequestException("Empty response from Pine Labs");
            }

            String plOrderId = responseBody.get("order_id") != null 
                    ? responseBody.get("order_id").toString() : null;
            String redirectUrl = responseBody.get("redirect_url") != null 
                    ? responseBody.get("redirect_url").toString() : null;

            // Store the Pine Labs order ID in our Payment entity
            if (plOrderId != null) {
                payment.setPgTransactionId(plOrderId);
                paymentRepository.save(payment);
            }

            log.info("Pine Labs order created: {} for order {}, redirectUrl: {}",
                    plOrderId, order.getOrderNumber(), redirectUrl);

            return redirectUrl;

        } catch (Exception e) {
            log.error("Failed to create Pine Labs payment for order {}: {}",
                    order.getOrderNumber(), e.getMessage(), e);
            throw new BadRequestException("Payment gateway error: " + e.getMessage());
        }
    }

    /**
     * Fetches the current payment status from Pine Labs and updates our database accordingly.
     * Called by the webhook controller or can be used for polling.
     */
    @Transactional
    public void syncPaymentStatus(String pgOrderId) {
        Payment payment = paymentRepository.findByPgTransactionId(pgOrderId)
                .orElseThrow(() -> new BadRequestException("Payment not found for PG order: " + pgOrderId));

        try {
            String token = pineLabsConfig.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Request-ID", java.util.UUID.randomUUID().toString());
            headers.set("Request-Timestamp", java.time.Instant.now().toString());

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = pineLabsRestTemplate.exchange(
                    pineLabsConfig.getBaseUrl() + "/api/pay/v1/orders/" + pgOrderId,
                    HttpMethod.GET,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.warn("Empty response when fetching Pine Labs order status for {}", pgOrderId);
                return;
            }

            String status = responseBody.get("status") != null 
                    ? responseBody.get("status").toString() : "UNKNOWN";
            log.info("Pine Labs status for order {}: {}", pgOrderId, status);

            Order order = payment.getOrder();

            if ("CHARGED".equalsIgnoreCase(status) || "CAPTURED".equalsIgnoreCase(status)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                payment.setTxnId(pgOrderId);
                order.setOrderStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                log.info("Payment SUCCESS for order {}", order.getOrderNumber());
            } else if ("FAILED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                payment.setStatus(PaymentStatus.FAILED);
                order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
                order.setUpdatedAt(LocalDateTime.now());
                log.info("Payment FAILED for order {}", order.getOrderNumber());
            } else {
                log.info("Pine Labs status '{}' - no DB change for order {}", status, order.getOrderNumber());
                return; // PENDING or other intermediate status
            }

            paymentRepository.save(payment);
            orderRepository.save(order);

        } catch (Exception e) {
            log.error("Failed to sync Pine Labs status for {}: {}", pgOrderId, e.getMessage(), e);
        }
    }
}
