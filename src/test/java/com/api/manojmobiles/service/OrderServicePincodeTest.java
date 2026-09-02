package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.cart.CartItemResponseDTO;
import com.api.manojmobiles.dto.cart.CartResponseDTO;
import com.api.manojmobiles.dto.order.OrderResponseDTO;
import com.api.manojmobiles.dto.order.PlaceOrderRequestDTO;
import com.api.manojmobiles.entity.*;
import com.api.manojmobiles.entity.enums.PaymentMethod;
import com.api.manojmobiles.entity.enums.StockStatus;
import com.api.manojmobiles.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServicePincodeTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderStatusHistoryRepository statusHistoryRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private ServiceablePincodeRepository pincodeRepository;
    @Mock private InventoryLogRepository inventoryLogRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartService cartService;
    @Mock private CouponService couponService;
    @Mock private NotificationService notificationService;
    @Mock private PineLabsPaymentService pineLabsPaymentService;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Address address;
    private ProductVariant variant;
    private CartResponseDTO cartDTO;
    private PlaceOrderRequestDTO placeOrderRequest;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("buyer@example.com")
                .phone("9876543210")
                .build();

        address = Address.builder()
                .id(UUID.randomUUID())
                .user(user)
                .label("Home")
                .addressLine("123 Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001") // Non-local pincode
                .isDefault(true)
                .build();

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Smartphone X");

        variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProduct(product);
        variant.setVariantName("Smartphone X (128GB)");
        variant.setStockQty(10);
        variant.setSellingPrice(BigDecimal.valueOf(15000));
        variant.setGstPercent(BigDecimal.valueOf(18));
        variant.setStockStatus(StockStatus.IN_STOCK);

        CartItemResponseDTO itemDTO = CartItemResponseDTO.builder()
                .id(UUID.randomUUID())
                .variantId(variant.getId())
                .qty(1)
                .isAvailable(true)
                .build();

        cartDTO = CartResponseDTO.builder()
                .id(UUID.randomUUID())
                .items(List.of(itemDTO))
                .build();

        placeOrderRequest = PlaceOrderRequestDTO.builder()
                .addressId(address.getId())
                .paymentMethod(PaymentMethod.COD)
                .build();
    }

    @Test
    @DisplayName("Place order succeeds even if pincode is not in local DB and defaults to standard 5 days")
    void placeOrder_unknownPincode_succeedsWithDefaultDelivery() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(cartService.getCart("buyer@example.com", null)).thenReturn(cartDTO);
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));

        // Pincode NOT in local DB
        when(pincodeRepository.findByPincode("400001")).thenReturn(Optional.empty());

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            return o;
        });
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(productVariantRepository.findForUpdateById(variant.getId())).thenReturn(Optional.of(variant));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO response = orderService.placeOrder("buyer@example.com", placeOrderRequest);

        assertNotNull(response);
        assertNotNull(response.getExpectedDeliveryDate());
        // Default delivery is +5 days
        LocalDate expectedDeliveryDate = response.getExpectedDeliveryDate().toLocalDate();
        assertEquals(LocalDate.now().plusDays(5), expectedDeliveryDate);
    }

    @Test
    @DisplayName("Place order uses local DB estimatedDeliveryDays when pincode is serviceable locally")
    void placeOrder_serviceableLocalPincode_usesCustomDeliveryDays() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(cartService.getCart("buyer@example.com", null)).thenReturn(cartDTO);
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));

        City mockCity = City.builder()
                .id(UUID.randomUUID())
                .name("Mumbai")
                .state("Maharashtra")
                .isActive(true)
                .build();
        ServiceablePincode localPincode = ServiceablePincode.builder()
                .pincode("400001")
                .city(mockCity)
                .estimatedDeliveryDays(2)
                .isActive(true)
                .build();
        when(pincodeRepository.findByPincode("400001")).thenReturn(Optional.of(localPincode));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            return o;
        });
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(productVariantRepository.findForUpdateById(variant.getId())).thenReturn(Optional.of(variant));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO response = orderService.placeOrder("buyer@example.com", placeOrderRequest);

        assertNotNull(response);
        assertNotNull(response.getExpectedDeliveryDate());
        LocalDate expectedDeliveryDate = response.getExpectedDeliveryDate().toLocalDate();
        assertEquals(LocalDate.now().plusDays(2), expectedDeliveryDate);
    }
}
