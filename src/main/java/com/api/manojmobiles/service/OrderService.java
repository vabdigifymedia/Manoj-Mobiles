package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.address.AddressResponseDTO;
import com.api.manojmobiles.dto.cart.CartItemResponseDTO;
import com.api.manojmobiles.dto.cart.CartResponseDTO;
import com.api.manojmobiles.dto.order.OrderItemResponseDTO;
import com.api.manojmobiles.dto.order.OrderResponseDTO;
import com.api.manojmobiles.dto.order.PlaceOrderRequestDTO;
import com.api.manojmobiles.entity.*;
import com.api.manojmobiles.entity.enums.OrderStatus;
import com.api.manojmobiles.entity.enums.PaymentMethod;
import com.api.manojmobiles.entity.enums.PaymentStatus;
import com.api.manojmobiles.entity.enums.NotificationType;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ServiceablePincodeRepository pincodeRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final CouponService couponService;
    private final NotificationService notificationService;
    private final PineLabsPaymentService pineLabsPaymentService;

    @Transactional
    public OrderResponseDTO placeOrder(String username, PlaceOrderRequestDTO request) {
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        // 1. Fetch Cart
        CartResponseDTO cartDTO = cartService.getCart(username, null);
        if (cartDTO.getItems() == null || cartDTO.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Check if there are any unavailable items
        for (CartItemResponseDTO item : cartDTO.getItems()) {
            if (!item.getIsAvailable()) {
                throw new BadRequestException("Remove unavailable items from cart before checkout");
            }
        }

        // 2. Validate Address
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Address does not belong to user");
        }

        // 3. Serviceability Check
        ServiceablePincode pincode = pincodeRepository.findByPincode(address.getPincode())
                .orElseThrow(() -> new BadRequestException("Delivery is not serviceable to pincode: " + address.getPincode()));

        LocalDateTime expectedDelivery = LocalDateTime.now().plusDays(pincode.getEstimatedDeliveryDays());

        // 4. Create Order Object
        Order order = Order.builder()
                .user(user)
                .address(address)
                .orderStatus(OrderStatus.PLACED)
                .placedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deliveryCharge(BigDecimal.ZERO) // Free Delivery Rule
                .discountAmount(BigDecimal.ZERO) // Coupon out of scope
                .totalAmount(BigDecimal.ZERO)
                .gstAmount(BigDecimal.ZERO)
                .build();
        
        // 5. Generate Order Number
        order.setOrderNumber(generateOrderNumber());

        order = orderRepository.save(order);

        // 6. Calculate Totals and Deduct Stock safely
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal gstTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemResponseDTO itemDTO : cartDTO.getItems()) {
            // Pessimistic Lock!
            ProductVariant variant = productVariantRepository.findForUpdateById(itemDTO.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + itemDTO.getVariantId()));

            if (variant.getStockQty() < itemDTO.getQty()) {
                throw new BadRequestException("Insufficient stock for " + variant.getVariantName() + ". Only " + variant.getStockQty() + " left.");
            }

            // Deduct stock
            int newQty = variant.getStockQty() - itemDTO.getQty();
            variant.setStockQty(newQty);
            // StockStatus will be updated manually if needed, or by ProductService usually. For simplicity:
            if (newQty == 0) variant.setStockStatus(com.api.manojmobiles.entity.enums.StockStatus.OUT_OF_STOCK);
            else if (newQty <= 5) variant.setStockStatus(com.api.manojmobiles.entity.enums.StockStatus.LIMITED_STOCK);
            productVariantRepository.save(variant);

            // Audit
            InventoryLog logEntry = InventoryLog.builder()
                    .variant(variant)
                    .changeQty(-itemDTO.getQty())
                    .reason(com.api.manojmobiles.entity.enums.InventoryReason.SALE)
                    .build();
            inventoryLogRepository.save(logEntry);

            // Calculations
            BigDecimal itemPrice = variant.getSellingPrice();
            BigDecimal itemSubTotal = itemPrice.multiply(BigDecimal.valueOf(itemDTO.getQty()));
            
            BigDecimal gstPercent = variant.getGstPercent() != null ? new BigDecimal(variant.getGstPercent().toString()) : BigDecimal.ZERO;
            BigDecimal gstAmount = itemSubTotal.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            subTotal = subTotal.add(itemSubTotal);
            gstTotal = gstTotal.add(gstAmount);

            // Create OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariant(variant);
            orderItem.setQty(itemDTO.getQty());
            orderItem.setPrice(itemPrice);
            orderItem.setSubtotal(itemSubTotal);
            orderItems.add(orderItemRepository.save(orderItem));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            appliedCoupon = couponService.getCouponEntityByCode(request.getCouponCode().trim());
            discountAmount = couponService.validateAndCalculateDiscount(appliedCoupon, subTotal, user);
        }

        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(subTotal.subtract(discountAmount).max(BigDecimal.ZERO));
        order.setGstAmount(gstTotal);
        orderRepository.save(order);
        
        if (appliedCoupon != null) {
            couponService.recordCouponUsage(appliedCoupon, user, order);
        }

        // 7. Create Payment
        Payment payment = Payment.builder()
                .order(order)
                .method(request.getPaymentMethod())
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // 8. Order Status History
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PLACED)
                .changedAt(LocalDateTime.now())
                .note("Order placed successfully")
                .build();
        statusHistoryRepository.save(history);

        // 9. Clear Cart finally
        cartService.clearCart(cartDTO.getId());

        // 10. Send Notification
        notificationService.createNotification(
                user,
                "Order Placed",
                "Your order " + order.getOrderNumber() + " has been placed successfully.",
                NotificationType.ORDER_UPDATE
        );

        log.info("Order {} placed for user {}", order.getOrderNumber(), username);

        // 11. Generate Pine Labs Payment Link for non-COD orders
        String paymentUrl = null;
        if (request.getPaymentMethod() != PaymentMethod.COD) {
            paymentUrl = pineLabsPaymentService.createPaymentOrder(order, payment, request.getReturnUrl());
        }

        OrderResponseDTO responseDTO = mapToDTO(order, orderItems, payment, expectedDelivery);
        responseDTO.setPaymentUrl(paymentUrl);
        return responseDTO;
    }

    @Transactional
    public OrderResponseDTO cancelOrder(String username, UUID orderId, com.api.manojmobiles.dto.order.CancelOrderRequestDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have access to this order");
        }

        if (order.getOrderStatus() != OrderStatus.PLACED && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Order cannot be cancelled at this stage. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        // Restock inventory
        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = productVariantRepository.findForUpdateById(item.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
            
            variant.setStockQty(variant.getStockQty() + item.getQty());
            if (variant.getStockQty() > 0 && variant.getStockStatus() == com.api.manojmobiles.entity.enums.StockStatus.OUT_OF_STOCK) {
                variant.setStockStatus(com.api.manojmobiles.entity.enums.StockStatus.IN_STOCK);
            } else if (variant.getStockQty() > 5 && variant.getStockStatus() == com.api.manojmobiles.entity.enums.StockStatus.LIMITED_STOCK) {
                variant.setStockStatus(com.api.manojmobiles.entity.enums.StockStatus.IN_STOCK);
            }
            productVariantRepository.save(variant);

            InventoryLog logEntry = InventoryLog.builder()
                    .variant(variant)
                    .changeQty(item.getQty())
                    .reason(com.api.manojmobiles.entity.enums.InventoryReason.RETURN)
                    .build();
            inventoryLogRepository.save(logEntry);
        }

        // Handle Payment refund logic
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null && payment.getMethod() != PaymentMethod.COD && payment.getStatus() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .changedAt(LocalDateTime.now())
                .note("Cancelled by user. Reason: " + request.getReason())
                .updatedBy(username)
                .build();
        statusHistoryRepository.save(history);

        log.info("Order {} cancelled by user {}", order.getOrderNumber(), username);

        // Send Notification
        notificationService.createNotification(
                user,
                "Order Cancelled",
                "Your order " + order.getOrderNumber() + " has been cancelled.",
                NotificationType.ORDER_UPDATE
        );

        return mapToDTO(order, order.getOrderItems(), payment, null);
    }

    @Transactional
    public OrderResponseDTO mockPayment(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));

        if (payment.getMethod() == PaymentMethod.COD) {
            throw new BadRequestException("Mock payment is not applicable for COD orders");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Payment is already successful");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTxnId("MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        paymentRepository.save(payment);

        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CONFIRMED)
                .changedAt(LocalDateTime.now())
                .note("Payment successful via mock endpoint")
                .build();
        statusHistoryRepository.save(history);

        // Send Notification
        notificationService.createNotification(
                order.getUser(),
                "Payment Successful",
                "Payment for order " + order.getOrderNumber() + " was successful. Your order is confirmed.",
                NotificationType.ORDER_UPDATE
        );

        return mapToDTO(order, order.getOrderItems(), payment, null); // We can calculate delivery again if needed, or omit for now
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getUserOrders(String username, Pageable pageable) {
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        
        return orderRepository.findByUserIdOrderByPlacedAtDesc(user.getId(), pageable)
                .map(order -> mapToDTO(order, order.getOrderItems(), paymentRepository.findByOrderId(order.getId()).orElse(null), null));
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(String username, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        
        if (!order.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have access to this order");
        }

        return mapToDTO(order, order.getOrderItems(), paymentRepository.findByOrderId(order.getId()).orElse(null), null);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(order -> mapToDTO(order, order.getOrderItems(), paymentRepository.findByOrderId(order.getId()).orElse(null), null));
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(UUID orderId, OrderStatus newStatus, String note, String updatedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        order.setOrderStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .changedAt(LocalDateTime.now())
                .note(note)
                .updatedBy(updatedBy)
                .build();
        statusHistoryRepository.save(history);

        // Send Notification
        notificationService.createNotification(
                order.getUser(),
                "Order Status Updated",
                "Your order " + order.getOrderNumber() + " is now " + newStatus.name() + ".",
                NotificationType.ORDER_UPDATE
        );

        return mapToDTO(order, order.getOrderItems(), paymentRepository.findByOrderId(order.getId()).orElse(null), null);
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber;
        do {
            int randomNum = 10000 + new Random().nextInt(90000); // 5 digits
            orderNumber = "ORD-" + datePart + "-" + randomNum;
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    private OrderResponseDTO mapToDTO(Order order, List<OrderItem> items, Payment payment, LocalDateTime expectedDelivery) {
        List<OrderItemResponseDTO> itemDTOs = new ArrayList<>();
        if (items != null) {
            itemDTOs = items.stream().map(item -> OrderItemResponseDTO.builder()
                    .id(item.getId())
                    .variantId(item.getVariant().getId())
                    .variantName(item.getVariant().getVariantName())
                    .productName(item.getVariant().getProduct().getName())
                    .primaryImageUrl(item.getVariant().getImages() != null && !item.getVariant().getImages().isEmpty() 
                            ? item.getVariant().getImages().get(0).getUrl() : null)
                    .qty(item.getQty())
                    .price(item.getPrice())
                    .subtotal(item.getSubtotal())
                    .build()).collect(Collectors.toList());
        }

        AddressResponseDTO addressDTO = AddressResponseDTO.builder()
                .id(order.getAddress().getId())
                .label(order.getAddress().getLabel())
                .addressLine(order.getAddress().getAddressLine())
                .city(order.getAddress().getCity())
                .state(order.getAddress().getState())
                .pincode(order.getAddress().getPincode())
                .isDefault(order.getAddress().getIsDefault())
                .build();

        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .address(addressDTO)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .deliveryCharge(order.getDeliveryCharge())
                .gstAmount(order.getGstAmount())
                .invoiceNumber(order.getInvoiceNumber())
                .paymentMethod(payment != null ? payment.getMethod() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .txnId(payment != null ? payment.getTxnId() : null)
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .placedAt(order.getPlacedAt())
                .expectedDeliveryDate(expectedDelivery) // Could be null on fetch unless we persist it or fetch pincode again
                .orderItems(itemDTOs)
                .build();
    }
}
