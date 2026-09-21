package com.api.manojmobiles.service.sms;

import lombok.Getter;

@Getter
public enum SmsTemplate {

    ORDER_DELIVERED(
            "17771#########05026",
            "Your order %s has been delivered successfully. Thank you for shopping with Manoj Mobiles! Rate your experience at manojmobiles.com. - Manoj Mobiles"
    ),
    REFUND_PROCESSED(
            "17771#########90225",
            "Refund of Rs. %s for order %s has been processed successfully. It will reflect in your account within 7 business days. - Manoj Mobiles"
    ),
    RETURN_PICKED(
            "17771#########99646",
            "Your returned product for order %s has been picked up. Refund of Rs. %s will be processed within %s business days. - Manoj Mobiles"
    ),
    RETURN_REQUEST_RECEIVED(
            "17771#########24747",
            "Your return request for order %s has been received. We will review it within %s hours. Return ID: %s . - Manoj Mobiles"
    ),
    PAYMENT_FAILED(
            "Payment Failed", // Note: The DLT portal snippet showed this literally instead of an ID.
            "Payment for your order %s of Rs. %s has failed. Please retry payment or place a new order. Need help? Call %s . - Manoj Mobiles"
    ),
    ORDER_CANCELLED(
            "17771#########59205",
            "Your order %s has been cancelled. If payment was made, refund of Rs. %s will be processed within 2 business days. - Manoj Mobiles"
    ),
    OUT_FOR_DELIVERY(
            "17771#########81422",
            "Your order %s is out for delivery by %s. Expected arrival by %s . Track live: %s - Manoj Mobiles"
    ),
    RETURN_REJECTED(
            "17771#########18481",
            "Your return request %s for order %s has been rejected. Reason: %s . For queries, contact us at %s . - Manoj Mobiles"
    ),
    RETURN_APPROVED(
            "17771#########30133",
            "Your return request %s for order %s has been approved. Please keep the product ready for pickup. - Manoj Mobiles"
    ),
    ORDER_STATUS_UPDATE(
            "17771#########11035",
            "Update for order %s : Status changed to %s . Track here: %s . For help, call %s . - Manoj Mobiles"
    ),
    ORDER_SHIPPED(
            "17771#########58814",
            "Your order %s has been shipped via %s . Expected delivery by %s . Track here: %s - Manoj Mobiles"
    ),
    ORDER_PACKED(
            "17771#########55593",
            "Great news! Your order %s has been packed and is ready for dispatch. You will receive tracking details shortly. - Manoj Mobiles"
    ),
    ORDER_CONFIRMED(
            "17771#########57913",
            "Payment of Rs. %s received for order %s . Your order is confirmed and will be processed shortly. - Manoj Mobiles"
    ),
    ORDER_PLACED_CONFIRMATION(
            "17771#########28015",
            "Your order %s has been placed successfully on Manoj Mobiles. Order value: Rs. %s. Track your order at manojmobiles.com. Thank you for shopping! - Manoj Mobiles"
    ),
    STAFF_PASSWORD_RESET(
            "17771#########61122",
            "%s is your OTP for password reset on Manoj Mobiles admin panel. Valid for 10 minutes. Do not share. - Manoj Mobiles www.emistore.in"
    ),
    DELIVERY_VERIFICATION_OTP(
            "17771#########97647",
            "%s is the OTP for delivery of your order %s . Share this OTP with the delivery person only. - Manoj Mobiles www.emistore.in"
    ),
    PHONE_VERIFICATION_OTP(
            "17771#########49194",
            "%s is your OTP to verify your phone number on Manoj Mobiles. Valid for 10 minutes. Do not share this with anyone. - Manoj Mobiles www.emistore.in"
    ),
    LOGIN(
            "17771#########40636",
            "%s is your OTP for Manoj Mobiles. Valid for 10 minutes. Do not share this OTP with anyone. - Manoj Mobiles www.emistore.in"
    );

    private final String templateId;
    private final String messageFormat;

    SmsTemplate(String templateId, String messageFormat) {
        this.templateId = templateId;
        this.messageFormat = messageFormat;
    }

    public String formatMessage(Object... args) {
        return String.format(messageFormat, args);
    }
}
