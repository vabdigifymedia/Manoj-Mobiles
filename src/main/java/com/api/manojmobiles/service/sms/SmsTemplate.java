package com.api.manojmobiles.service.sms;

import lombok.Getter;

@Getter
public enum SmsTemplate {

    ORDER_DELIVERED(
            "1777178981242405026",
            "Your order %s has been delivered successfully. Thank you for shopping with Manoj Mobiles! Rate your experience at manojmobiles.com. - Manoj Mobiles"
    ),
    REFUND_PROCESSED(
            "1777178973642290225",
            "Refund of Rs. %s for order %s has been processed successfully. It will reflect in your account within 7 business days. - Manoj Mobiles"
    ),
    RETURN_PICKED(
            "1777178973631899646",
            "Your returned product for order %s has been picked up. Refund of Rs. %s will be processed within %s business days. - Manoj Mobiles"
    ),
    RETURN_REQUEST_RECEIVED(
            "1777178973607924747",
            "Your return request for order %s has been received. We will review it within %s hours. Return ID: %s . - Manoj Mobiles"
    ),
    PAYMENT_FAILED(
            "Payment Failed", // Note: The DLT portal snippet showed this literally instead of an ID.
            "Payment for your order %s of Rs. %s has failed. Please retry payment or place a new order. Need help? Call %s . - Manoj Mobiles"
    ),
    ORDER_CANCELLED(
            "1777178973579559205",
            "Your order %s has been cancelled. If payment was made, refund of Rs. %s will be processed within 2 business days. - Manoj Mobiles"
    ),
    OUT_FOR_DELIVERY(
            "1777178973462181422",
            "Your order %s is out for delivery by %s. Expected arrival by %s . Track live: %s - Manoj Mobiles"
    ),
    RETURN_REJECTED(
            "1777178973652618481",
            "Your return request %s for order %s has been rejected. Reason: %s . For queries, contact us at %s . - Manoj Mobiles"
    ),
    RETURN_APPROVED(
            "1777178973618230133",
            "Your return request %s for order %s has been approved. Please keep the product ready for pickup. - Manoj Mobiles"
    ),
    ORDER_STATUS_UPDATE(
            "1777178980651411035",
            "Update for order %s : Status changed to %s . Track here: %s . For help, call %s . - Manoj Mobiles"
    ),
    ORDER_SHIPPED(
            "1777178973108658814",
            "Your order %s has been shipped via %s . Expected delivery by %s . Track here: %s - Manoj Mobiles"
    ),
    ORDER_PACKED(
            "1777178973092655593",
            "Great news! Your order %s has been packed and is ready for dispatch. You will receive tracking details shortly. - Manoj Mobiles"
    ),
    ORDER_CONFIRMED(
            "1777178973081757913",
            "Payment of Rs. %s received for order %s . Your order is confirmed and will be processed shortly. - Manoj Mobiles"
    ),
    ORDER_PLACED_CONFIRMATION(
            "1777178973070828015",
            "Your order %s has been placed successfully on Manoj Mobiles. Order value: Rs. %s. Track your order at manojmobiles.com. Thank you for shopping! - Manoj Mobiles"
    ),
    STAFF_PASSWORD_RESET(
            "1777178973057761122",
            "%s is your OTP for password reset on Manoj Mobiles admin panel. Valid for 10 minutes. Do not share. - Manoj Mobiles www.emistore.in"
    ),
    DELIVERY_VERIFICATION_OTP(
            "1777178973045297647",
            "%s is the OTP for delivery of your order %s . Share this OTP with the delivery person only. - Manoj Mobiles www.emistore.in"
    ),
    PHONE_VERIFICATION_OTP(
            "1777178973017349194",
            "%s is your OTP to verify your phone number on Manoj Mobiles. Valid for 10 minutes. Do not share this with anyone. - Manoj Mobiles www.emistore.in"
    ),
    LOGIN(
            "1777178973003140636",
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
