package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.delivery.*;
import com.api.manojmobiles.dto.order.LiveLocationResponseDTO;
import com.api.manojmobiles.entity.*;
import com.api.manojmobiles.entity.enums.*;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.*;
import com.api.manojmobiles.utils.GeoUtils;
import com.api.manojmobiles.utils.PolylineCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final List<OrderStatus> ACTIVE_SHIPMENT_STATUSES = List.of(
            OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY
    );
    private static final double MIN_HEADING_MOVE_METRES = 2.0;
    private static final double DEFAULT_STORE_LAT = 28.5355;
    private static final double DEFAULT_STORE_LNG = 77.3910;
    private static final BigDecimal DEFAULT_HYPERLOCAL_CHARGE = BigDecimal.valueOf(49);
    private static final BigDecimal DEFAULT_FREE_DELIVERY_THRESHOLD = BigDecimal.valueOf(999);
    private static final int DEFAULT_HYPERLOCAL_MINS = 45;
    private static final double DEFAULT_HYPERLOCAL_RADIUS_KM = 15.0;
    private static final BigDecimal DEFAULT_STANDARD_CHARGE = BigDecimal.ZERO;
    private static final int DEFAULT_STANDARD_DAYS = 5;

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final ShipmentRepository shipmentRepository;
    private final AgentLocationTrackingRepository locationTrackingRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ServiceablePincodeRepository pincodeRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final SimpMessagingTemplate messagingTemplate;
    private final LiveLocationService liveLocationService;
    private final GoogleMapsService googleMapsService;
    private final NotificationService notificationService;
    private final StoreSettingService storeSettingService;
    private final DeliveryPartnerRepository deliveryPartnerRepository;

    @Transactional
    public DeliveryAgentResponseDTO createDeliveryAgent(DeliveryAgentRequestDTO request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("User with this email already exists");
        }
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new BadRequestException("User with this phone already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.DELIVERY_AGENT)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        DeliveryAgent agent = DeliveryAgent.builder()
                .user(user)
                .vehicleNo(request.getVehicleNo())
                .isAvailable(true)
                .build();
        agent = deliveryAgentRepository.save(agent);

        // Auto-create/sync DeliveryPartner so admin panel "Delivery Partners" menu also lists this agent
        DeliveryPartner partner = deliveryPartnerRepository.findByPhone(request.getPhone())
                .orElseGet(() -> DeliveryPartner.builder().build());
        partner.setName(request.getName());
        partner.setPhone(request.getPhone());
        partner.setEmail(request.getEmail());
        partner.setVehicleNo(request.getVehicleNo());
        partner.setIsActive(true);
        partner.setUser(user);
        partner.setAgent(agent);
        deliveryPartnerRepository.save(partner);

        return mapToAgentDTO(agent);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryAgentResponseDTO> getAllAgents(Pageable pageable) {
        return deliveryAgentRepository.findAll(pageable).map(this::mapToAgentDTO);
    }

    @Transactional
    public DeliveryAgentResponseDTO updateDeliveryAgent(UUID id, DeliveryAgentRequestDTO request) {
        DeliveryAgent agent = deliveryAgentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found with id: " + id));

        User user = agent.getUser();
        user.setName(request.getName());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);

        agent.setVehicleNo(request.getVehicleNo());
        DeliveryAgent updatedAgent = deliveryAgentRepository.save(agent);
        return mapToAgentDTO(updatedAgent);
    }

    @Transactional
    public void deleteDeliveryAgent(UUID id) {
        DeliveryAgent agent = deliveryAgentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found with id: " + id));
        deliveryAgentRepository.delete(agent);
        deliveryPartnerRepository.findByAgentId(agent.getId())
                .ifPresent(deliveryPartnerRepository::delete);
        userRepository.delete(agent.getUser());
    }

    @Transactional
    public DeliveryAgentResponseDTO updateAgentAvailability(String username, AvailabilityRequestDTO request) {
        User user = getUserByEmailOrPhone(username);
        DeliveryAgent agent = deliveryAgentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found"));

        agent.setIsAvailable(request.getIsAvailable());
        return mapToAgentDTO(deliveryAgentRepository.save(agent));
    }

    /**
     * Idle/active agent GPS heartbeat so auto-dispatch always knows which available agent is closest.
     */
    @Transactional
    public DeliveryAgentResponseDTO recordAgentHeartbeat(String username, AgentHeartbeatRequestDTO request) {
        User user = getUserByEmailOrPhone(username);
        DeliveryAgent agent = deliveryAgentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found"));

        Double heading = request.getHeading();
        if (heading == null && agent.getCurrentLat() != null && agent.getCurrentLng() != null) {
            double moved = GeoUtils.haversineMetres(agent.getCurrentLat(), agent.getCurrentLng(),
                    request.getLat(), request.getLng());
            if (moved >= MIN_HEADING_MOVE_METRES) {
                heading = GeoUtils.bearingDegrees(agent.getCurrentLat(), agent.getCurrentLng(),
                        request.getLat(), request.getLng());
            }
        }

        agent.setCurrentLat(request.getLat());
        agent.setCurrentLng(request.getLng());
        if (heading != null) {
            agent.setHeading(heading);
        }
        if (request.getIsAvailable() != null) {
            agent.setIsAvailable(request.getIsAvailable());
        }
        agent.setLastLocationAt(LocalDateTime.now());
        agent = deliveryAgentRepository.save(agent);

        liveLocationService.updateAgentHeartbeat(agent.getId(), request.getLat(), request.getLng(), agent.getHeading());
        return mapToAgentDTO(agent);
    }

    @Transactional
    public ShipmentResponseDTO assignShipment(UUID orderId, AssignShipmentRequestDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        DeliveryAgent agent = deliveryAgentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found"));

        if (!Boolean.TRUE.equals(agent.getIsAvailable())) {
            throw new BadRequestException("Selected agent is currently not available");
        }

        return assignShipmentInternal(order, agent, "Assigned manually by admin");
    }

    /**
     * Auto-dispatches the nearest available hyperlocal delivery agent based on live GPS heartbeat.
     */
    @Transactional
    public ShipmentResponseDTO autoAssignNearestAgent(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getDeliveryType() != DeliveryType.HYPERLOCAL) {
            throw new BadRequestException("Auto-dispatch is only supported for HYPERLOCAL orders");
        }

        StoreSetting settings = storeSettingService.getCurrentSettings();
        Double storeLat = resolveStoreLat(settings);
        Double storeLng = resolveStoreLng(settings);

        List<DeliveryAgent> candidates = deliveryAgentRepository.findByIsAvailableTrue().stream()
                .filter(a -> shipmentRepository.countByAgentIdAndCurrentStatusIn(a.getId(), ACTIVE_SHIPMENT_STATUSES) == 0)
                .toList();

        if (candidates.isEmpty()) {
            throw new BadRequestException("No available delivery agents right now");
        }

        DeliveryAgent nearest = candidates.stream()
                .min(Comparator.comparingDouble(agent -> {
                    LiveLocationResponseDTO cached = liveLocationService.getAgentHeartbeat(agent.getId());
                    Double lat = cached != null ? cached.getLat() : agent.getCurrentLat();
                    Double lng = cached != null ? cached.getLng() : agent.getCurrentLng();
                    return GeoUtils.safeDistance(storeLat, storeLng, lat, lng);
                }))
                .orElseThrow(() -> new BadRequestException("No available delivery agents right now"));

        return assignShipmentInternal(order, nearest, "Auto-dispatched nearest available agent");
    }

    private ShipmentResponseDTO assignShipmentInternal(Order order, DeliveryAgent agent, String reason) {
        if (order.getOrderStatus() != OrderStatus.PLACED
                && order.getOrderStatus() != OrderStatus.CONFIRMED
                && order.getOrderStatus() != OrderStatus.PACKED) {
            throw new BadRequestException("Order cannot be assigned in current status: " + order.getOrderStatus());
        }

        if (shipmentRepository.existsByOrderId(order.getId())) {
            throw new BadRequestException("A shipment is already assigned to this order");
        }

        StoreSetting settings = storeSettingService.getCurrentSettings();
        Double storeLat = resolveStoreLat(settings);
        Double storeLng = resolveStoreLng(settings);
        Double destLat = resolveDestLat(order);
        Double destLng = resolveDestLng(order);

        RouteInfoDTO route = googleMapsService.getRoute(storeLat, storeLng, destLat, destLng);

        LiveLocationResponseDTO heartbeat = liveLocationService.getAgentHeartbeat(agent.getId());
        Double initialLat = heartbeat != null ? heartbeat.getLat()
                : (agent.getCurrentLat() != null ? agent.getCurrentLat() : storeLat);
        Double initialLng = heartbeat != null ? heartbeat.getLng()
                : (agent.getCurrentLng() != null ? agent.getCurrentLng() : storeLng);
        Double initialHeading = heartbeat != null ? heartbeat.getHeading() : agent.getHeading();
        LocalDateTime now = LocalDateTime.now();

        Shipment shipment = Shipment.builder()
                .order(order)
                .agent(agent)
                .assignedAt(now)
                .currentStatus(OrderStatus.SHIPPED)
                .routePolyline(route.getEncodedPolyline())
                .routeDistanceMetres(route.getDistanceMetres())
                .routeDurationSeconds(route.getDurationSeconds())
                .storeLat(storeLat)
                .storeLng(storeLng)
                .progress(0.0)
                .remainingMetres(route.getDistanceMetres())
                .remainingSeconds(route.getDurationSeconds())
                .eta(route.getEta())
                .lastLat(initialLat)
                .lastLng(initialLng)
                .lastHeading(initialHeading)
                .lastLocationAt(now)
                .build();
        shipment = shipmentRepository.save(shipment);

        agent.setIsAvailable(false);
        deliveryAgentRepository.save(agent);

        order.setOrderStatus(OrderStatus.SHIPPED);
        order.setExpectedDeliveryDate(route.getEta());
        order.setUpdatedAt(now);

        // Auto-link DeliveryPartner to order if exists
        deliveryPartnerRepository.findByAgentId(agent.getId())
                .or(() -> deliveryPartnerRepository.findByUserId(agent.getUser().getId()))
                .ifPresent(order::setDeliveryPartner);

        orderRepository.save(order);

        recordStatusHistory(order, OrderStatus.SHIPPED,
                reason + " (" + agent.getUser().getName() + ", " + agent.getVehicleNo() + ")",
                agent.getUser().getEmail());

        // Seed Redis with initial location so GET /live-location works immediately
        if (initialLat != null && initialLng != null) {
            liveLocationService.updateLiveLocation(order.getId(), LiveLocationResponseDTO.builder()
                    .lat(initialLat)
                    .lng(initialLng)
                    .rawLat(initialLat)
                    .rawLng(initialLng)
                    .heading(initialHeading)
                    .progress(0.0)
                    .remainingMetres(route.getDistanceMetres())
                    .remainingSeconds(route.getDurationSeconds())
                    .eta(route.getEta())
                    .recordedAt(now)
                    .build());
        }

        DeliveryAgentResponseDTO agentDTO = mapToAgentDTO(agent);
        ShipmentResponseDTO shipmentDTO = mapToShipmentDTO(shipment);

        // Push AGENT_ASSIGNED event to customer
        TrackingEventDTO event = TrackingEventDTO.builder()
                .eventType(TrackingEventType.AGENT_ASSIGNED)
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(OrderStatus.SHIPPED)
                .lat(initialLat)
                .lng(initialLng)
                .rawLat(initialLat)
                .rawLng(initialLng)
                .heading(initialHeading)
                .progress(0.0)
                .remainingMetres(route.getDistanceMetres())
                .remainingSeconds(route.getDurationSeconds())
                .eta(route.getEta())
                .routePolyline(route.getEncodedPolyline())
                .routeDistanceMetres(route.getDistanceMetres())
                .routeDurationSeconds(route.getDurationSeconds())
                .agent(agentDTO)
                .message(agent.getUser().getName() + " has been assigned to deliver your order")
                .timestamp(now)
                .build();
        broadcastTrackingEvent(order, event);

        // Push assignment alert to the delivery agent's private queue
        String agentPrincipal = resolvePrincipalName(agent.getUser());
        if (agentPrincipal != null) {
            messagingTemplate.convertAndSendToUser(
                    agentPrincipal,
                    "/queue/agent-assignments",
                    shipmentDTO
            );
        }

        notificationService.createNotification(
                order.getUser(),
                "Delivery Partner Assigned",
                agent.getUser().getName() + " (" + agent.getVehicleNo() + ") is assigned to order " + order.getOrderNumber(),
                NotificationType.ORDER_UPDATE
        );
        notificationService.createNotification(
                agent.getUser(),
                "New Delivery Assigned",
                "You have been assigned order " + order.getOrderNumber() + ". Please pick up from store.",
                NotificationType.ORDER_UPDATE
        );

        return shipmentDTO;
    }

    @Transactional(readOnly = true)
    public Page<ShipmentResponseDTO> getAgentShipments(String username, Pageable pageable) {
        User user = getUserByEmailOrPhone(username);
        DeliveryAgent agent = deliveryAgentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found"));

        return shipmentRepository.findByAgentIdAndCurrentStatusIn(agent.getId(), ACTIVE_SHIPMENT_STATUSES, pageable)
                .map(this::mapToShipmentDTO);
    }

    @Transactional
    public ShipmentResponseDTO updateShipmentStatus(String username, UUID shipmentId, OrderStatus newStatus) {
        User user = getUserByEmailOrPhone(username);
        DeliveryAgent agent = deliveryAgentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found"));

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        if (!shipment.getAgent().getId().equals(agent.getId())) {
            throw new AccessDeniedException("You do not have permission to update this shipment");
        }

        Order order = shipment.getOrder();
        LocalDateTime now = LocalDateTime.now();
        String eventMessage;

        if (newStatus == OrderStatus.OUT_FOR_DELIVERY && shipment.getCurrentStatus() == OrderStatus.SHIPPED) {
            shipment.setCurrentStatus(OrderStatus.OUT_FOR_DELIVERY);
            shipment.setPickedAt(now);
            order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
            order.setUpdatedAt(now);

            // Ensure route polyline is computed from pickup location if missing
            if (shipment.getRoutePolyline() == null || shipment.getRoutePolyline().isBlank()) {
                StoreSetting settings = storeSettingService.getCurrentSettings();
                Double originLat = agent.getCurrentLat() != null ? agent.getCurrentLat() : resolveStoreLat(settings);
                Double originLng = agent.getCurrentLng() != null ? agent.getCurrentLng() : resolveStoreLng(settings);
                RouteInfoDTO route = googleMapsService.getRoute(originLat, originLng, resolveDestLat(order), resolveDestLng(order));
                shipment.setRoutePolyline(route.getEncodedPolyline());
                shipment.setRouteDistanceMetres(route.getDistanceMetres());
                shipment.setRouteDurationSeconds(route.getDurationSeconds());
                shipment.setRemainingMetres(route.getDistanceMetres());
                shipment.setRemainingSeconds(route.getDurationSeconds());
                shipment.setEta(route.getEta());
                order.setExpectedDeliveryDate(route.getEta());
            }

            eventMessage = agent.getUser().getName() + " has picked up your order and is on the way";
            recordStatusHistory(order, OrderStatus.OUT_FOR_DELIVERY, eventMessage, username);
            notificationService.createNotification(order.getUser(), "Out for Delivery", eventMessage, NotificationType.ORDER_UPDATE);

        } else if (newStatus == OrderStatus.DELIVERED && shipment.getCurrentStatus() == OrderStatus.OUT_FOR_DELIVERY) {
            shipment.setCurrentStatus(OrderStatus.DELIVERED);
            shipment.setDeliveredAt(now);
            shipment.setProgress(1.0);
            shipment.setRemainingMetres(0);
            shipment.setRemainingSeconds(0);
            shipment.setEta(now);

            order.setOrderStatus(OrderStatus.DELIVERED);
            order.setUpdatedAt(now);

            // Free the agent for the next dispatch
            agent.setIsAvailable(true);
            deliveryAgentRepository.save(agent);

            // Automatically settle COD payments upon successful delivery confirmation
            if (order.getPayment() != null && order.getPayment().getMethod() == PaymentMethod.COD) {
                order.getPayment().setStatus(PaymentStatus.SUCCESS);
                order.getPayment().setPaidAt(now);
            }

            liveLocationService.clearLiveLocation(order.getId());
            eventMessage = "Your order " + order.getOrderNumber() + " has been delivered";
            recordStatusHistory(order, OrderStatus.DELIVERED, eventMessage, username);
            notificationService.createNotification(order.getUser(), "Order Delivered", eventMessage, NotificationType.ORDER_UPDATE);

        } else {
            throw new BadRequestException("Invalid status transition from " + shipment.getCurrentStatus() + " to " + newStatus);
        }

        shipmentRepository.save(shipment);
        orderRepository.save(order);

        TrackingEventDTO event = TrackingEventDTO.builder()
                .eventType(TrackingEventType.STATUS_CHANGE)
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(newStatus)
                .lat(shipment.getLastLat())
                .lng(shipment.getLastLng())
                .rawLat(shipment.getLastLat())
                .rawLng(shipment.getLastLng())
                .heading(shipment.getLastHeading())
                .progress(shipment.getProgress())
                .remainingMetres(shipment.getRemainingMetres())
                .remainingSeconds(shipment.getRemainingSeconds())
                .eta(shipment.getEta())
                .routePolyline(shipment.getRoutePolyline())
                .routeDistanceMetres(shipment.getRouteDistanceMetres())
                .routeDurationSeconds(shipment.getRouteDurationSeconds())
                .agent(mapToAgentDTO(agent))
                .message(eventMessage)
                .timestamp(now)
                .build();
        broadcastTrackingEvent(order, event);

        return mapToShipmentDTO(shipment);
    }

    /**
     * Ingests a live GPS ping from the assigned Delivery Agent:
     * 1. Snaps coordinate to the planned road polyline & calculates progress / heading / remaining ETA.
     * 2. Persists audit trail in PostgreSQL (fixing the missing agent FK).
     * 3. Updates Redis cache via LiveLocationService so REST fallback never returns null.
     * 4. Broadcasts rich TrackingEventDTO over WebSocket to the customer.
     */
    @Transactional
    public TrackingEventDTO updateLocation(String username, LocationUpdateRequestDTO request) {
        User user = getUserByEmailOrPhone(username);
        DeliveryAgent agent = deliveryAgentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found"));

        Shipment shipment = shipmentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order"));

        if (!shipment.getAgent().getId().equals(agent.getId())) {
            throw new AccessDeniedException("You are not assigned to this shipment");
        }

        if (shipment.getCurrentStatus() != OrderStatus.SHIPPED
                && shipment.getCurrentStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new BadRequestException("Location tracking is only active when order is SHIPPED or OUT_FOR_DELIVERY");
        }

        Order order = shipment.getOrder();
        LocalDateTime now = LocalDateTime.now();

        // Ensure polyline exists
        if (shipment.getRoutePolyline() == null || shipment.getRoutePolyline().isBlank()) {
            StoreSetting settings = storeSettingService.getCurrentSettings();
            RouteInfoDTO route = googleMapsService.getRoute(
                    resolveStoreLat(settings), resolveStoreLng(settings),
                    resolveDestLat(order), resolveDestLng(order));
            shipment.setRoutePolyline(route.getEncodedPolyline());
            shipment.setRouteDistanceMetres(route.getDistanceMetres());
            shipment.setRouteDurationSeconds(route.getDurationSeconds());
        }

        // Compute snapped point, progress, remaining distance, heading, and ETA
        double rawLat = request.getLat();
        double rawLng = request.getLng();
        double snappedLat = rawLat;
        double snappedLng = rawLng;
        double progress = shipment.getProgress() != null ? shipment.getProgress() : 0.0;
        double remainingMetres = 0.0;

        if (shipment.getRoutePolyline() != null && !shipment.getRoutePolyline().isBlank()) {
            double[][] path = PolylineCodec.decode(shipment.getRoutePolyline());
            GeoUtils.SnapResult snap = GeoUtils.snapToPath(path, rawLat, rawLng);
            snappedLat = snap.lat();
            snappedLng = snap.lng();
            progress = snap.progress();
            remainingMetres = snap.remainingMetres();
        } else if (resolveDestLat(order) != null && resolveDestLng(order) != null) {
            remainingMetres = GeoUtils.haversineMetres(rawLat, rawLng, resolveDestLat(order), resolveDestLng(order));
        }

        Double heading = request.getHeading();
        if (heading == null && shipment.getLastLat() != null && shipment.getLastLng() != null) {
            double moved = GeoUtils.haversineMetres(shipment.getLastLat(), shipment.getLastLng(), snappedLat, snappedLng);
            if (moved >= MIN_HEADING_MOVE_METRES) {
                heading = GeoUtils.bearingDegrees(shipment.getLastLat(), shipment.getLastLng(), snappedLat, snappedLng);
            } else {
                heading = shipment.getLastHeading();
            }
        }

        int remainingMetresInt = (int) Math.round(remainingMetres);
        int remainingSeconds = googleMapsService.estimateRemainingSeconds(remainingMetres, request.getSpeedKmh());
        LocalDateTime eta = now.plusSeconds(remainingSeconds);

        // Update agent's live position
        agent.setCurrentLat(rawLat);
        agent.setCurrentLng(rawLng);
        if (heading != null) {
            agent.setHeading(heading);
        }
        agent.setLastLocationAt(now);
        deliveryAgentRepository.save(agent);

        // Update shipment live snapshot
        shipment.setLastLat(snappedLat);
        shipment.setLastLng(snappedLng);
        shipment.setLastHeading(heading);
        shipment.setLastLocationAt(now);
        shipment.setProgress(progress);
        shipment.setRemainingMetres(remainingMetresInt);
        shipment.setRemainingSeconds(remainingSeconds);
        shipment.setEta(eta);
        shipmentRepository.save(shipment);

        // Persist history trail in Postgres (agent FK included!)
        AgentLocationTracking location = AgentLocationTracking.builder()
                .agent(agent)
                .order(order)
                .lat(rawLat)
                .lng(rawLng)
                .heading(heading)
                .speedKmh(request.getSpeedKmh())
                .recordedAt(now)
                .build();
        locationTrackingRepository.save(location);

        // Write to Redis so GET /live-location and snapshots have instant sub-ms access
        LiveLocationResponseDTO cachedLocation = LiveLocationResponseDTO.builder()
                .lat(snappedLat)
                .lng(snappedLng)
                .rawLat(rawLat)
                .rawLng(rawLng)
                .heading(heading)
                .speedKmh(request.getSpeedKmh())
                .progress(progress)
                .remainingMetres(remainingMetresInt)
                .remainingSeconds(remainingSeconds)
                .eta(eta)
                .recordedAt(now)
                .build();
        liveLocationService.updateLiveLocation(order.getId(), cachedLocation);
        liveLocationService.updateAgentHeartbeat(agent.getId(), rawLat, rawLng, heading);

        // Broadcast rich event over WebSocket
        TrackingEventDTO event = TrackingEventDTO.builder()
                .eventType(TrackingEventType.LOCATION_UPDATE)
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(shipment.getCurrentStatus())
                .lat(snappedLat)
                .lng(snappedLng)
                .rawLat(rawLat)
                .rawLng(rawLng)
                .heading(heading)
                .speedKmh(request.getSpeedKmh())
                .progress(progress)
                .remainingMetres(remainingMetresInt)
                .remainingSeconds(remainingSeconds)
                .eta(eta)
                .timestamp(now)
                .build();
        broadcastTrackingEvent(order, event);

        return event;
    }

    /**
     * Bridges the legacy /api/delivery/orders/{orderId}/live-location endpoint into the unified
     * Redis + WebSocket pipeline so both agent APIs work identically.
     */
    @Transactional
    public TrackingEventDTO updateLocationByOrderId(UUID orderId, Double lat, Double lng, Double heading, Double speedKmh) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Optional<Shipment> shipmentOpt = shipmentRepository.findByOrderId(orderId);
        LocalDateTime now = LocalDateTime.now();

        double snappedLat = lat;
        double snappedLng = lng;
        double progress = 0.0;
        double remainingMetres = 0.0;
        Double computedHeading = heading;

        if (shipmentOpt.isPresent()) {
            Shipment shipment = shipmentOpt.get();
            if (shipment.getRoutePolyline() != null && !shipment.getRoutePolyline().isBlank()) {
                double[][] path = PolylineCodec.decode(shipment.getRoutePolyline());
                GeoUtils.SnapResult snap = GeoUtils.snapToPath(path, lat, lng);
                snappedLat = snap.lat();
                snappedLng = snap.lng();
                progress = snap.progress();
                remainingMetres = snap.remainingMetres();
            } else if (resolveDestLat(order) != null && resolveDestLng(order) != null) {
                remainingMetres = GeoUtils.haversineMetres(lat, lng, resolveDestLat(order), resolveDestLng(order));
            }

            if (computedHeading == null && shipment.getLastLat() != null && shipment.getLastLng() != null) {
                double moved = GeoUtils.haversineMetres(shipment.getLastLat(), shipment.getLastLng(), snappedLat, snappedLng);
                computedHeading = moved >= MIN_HEADING_MOVE_METRES
                        ? GeoUtils.bearingDegrees(shipment.getLastLat(), shipment.getLastLng(), snappedLat, snappedLng)
                        : shipment.getLastHeading();
            }

            int remainingSeconds = googleMapsService.estimateRemainingSeconds(remainingMetres, speedKmh);
            LocalDateTime eta = now.plusSeconds(remainingSeconds);

            shipment.setLastLat(snappedLat);
            shipment.setLastLng(snappedLng);
            shipment.setLastHeading(computedHeading);
            shipment.setLastLocationAt(now);
            shipment.setProgress(progress);
            shipment.setRemainingMetres((int) Math.round(remainingMetres));
            shipment.setRemainingSeconds(remainingSeconds);
            shipment.setEta(eta);
            shipmentRepository.save(shipment);
        } else {
            Double destLat = resolveDestLat(order);
            Double destLng = resolveDestLng(order);
            if (destLat != null && destLng != null) {
                remainingMetres = GeoUtils.haversineMetres(lat, lng, destLat, destLng);
                StoreSetting settings = storeSettingService.getCurrentSettings();
                Double storeLat = resolveStoreLat(settings);
                Double storeLng = resolveStoreLng(settings);
                double totalDist = GeoUtils.haversineMetres(storeLat, storeLng, destLat, destLng);
                if (totalDist > 0) {
                    progress = Math.min(1.0, Math.max(0.0, 1.0 - (remainingMetres / totalDist)));
                }
            }
            LiveLocationResponseDTO previous = liveLocationService.getLiveLocation(orderId);
            if (computedHeading == null && previous != null && previous.getLat() != null && previous.getLng() != null) {
                double moved = GeoUtils.haversineMetres(previous.getLat(), previous.getLng(), snappedLat, snappedLng);
                computedHeading = moved >= MIN_HEADING_MOVE_METRES
                        ? GeoUtils.bearingDegrees(previous.getLat(), previous.getLng(), snappedLat, snappedLng)
                        : previous.getHeading();
            }
        }

        int remainingMetresInt = (int) Math.round(remainingMetres);
        int remainingSeconds = googleMapsService.estimateRemainingSeconds(remainingMetres, speedKmh);
        LocalDateTime eta = now.plusSeconds(remainingSeconds);

        LiveLocationResponseDTO cached = LiveLocationResponseDTO.builder()
                .lat(snappedLat)
                .lng(snappedLng)
                .rawLat(lat)
                .rawLng(lng)
                .heading(computedHeading)
                .speedKmh(speedKmh)
                .progress(progress)
                .remainingMetres(remainingMetresInt)
                .remainingSeconds(remainingSeconds)
                .eta(eta)
                .recordedAt(now)
                .build();
        liveLocationService.updateLiveLocation(orderId, cached);

        TrackingEventDTO event = TrackingEventDTO.builder()
                .eventType(TrackingEventType.LOCATION_UPDATE)
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getOrderStatus())
                .lat(snappedLat)
                .lng(snappedLng)
                .rawLat(lat)
                .rawLng(lng)
                .heading(computedHeading)
                .speedKmh(speedKmh)
                .progress(progress)
                .remainingMetres(remainingMetresInt)
                .remainingSeconds(remainingSeconds)
                .eta(eta)
                .timestamp(now)
                .build();
        broadcastTrackingEvent(order, event);
        return event;
    }

    /**
     * Full tracking snapshot for customer map UI (paints store, destination, route polyline,
     * live location, ETA, progress, agent info, and timeline immediately on page load).
     */
    @Transactional(readOnly = true)
    public OrderTrackingSnapshotDTO getOrderTrackingSnapshot(String username, UUID orderId) {
        User user = getUserByEmailOrPhone(username);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this order");
        }
        return buildTrackingSnapshot(order);
    }

    @Transactional(readOnly = true)
    public OrderTrackingSnapshotDTO getOrderTrackingSnapshotForAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return buildTrackingSnapshot(order);
    }

    private OrderTrackingSnapshotDTO buildTrackingSnapshot(Order order) {
        Optional<Shipment> shipmentOpt = shipmentRepository.findByOrderId(order.getId());
        Shipment shipment = shipmentOpt.orElse(null);

        StoreSetting settings = storeSettingService.getCurrentSettings();
        Double storeLat = shipment != null && shipment.getStoreLat() != null
                ? shipment.getStoreLat() : resolveStoreLat(settings);
        Double storeLng = shipment != null && shipment.getStoreLng() != null
                ? shipment.getStoreLng() : resolveStoreLng(settings);
        Double destLat = resolveDestLat(order);
        Double destLng = resolveDestLng(order);

        String routePolyline = shipment != null ? shipment.getRoutePolyline() : null;
        Integer routeDistanceMetres = shipment != null ? shipment.getRouteDistanceMetres() : null;
        Integer routeDurationSeconds = shipment != null ? shipment.getRouteDurationSeconds() : null;
        LocalDateTime eta = shipment != null && shipment.getEta() != null
                ? shipment.getEta() : order.getExpectedDeliveryDate();

        // For hyperlocal orders without a stored polyline yet, compute one on-the-fly so the map renders cleanly
        if (order.getDeliveryType() == DeliveryType.HYPERLOCAL
                && (routePolyline == null || routePolyline.isBlank())
                && destLat != null && destLng != null) {
            RouteInfoDTO route = googleMapsService.getRoute(storeLat, storeLng, destLat, destLng);
            routePolyline = route.getEncodedPolyline();
            routeDistanceMetres = route.getDistanceMetres();
            routeDurationSeconds = route.getDurationSeconds();
            if (eta == null) {
                eta = route.getEta();
            }
        }

        // Resolve live agent location: Redis -> Shipment snapshot -> Latest DB ping
        LiveLocationResponseDTO liveLocation = liveLocationService.getLiveLocation(order.getId());
        if (liveLocation == null && shipment != null && shipment.getLastLat() != null && shipment.getLastLng() != null) {
            liveLocation = LiveLocationResponseDTO.builder()
                    .lat(shipment.getLastLat())
                    .lng(shipment.getLastLng())
                    .rawLat(shipment.getLastLat())
                    .rawLng(shipment.getLastLng())
                    .heading(shipment.getLastHeading())
                    .progress(shipment.getProgress())
                    .remainingMetres(shipment.getRemainingMetres())
                    .remainingSeconds(shipment.getRemainingSeconds())
                    .eta(shipment.getEta())
                    .recordedAt(shipment.getLastLocationAt())
                    .build();
        }
        if (liveLocation == null) {
            Optional<AgentLocationTracking> latestPing =
                    locationTrackingRepository.findFirstByOrderIdOrderByRecordedAtDesc(order.getId());
            if (latestPing.isPresent()) {
                AgentLocationTracking ping = latestPing.get();
                liveLocation = LiveLocationResponseDTO.builder()
                        .lat(ping.getLat())
                        .lng(ping.getLng())
                        .rawLat(ping.getLat())
                        .rawLng(ping.getLng())
                        .heading(ping.getHeading())
                        .speedKmh(ping.getSpeedKmh())
                        .recordedAt(ping.getRecordedAt())
                        .build();
            }
        }

        // Resolve assigned agent (either DeliveryAgent on Shipment or legacy DeliveryPartner on Order)
        DeliveryAgentResponseDTO agentDTO = null;
        if (shipment != null && shipment.getAgent() != null) {
            agentDTO = mapToAgentDTO(shipment.getAgent());
        } else if (order.getDeliveryPartner() != null) {
            DeliveryPartner partner = order.getDeliveryPartner();
            agentDTO = DeliveryAgentResponseDTO.builder()
                    .id(partner.getId())
                    .name(partner.getName())
                    .phone(partner.getPhone())
                    .vehicleNo(partner.getVehicleNo())
                    .isAvailable(partner.getIsActive())
                    .build();
        }

        Double progress = liveLocation != null && liveLocation.getProgress() != null
                ? liveLocation.getProgress()
                : (shipment != null && shipment.getProgress() != null
                        ? shipment.getProgress()
                        : (order.getOrderStatus() == OrderStatus.DELIVERED ? 1.0 : 0.0));

        Integer remainingMetres = liveLocation != null && liveLocation.getRemainingMetres() != null
                ? liveLocation.getRemainingMetres()
                : (shipment != null ? shipment.getRemainingMetres() : routeDistanceMetres);

        Integer remainingSeconds = liveLocation != null && liveLocation.getRemainingSeconds() != null
                ? liveLocation.getRemainingSeconds()
                : (shipment != null ? shipment.getRemainingSeconds() : routeDurationSeconds);

        if (liveLocation != null && liveLocation.getEta() != null) {
            eta = liveLocation.getEta();
        }

        List<OrderTrackingSnapshotDTO.StatusTimelineItemDTO> timeline = statusHistoryRepository
                .findByOrderIdOrderByChangedAtAsc(order.getId())
                .stream()
                .map(h -> OrderTrackingSnapshotDTO.StatusTimelineItemDTO.builder()
                        .status(h.getStatus())
                        .changedAt(h.getChangedAt())
                        .note(h.getNote())
                        .build())
                .toList();

        Address addr = order.getAddress();
        String destAddress = addr != null
                ? String.join(", ", addr.getAddressLine(), addr.getCity(), addr.getState())
                : null;

        boolean liveTrackingActive = order.getDeliveryType() == DeliveryType.HYPERLOCAL
                && (order.getOrderStatus() == OrderStatus.SHIPPED
                        || order.getOrderStatus() == OrderStatus.OUT_FOR_DELIVERY);

        return OrderTrackingSnapshotDTO.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .deliveryType(order.getDeliveryType())
                .orderStatus(order.getOrderStatus())
                .placedAt(order.getPlacedAt())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .eta(eta)
                .trackingId(order.getTrackingId())
                .courierPartner(order.getCourierPartner())
                .store(OrderTrackingSnapshotDTO.GeoPointDTO.builder()
                        .lat(storeLat)
                        .lng(storeLng)
                        .label(settings.getStoreName() != null ? settings.getStoreName() : "Manoj Mobiles")
                        .address(settings.getStoreAddress())
                        .build())
                .destination(OrderTrackingSnapshotDTO.GeoPointDTO.builder()
                        .lat(destLat)
                        .lng(destLng)
                        .label(addr != null ? addr.getLabel() : "Delivery Address")
                        .address(destAddress)
                        .pincode(addr != null ? addr.getPincode() : null)
                        .build())
                .routePolyline(routePolyline)
                .routeDistanceMetres(routeDistanceMetres)
                .routeDurationSeconds(routeDurationSeconds)
                .agentLocation(liveLocation)
                .progress(progress)
                .remainingMetres(remainingMetres)
                .remainingSeconds(remainingSeconds)
                .agent(agentDTO)
                .assignedAt(shipment != null ? shipment.getAssignedAt() : null)
                .pickedAt(shipment != null ? shipment.getPickedAt() : null)
                .deliveredAt(shipment != null ? shipment.getDeliveredAt() : null)
                .liveTrackingActive(liveTrackingActive)
                .wsEndpoint("/ws-delivery")
                .wsUserQueue("/user/queue/order-tracking")
                .wsOrderTopic("/topic/orders/" + order.getId() + "/tracking")
                .timeline(timeline)
                .build();
    }

    /**
     * Evaluates available delivery options (HYPERLOCAL vs STANDARD) for a pincode / coordinate pair
     * so checkout can display both options with charges and ETAs.
     */
    @Transactional(readOnly = true)
    public DeliveryOptionsResponseDTO getDeliveryOptions(String pincode, Double lat, Double lng, BigDecimal cartTotal) {
        StoreSetting settings = storeSettingService.getCurrentSettings();
        Double storeLat = resolveStoreLat(settings);
        Double storeLng = resolveStoreLng(settings);

        Optional<ServiceablePincode> pinOpt = pincode != null
                ? pincodeRepository.findByPincode(pincode.trim())
                : Optional.empty();

        boolean activePincode = pinOpt.isPresent()
                && Boolean.TRUE.equals(pinOpt.get().getIsActive())
                && (pinOpt.get().getCity() == null || Boolean.TRUE.equals(pinOpt.get().getCity().getIsActive()));

        Double distanceKm = null;
        if (lat != null && lng != null) {
            double metres = GeoUtils.haversineMetres(storeLat, storeLng, lat, lng);
            distanceKm = BigDecimal.valueOf(metres / 1000.0).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        boolean hyperlocalEnabled = resolveHyperlocalEnabled(settings);
        double maxRadiusKm = resolveHyperlocalMaxRadiusKm(settings);
        boolean withinRadius = distanceKm == null || distanceKm <= maxRadiusKm;
        boolean hyperlocalAvailable = hyperlocalEnabled && activePincode && withinRadius;

        String hyperlocalUnavailableReason = null;
        if (!hyperlocalEnabled) {
            hyperlocalUnavailableReason = "Hyperlocal express delivery is currently paused";
        } else if (!activePincode) {
            hyperlocalUnavailableReason = "Hyperlocal express delivery is not available for pincode " + pincode;
        } else if (!withinRadius) {
            hyperlocalUnavailableReason = String.format(
                    "Address is %.1f km away (max %.0f km for hyperlocal delivery)", distanceKm, maxRadiusKm);
        }

        BigDecimal effectiveTotal = cartTotal != null ? cartTotal : BigDecimal.ZERO;
        BigDecimal hyperlocalCharge = computeDeliveryCharge(DeliveryType.HYPERLOCAL, effectiveTotal);
        BigDecimal standardCharge = computeDeliveryCharge(DeliveryType.STANDARD, effectiveTotal);

        int hyperlocalMins = resolveHyperlocalEstimatedMins(settings);
        LocalDateTime hyperlocalEta = LocalDateTime.now().plusMinutes(hyperlocalMins);
        if (hyperlocalAvailable && lat != null && lng != null) {
            RouteInfoDTO route = googleMapsService.getRoute(storeLat, storeLng, lat, lng);
            hyperlocalMins = Math.max(15, (int) Math.ceil(route.getDurationSeconds() / 60.0));
            hyperlocalEta = route.getEta();
        }

        int standardDays = pinOpt.isPresent() && pinOpt.get().getEstimatedDeliveryDays() != null
                && pinOpt.get().getEstimatedDeliveryDays() > 1
                ? pinOpt.get().getEstimatedDeliveryDays()
                : resolveStandardEstimatedDays(settings);

        boolean codAvailable = !pinOpt.isPresent() || Boolean.TRUE.equals(pinOpt.get().getCodAvailable());

        DeliveryOptionDTO hyperlocalOption = DeliveryOptionDTO.builder()
                .type(DeliveryType.HYPERLOCAL)
                .label("Express Hyperlocal Delivery")
                .description(hyperlocalAvailable
                        ? "Live map tracking • Delivered in ~" + hyperlocalMins + " mins"
                        : hyperlocalUnavailableReason)
                .available(hyperlocalAvailable)
                .unavailableReason(hyperlocalUnavailableReason)
                .deliveryCharge(hyperlocalCharge)
                .estimatedMinutes(hyperlocalAvailable ? hyperlocalMins : null)
                .estimatedDays(null)
                .estimatedDeliveryTime(hyperlocalAvailable ? hyperlocalEta : null)
                .liveTrackingSupported(true)
                .codAvailable(codAvailable)
                .build();

        DeliveryOptionDTO standardOption = DeliveryOptionDTO.builder()
                .type(DeliveryType.STANDARD)
                .label("Standard Courier Delivery")
                .description("Delivered in " + standardDays + " business days via trusted courier partner")
                .available(true)
                .unavailableReason(null)
                .deliveryCharge(standardCharge)
                .estimatedMinutes(null)
                .estimatedDays(standardDays)
                .estimatedDeliveryTime(LocalDateTime.now().plusDays(standardDays))
                .liveTrackingSupported(false)
                .codAvailable(codAvailable)
                .build();

        return DeliveryOptionsResponseDTO.builder()
                .pincode(pincode)
                .hyperlocalAvailable(hyperlocalAvailable)
                .standardAvailable(true)
                .distanceFromStoreKm(distanceKm)
                .recommendedType(hyperlocalAvailable ? DeliveryType.HYPERLOCAL : DeliveryType.STANDARD)
                .options(List.of(hyperlocalOption, standardOption))
                .build();
    }

    @Transactional(readOnly = true)
    public DeliveryOptionsResponseDTO getDeliveryOptionsForAddress(String username, UUID addressId, BigDecimal cartTotal) {
        User user = getUserByEmailOrPhone(username);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Address does not belong to user");
        }
        return getDeliveryOptions(address.getPincode(), address.getLat(), address.getLng(), cartTotal);
    }

    public BigDecimal computeDeliveryCharge(DeliveryType type, BigDecimal orderSubTotal) {
        StoreSetting settings = storeSettingService.getCurrentSettings();
        BigDecimal freeAbove = resolveFreeDeliveryThreshold(settings);
        if (freeAbove != null && orderSubTotal != null && orderSubTotal.compareTo(freeAbove) >= 0) {
            return BigDecimal.ZERO;
        }
        if (type == DeliveryType.HYPERLOCAL) {
            return resolveHyperlocalCharge(settings);
        }
        return resolveStandardCharge(settings);
    }

    public boolean isHyperlocalEligible(String pincode, Double lat, Double lng) {
        StoreSetting settings = storeSettingService.getCurrentSettings();
        if (!resolveHyperlocalEnabled(settings)) {
            return false;
        }
        Optional<ServiceablePincode> pinOpt = pincode != null
                ? pincodeRepository.findByPincode(pincode.trim())
                : Optional.empty();
        boolean activePincode = pinOpt.isPresent()
                && Boolean.TRUE.equals(pinOpt.get().getIsActive())
                && (pinOpt.get().getCity() == null || Boolean.TRUE.equals(pinOpt.get().getCity().getIsActive()));
        if (!activePincode) {
            return false;
        }
        if (lat != null && lng != null) {
            double distKm = GeoUtils.haversineMetres(
                    resolveStoreLat(settings), resolveStoreLng(settings), lat, lng) / 1000.0;
            return distKm <= resolveHyperlocalMaxRadiusKm(settings);
        }
        return true;
    }

    public Double resolveStoreLat(StoreSetting settings) {
        return settings != null && settings.getStoreLat() != null ? settings.getStoreLat() : DEFAULT_STORE_LAT;
    }

    public Double resolveStoreLng(StoreSetting settings) {
        return settings != null && settings.getStoreLng() != null ? settings.getStoreLng() : DEFAULT_STORE_LNG;
    }

    public int resolveStandardEstimatedDays(StoreSetting settings) {
        return settings != null && settings.getStandardEstimatedDays() != null && settings.getStandardEstimatedDays() > 0
                ? settings.getStandardEstimatedDays()
                : DEFAULT_STANDARD_DAYS;
    }

    private boolean resolveHyperlocalEnabled(StoreSetting settings) {
        return settings == null || settings.getHyperlocalEnabled() == null || Boolean.TRUE.equals(settings.getHyperlocalEnabled());
    }

    private BigDecimal resolveHyperlocalCharge(StoreSetting settings) {
        return settings != null && settings.getHyperlocalDeliveryCharge() != null
                ? settings.getHyperlocalDeliveryCharge()
                : DEFAULT_HYPERLOCAL_CHARGE;
    }

    private BigDecimal resolveFreeDeliveryThreshold(StoreSetting settings) {
        return settings != null && settings.getFreeDeliveryThreshold() != null
                ? settings.getFreeDeliveryThreshold()
                : DEFAULT_FREE_DELIVERY_THRESHOLD;
    }

    private int resolveHyperlocalEstimatedMins(StoreSetting settings) {
        return settings != null && settings.getHyperlocalEstimatedMins() != null && settings.getHyperlocalEstimatedMins() > 0
                ? settings.getHyperlocalEstimatedMins()
                : DEFAULT_HYPERLOCAL_MINS;
    }

    private double resolveHyperlocalMaxRadiusKm(StoreSetting settings) {
        return settings != null && settings.getHyperlocalMaxRadiusKm() != null && settings.getHyperlocalMaxRadiusKm() > 0
                ? settings.getHyperlocalMaxRadiusKm()
                : DEFAULT_HYPERLOCAL_RADIUS_KM;
    }

    private BigDecimal resolveStandardCharge(StoreSetting settings) {
        return settings != null && settings.getStandardDeliveryCharge() != null
                ? settings.getStandardDeliveryCharge()
                : DEFAULT_STANDARD_CHARGE;
    }

    @Transactional(readOnly = true)
    public LocationResponseDTO trackOrderLocation(String username, UUID orderId) {
        User user = getUserByEmailOrPhone(username);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this order");
        }

        if (order.getOrderStatus() != OrderStatus.OUT_FOR_DELIVERY
                && order.getOrderStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException("Order is not currently out for delivery");
        }

        LiveLocationResponseDTO cached = liveLocationService.getLiveLocation(orderId);
        if (cached != null) {
            return LocationResponseDTO.builder()
                    .orderId(order.getId())
                    .lat(cached.getLat())
                    .lng(cached.getLng())
                    .recordedAt(cached.getRecordedAt())
                    .build();
        }

        AgentLocationTracking location = locationTrackingRepository.findFirstByOrderIdOrderByRecordedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No location data available yet"));

        return LocationResponseDTO.builder()
                .orderId(order.getId())
                .lat(location.getLat())
                .lng(location.getLng())
                .recordedAt(location.getRecordedAt())
                .build();
    }

    /**
     * Broadcasts a tracking event to both the customer's private queue (/user/queue/order-tracking)
     * and the order-scoped topic (/topic/orders/{orderId}/tracking).
     */
    public void broadcastTrackingEvent(Order order, TrackingEventDTO event) {
        if (order == null || event == null) {
            return;
        }
        try {
            String customerPrincipal = resolvePrincipalName(order.getUser());
            if (customerPrincipal != null) {
                messagingTemplate.convertAndSendToUser(
                        customerPrincipal,
                        "/queue/order-tracking",
                        event
                );
            }
            messagingTemplate.convertAndSend("/topic/orders/" + order.getId() + "/tracking", event);
        } catch (Exception ex) {
            log.warn("Failed to broadcast tracking event for order {}: {}", order.getId(), ex.getMessage());
        }
    }

    private String resolvePrincipalName(User user) {
        if (user == null) {
            return null;
        }
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail();
        }
        return user.getPhone();
    }

    private void recordStatusHistory(Order order, OrderStatus status, String note, String updatedBy) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(status)
                .changedAt(LocalDateTime.now())
                .note(note)
                .updatedBy(updatedBy)
                .build();
        statusHistoryRepository.save(history);
    }

    private Double resolveDestLat(Order order) {
        if (order.getShippingLat() != null) {
            return order.getShippingLat();
        }
        return order.getAddress() != null ? order.getAddress().getLat() : null;
    }

    private Double resolveDestLng(Order order) {
        if (order.getShippingLng() != null) {
            return order.getShippingLng();
        }
        return order.getAddress() != null ? order.getAddress().getLng() : null;
    }

    private User getUserByEmailOrPhone(String identifier) {
        return userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByPhone(identifier)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    private DeliveryAgentResponseDTO mapToAgentDTO(DeliveryAgent agent) {
        return DeliveryAgentResponseDTO.builder()
                .id(agent.getId())
                .userId(agent.getUser().getId())
                .name(agent.getUser().getName())
                .phone(agent.getUser().getPhone())
                .vehicleNo(agent.getVehicleNo())
                .isAvailable(agent.getIsAvailable())
                .currentLat(agent.getCurrentLat())
                .currentLng(agent.getCurrentLng())
                .heading(agent.getHeading())
                .lastLocationAt(agent.getLastLocationAt())
                .build();
    }

    private ShipmentResponseDTO mapToShipmentDTO(Shipment shipment) {
        Order order = shipment.getOrder();
        StoreSetting settings = storeSettingService.getCurrentSettings();

        String custName = order != null && order.getUser() != null ? order.getUser().getName() : null;
        String custPhone = order != null && order.getUser() != null ? order.getUser().getPhone() : null;
        String addressStr = null;
        String pincode = null;
        if (order != null && order.getAddress() != null) {
            Address addr = order.getAddress();
            StringBuilder sb = new StringBuilder();
            if (addr.getAddressLine() != null && !addr.getAddressLine().isBlank()) sb.append(addr.getAddressLine());
            if (addr.getCity() != null && !addr.getCity().isBlank()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(addr.getCity());
            }
            if (addr.getState() != null && !addr.getState().isBlank()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(addr.getState());
            }
            if (addr.getPincode() != null && !addr.getPincode().isBlank()) {
                if (sb.length() > 0) sb.append(" - ");
                sb.append(addr.getPincode());
            }
            addressStr = sb.toString();
            pincode = addr.getPincode();
        }

        Double destLat = order != null ? resolveDestLat(order) : null;
        Double destLng = order != null ? resolveDestLng(order) : null;

        String payMethod = "PREPAID";
        if (order != null && order.getPayment() != null && order.getPayment().getMethod() != null) {
            payMethod = order.getPayment().getMethod().name();
        }
        java.math.BigDecimal totalAmt = order != null && order.getTotalAmount() != null ? order.getTotalAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal collectAmt = "COD".equalsIgnoreCase(payMethod) ? totalAmt : java.math.BigDecimal.ZERO;

        return ShipmentResponseDTO.builder()
                .id(shipment.getId())
                .orderId(order != null ? order.getId() : null)
                .orderNumber(order != null ? order.getOrderNumber() : null)
                .agent(mapToAgentDTO(shipment.getAgent()))
                .currentStatus(shipment.getCurrentStatus())
                .assignedAt(shipment.getAssignedAt())
                .pickedAt(shipment.getPickedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .routePolyline(shipment.getRoutePolyline())
                .routeDistanceMetres(shipment.getRouteDistanceMetres())
                .routeDurationSeconds(shipment.getRouteDurationSeconds())
                .progress(shipment.getProgress())
                .remainingMetres(shipment.getRemainingMetres())
                .remainingSeconds(shipment.getRemainingSeconds())
                .eta(shipment.getEta())
                .customerName(custName)
                .customerPhone(custPhone)
                .deliveryAddress(addressStr)
                .deliveryPincode(pincode)
                .destinationLat(destLat)
                .destinationLng(destLng)
                .paymentMethod(payMethod)
                .totalAmount(totalAmt)
                .collectAmount(collectAmt)
                .storeName(settings != null ? settings.getStoreName() : "Manoj Mobiles")
                .storeAddress(settings != null ? settings.getStoreAddress() : null)
                .storeLat(shipment.getStoreLat() != null ? shipment.getStoreLat() : (settings != null ? resolveStoreLat(settings) : null))
                .storeLng(shipment.getStoreLng() != null ? shipment.getStoreLng() : (settings != null ? resolveStoreLng(settings) : null))
                .build();
    }
}
