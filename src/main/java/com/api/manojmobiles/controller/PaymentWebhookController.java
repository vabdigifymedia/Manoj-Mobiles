package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.service.PineLabsPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment/webhook")
@RequiredArgsConstructor
@Tag(name = "Payment Webhook APIs", description = "Endpoints for payment gateway callbacks")
public class PaymentWebhookController {

    private final PineLabsPaymentService pineLabsPaymentService;

    /**
     * Pine Labs calls this endpoint after a payment is completed (success or failure).
     * This is an unauthenticated endpoint — Pine Labs does not send JWT tokens.
     * The payload typically contains the Pine Labs order_id which we use to 
     * fetch the latest status from Pine Labs API and update our database.
     */
    @Operation(summary = "Pine Labs Webhook", description = "Receives payment status updates from Pine Labs")
    @PostMapping("/pine-labs")
    public ResponseEntity<ApiResponse<Void>> handlePineLabsWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Pine Labs webhook received: {}", payload);

        String orderId = null;

        // Pine Labs may send the order ID under various keys depending on the event
        if (payload.containsKey("order_id")) {
            orderId = payload.get("order_id").toString();
        } else if (payload.containsKey("orderId")) {
            orderId = payload.get("orderId").toString();
        } else if (payload.containsKey("merchant_order_reference")) {
            // Fallback: look up by our order number
            log.warn("Webhook payload does not contain order_id, falling back to merchant_order_reference");
        }

        if (orderId != null) {
            pineLabsPaymentService.syncPaymentStatus(orderId);
        } else {
            log.warn("Unable to extract order_id from Pine Labs webhook payload: {}", payload);
        }

        // Always return 200 OK to Pine Labs to avoid retries
        return ResponseEntity.ok(ApiResponse.success("Webhook received", null));
    }

    /**
     * Manual endpoint to sync/check payment status from Pine Labs.
     * Useful for debugging or manual verification when webhook doesn't fire.
     */
    @Operation(summary = "Manual payment status sync", description = "Manually fetches and syncs payment status from Pine Labs by PG order ID")
    @GetMapping("/pine-labs/sync/{pgOrderId}")
    public ResponseEntity<ApiResponse<Void>> manualSync(@PathVariable String pgOrderId) {
        pineLabsPaymentService.syncPaymentStatus(pgOrderId);
        return ResponseEntity.ok(ApiResponse.success("Payment status synced", null));
    }
}
