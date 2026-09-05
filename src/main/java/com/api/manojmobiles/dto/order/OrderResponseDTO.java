package com.api.manojmobiles.dto.order;

import com.api.manojmobiles.dto.address.AddressResponseDTO;
import com.api.manojmobiles.entity.enums.DeliveryType;
import com.api.manojmobiles.entity.enums.OrderStatus;
import com.api.manojmobiles.entity.enums.PaymentMethod;
import com.api.manojmobiles.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private UUID id;
    private String orderNumber;
    private OrderStatus orderStatus;
    
    private DeliveryType deliveryType;
    private String trackingId;
    private String courierPartner;
    
    private DeliveryPartnerInfoDTO deliveryPartnerInfo;

    private AddressResponseDTO address;
    
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryCharge;
    private BigDecimal gstAmount;
    private String invoiceNumber;
    
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String txnId;
    private LocalDateTime paidAt;
    
    private LocalDateTime placedAt;
    private LocalDateTime expectedDeliveryDate;

    private String paymentUrl; // Pine Labs checkout URL for online payments

    private List<OrderItemResponseDTO> orderItems;
}