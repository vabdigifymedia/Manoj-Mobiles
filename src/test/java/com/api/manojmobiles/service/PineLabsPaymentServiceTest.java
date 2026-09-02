package com.api.manojmobiles.service;

import com.api.manojmobiles.config.PineLabsConfig;
import com.api.manojmobiles.entity.Order;
import com.api.manojmobiles.entity.Payment;
import com.api.manojmobiles.entity.enums.OrderStatus;
import com.api.manojmobiles.entity.enums.PaymentStatus;
import com.api.manojmobiles.repository.OrderRepository;
import com.api.manojmobiles.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PineLabsPaymentServiceTest {

    @Mock
    private PineLabsConfig pineLabsConfig;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PineLabsPaymentService pineLabsPaymentService;

    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setOrderNumber("ORD-12345");
        order.setTotalAmount(new BigDecimal("500.00"));
        order.setOrderStatus(OrderStatus.PLACED);

        payment = new Payment();
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.PENDING);
    }

    @Test
    void testCreatePaymentOrder_Success() {
        when(pineLabsConfig.getAccessToken()).thenReturn("mock-token");
        when(pineLabsConfig.getBaseUrl()).thenReturn("https://mock-url.com");

        Map<String, Object> mockResponseMap = new HashMap<>();
        mockResponseMap.put("order_id", "pine-order-123");
        mockResponseMap.put("redirect_url", "https://checkout.url");
        ResponseEntity<Map<String, Object>> mockResponseEntity = new ResponseEntity<>(mockResponseMap, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://mock-url.com/api/pay/v1/orders"),
                eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<?>>any(),
                ArgumentMatchers.<org.springframework.core.ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(mockResponseEntity);

        String redirectUrl = pineLabsPaymentService.createPaymentOrder(order, payment);

        assertEquals("https://checkout.url", redirectUrl);
        assertEquals("pine-order-123", payment.getPgTransactionId());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void testSyncPaymentStatus_Success() {
        payment.setPgTransactionId("pine-order-123");
        when(paymentRepository.findByPgTransactionId("pine-order-123")).thenReturn(Optional.of(payment));
        when(pineLabsConfig.getAccessToken()).thenReturn("mock-token");
        when(pineLabsConfig.getBaseUrl()).thenReturn("https://mock-url.com");

        Map<String, Object> mockResponseMap = new HashMap<>();
        mockResponseMap.put("status", "CHARGED");
        mockResponseMap.put("order_id", "pine-order-123");
        ResponseEntity<Map<String, Object>> mockResponseEntity = new ResponseEntity<>(mockResponseMap, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://mock-url.com/api/pay/v1/orders/pine-order-123"),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<?>>any(),
                ArgumentMatchers.<org.springframework.core.ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(mockResponseEntity);

        pineLabsPaymentService.syncPaymentStatus("pine-order-123");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(paymentRepository, times(1)).save(payment);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void testSyncPaymentStatus_Failed() {
        payment.setPgTransactionId("pine-order-123");
        when(paymentRepository.findByPgTransactionId("pine-order-123")).thenReturn(Optional.of(payment));
        when(pineLabsConfig.getAccessToken()).thenReturn("mock-token");
        when(pineLabsConfig.getBaseUrl()).thenReturn("https://mock-url.com");

        Map<String, Object> mockResponseMap = new HashMap<>();
        mockResponseMap.put("status", "FAILED");
        mockResponseMap.put("order_id", "pine-order-123");
        ResponseEntity<Map<String, Object>> mockResponseEntity = new ResponseEntity<>(mockResponseMap, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://mock-url.com/api/pay/v1/orders/pine-order-123"),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<?>>any(),
                ArgumentMatchers.<org.springframework.core.ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(mockResponseEntity);

        pineLabsPaymentService.syncPaymentStatus("pine-order-123");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(OrderStatus.PAYMENT_FAILED, order.getOrderStatus());
        verify(paymentRepository, times(1)).save(payment);
        verify(orderRepository, times(1)).save(order);
    }
}
