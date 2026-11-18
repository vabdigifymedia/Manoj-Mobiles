package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.delivery.*;
import com.api.manojmobiles.dto.order.LiveLocationResponseDTO;
import com.api.manojmobiles.entity.*;
import com.api.manojmobiles.entity.enums.DeliveryType;
import com.api.manojmobiles.entity.enums.OrderStatus;
import com.api.manojmobiles.repository.*;
import com.api.manojmobiles.utils.GeoUtils;
import com.api.manojmobiles.utils.PolylineCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HyperlocalTrackingTest {

    @Mock private DeliveryAgentRepository deliveryAgentRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private AgentLocationTrackingRepository locationTrackingRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusHistoryRepository statusHistoryRepository;
    @Mock private ServiceablePincodeRepository pincodeRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private LiveLocationService liveLocationService;
    @Mock private NotificationService notificationService;
    @Mock private StoreSettingService storeSettingService;

    private GoogleMapsService googleMapsService;
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        googleMapsService = new GoogleMapsService();
        StoreSetting defaultSettings = StoreSetting.builder()
                .storeName("Manoj Mobiles")
                .storeAddress("Sector 18, Noida")
                .storeLat(28.5355)
                .storeLng(77.3910)
                .freeDeliveryThreshold(BigDecimal.valueOf(999))
                .hyperlocalEnabled(true)
                .hyperlocalDeliveryCharge(BigDecimal.valueOf(49))
                .hyperlocalEstimatedMins(45)
                .hyperlocalMaxRadiusKm(15.0)
                .standardDeliveryCharge(BigDecimal.ZERO)
                .standardEstimatedDays(4)
                .build();
        lenient().when(storeSettingService.getCurrentSettings()).thenReturn(defaultSettings);

        deliveryService = new DeliveryService(
                deliveryAgentRepository,
                shipmentRepository,
                locationTrackingRepository,
                userRepository,
                orderRepository,
                statusHistoryRepository,
                pincodeRepository,
                addressRepository,
                passwordEncoder,
                messagingTemplate,
                liveLocationService,
                googleMapsService,
                notificationService,
                storeSettingService
        );
    }

    @Test
    @DisplayName("PolylineCodec round-trip & GeoUtils snapToPath progress calculation")
    void polylineAndSnapToPath_workEndToEnd() {
        double[][] points = {
                {28.5355, 77.3910},
                {28.5455, 77.3910},
                {28.5555, 77.3910}
        };
        String encoded = PolylineCodec.encode(points);
        assertFalse(encoded.isBlank());

        double[][] decoded = PolylineCodec.decode(encoded);
        assertEquals(3, decoded.length);
        assertEquals(28.5355, decoded[0][0], 1e-4);
        assertEquals(77.3910, decoded[0][1], 1e-4);

        // Snap a point roughly halfway along the path (slightly offset east by ~50m)
        GeoUtils.SnapResult snap = GeoUtils.snapToPath(decoded, 28.5455, 77.3915);
        assertEquals(0.5, snap.progress(), 0.05);
        assertTrue(snap.remainingMetres() > 900 && snap.remainingMetres() < 1300);

        double bearingNorth = GeoUtils.bearingDegrees(28.5355, 77.3910, 28.5455, 77.3910);
        assertTrue(bearingNorth < 1.0 || bearingNorth > 359.0);
    }

    @Test
    @DisplayName("GoogleMapsService produces straight-line fallback polyline and ETA without API key")
    void googleMapsFallback_generatesValidPolylineAndEta() {
        RouteInfoDTO route = googleMapsService.getRoute(28.5355, 77.3910, 28.5672, 77.3210);
        assertTrue(route.isFallback());
        assertNotNull(route.getEncodedPolyline());
        assertFalse(route.getEncodedPolyline().isBlank());
        assertTrue(route.getDistanceMetres() > 5000);
        assertTrue(route.getDurationSeconds() > 300);
        assertNotNull(route.getEta());
    }

    @Test
    @DisplayName("updateLocation writes to Redis, saves agent FK in DB, and broadcasts TrackingEventDTO")
    void updateLocation_writesRedisAndBroadcastsRichEvent() {
        UUID orderId = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).email("customer@example.com").name("Ankur").build();
        User agentUser = User.builder().id(UUID.randomUUID()).email("rider@example.com").name("Ravi").phone("9999999999").build();
        DeliveryAgent agent = DeliveryAgent.builder().id(UUID.randomUUID()).user(agentUser).vehicleNo("UP16-AB-1234").isAvailable(false).build();
        Order order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-20261118-10001")
                .user(customer)
                .deliveryType(DeliveryType.HYPERLOCAL)
                .orderStatus(OrderStatus.OUT_FOR_DELIVERY)
                .shippingLat(28.5555)
                .shippingLng(77.3910)
                .build();

        RouteInfoDTO route = googleMapsService.getRoute(28.5355, 77.3910, 28.5555, 77.3910);
        Shipment shipment = Shipment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .agent(agent)
                .currentStatus(OrderStatus.OUT_FOR_DELIVERY)
                .routePolyline(route.getEncodedPolyline())
                .lastLat(28.5355)
                .lastLng(77.3910)
                .build();

        when(userRepository.findByEmail("rider@example.com")).thenReturn(Optional.of(agentUser));
        when(deliveryAgentRepository.findByUserId(agentUser.getId())).thenReturn(Optional.of(agent));
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        LocationUpdateRequestDTO req = new LocationUpdateRequestDTO();
        req.setOrderId(orderId);
        req.setLat(28.5455);
        req.setLng(77.3912);
        req.setSpeedKmh(24.0);

        TrackingEventDTO event = deliveryService.updateLocation("rider@example.com", req);

        assertEquals(TrackingEventType.LOCATION_UPDATE, event.getEventType());
        assertEquals(0.5, event.getProgress(), 0.05);
        assertNotNull(event.getHeading());
        assertNotNull(event.getEta());

        // Verify AgentLocationTracking persisted WITH non-null agent FK
        ArgumentCaptor<AgentLocationTracking> dbCaptor = ArgumentCaptor.forClass(AgentLocationTracking.class);
        verify(locationTrackingRepository).save(dbCaptor.capture());
        assertNotNull(dbCaptor.getValue().getAgent());
        assertEquals(agent.getId(), dbCaptor.getValue().getAgent().getId());

        // Verify Redis cache IS written
        verify(liveLocationService).updateLiveLocation(eq(orderId), any(LiveLocationResponseDTO.class));
        // Verify WebSocket push to both customer queue and order topic
        verify(messagingTemplate).convertAndSendToUser(eq("customer@example.com"), eq("/queue/order-tracking"), any(TrackingEventDTO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/" + orderId + "/tracking"), any(TrackingEventDTO.class));
    }

    @Test
    @DisplayName("autoAssignNearestAgent selects closest available agent by GPS and notifies both sides")
    void autoAssignNearestAgent_picksClosestAgent() {
        UUID orderId = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).email("buyer@example.com").name("Buyer").build();
        Order order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-20261118-20002")
                .user(customer)
                .deliveryType(DeliveryType.HYPERLOCAL)
                .orderStatus(OrderStatus.CONFIRMED)
                .shippingLat(28.5555)
                .shippingLng(77.3910)
                .build();

        DeliveryAgent farAgent = DeliveryAgent.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).email("far@example.com").name("Far Rider").phone("1111111111").build())
                .vehicleNo("DL-01-FAR")
                .isAvailable(true)
                .currentLat(28.6500)
                .currentLng(77.2000)
                .build();

        DeliveryAgent nearAgent = DeliveryAgent.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).email("near@example.com").name("Near Rider").phone("2222222222").build())
                .vehicleNo("UP16-NEAR")
                .isAvailable(true)
                .currentLat(28.5360)
                .currentLng(77.3915)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(deliveryAgentRepository.findByIsAvailableTrue()).thenReturn(List.of(farAgent, nearAgent));
        when(shipmentRepository.countByAgentIdAndCurrentStatusIn(any(), anyList())).thenReturn(0L);
        when(shipmentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        ShipmentResponseDTO result = deliveryService.autoAssignNearestAgent(orderId);

        assertEquals(nearAgent.getId(), result.getAgent().getId());
        assertNotNull(result.getRoutePolyline());
        assertFalse(result.getRoutePolyline().isBlank());
        assertEquals(OrderStatus.SHIPPED, result.getCurrentStatus());

        verify(messagingTemplate).convertAndSendToUser(eq("buyer@example.com"), eq("/queue/order-tracking"), any(TrackingEventDTO.class));
        verify(messagingTemplate).convertAndSendToUser(eq("near@example.com"), eq("/queue/agent-assignments"), any(ShipmentResponseDTO.class));
    }

    @Test
    @DisplayName("getDeliveryOptions exposes both HYPERLOCAL and STANDARD with free-above threshold")
    void getDeliveryOptions_returnsBothModes() {
        City noida = City.builder().id(UUID.randomUUID()).name("Noida").state("UP").isActive(true).build();
        ServiceablePincode pin = ServiceablePincode.builder()
                .pincode("201301")
                .city(noida)
                .estimatedDeliveryDays(3)
                .codAvailable(true)
                .isActive(true)
                .build();
        when(pincodeRepository.findByPincode("201301")).thenReturn(Optional.of(pin));

        DeliveryOptionsResponseDTO options = deliveryService.getDeliveryOptions(
                "201301", 28.5450, 77.3920, BigDecimal.valueOf(1500));

        assertTrue(options.isHyperlocalAvailable());
        assertTrue(options.isStandardAvailable());
        assertEquals(DeliveryType.HYPERLOCAL, options.getRecommendedType());
        assertEquals(2, options.getOptions().size());
        assertEquals(BigDecimal.ZERO, options.getOptions().get(0).getDeliveryCharge()); // free above 999
    }
}
