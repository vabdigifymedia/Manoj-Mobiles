package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.delivery.*;
import com.api.manojmobiles.entity.*;
import com.api.manojmobiles.entity.enums.*;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final ShipmentRepository shipmentRepository;
    private final AgentLocationTrackingRepository locationTrackingRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final SimpMessagingTemplate messagingTemplate;

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
        // Assuming email/phone updates might need unique checks, simplify for now or omit them.
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

    @Transactional
    public ShipmentResponseDTO assignShipment(UUID orderId, AssignShipmentRequestDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.CONFIRMED && order.getOrderStatus() != OrderStatus.PACKED) {
            throw new BadRequestException("Order cannot be assigned in current status: " + order.getOrderStatus());
        }

        DeliveryAgent agent = deliveryAgentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found"));

        if (!Boolean.TRUE.equals(agent.getIsAvailable())) {
            throw new BadRequestException("Selected agent is currently not available");
        }

        Shipment shipment = Shipment.builder()
                .order(order)
                .agent(agent)
                .assignedAt(LocalDateTime.now())
                .currentStatus(OrderStatus.SHIPPED)
                .build();
        shipment = shipmentRepository.save(shipment);

        order.setOrderStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        return mapToShipmentDTO(shipment);
    }

    @Transactional(readOnly = true)
    public Page<ShipmentResponseDTO> getAgentShipments(String username, Pageable pageable) {
        User user = getUserByEmailOrPhone(username);
        DeliveryAgent agent = deliveryAgentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found"));

        List<OrderStatus> activeStatuses = List.of(OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY);
        return shipmentRepository.findByAgentIdAndCurrentStatusIn(agent.getId(), activeStatuses, pageable)
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

        if (newStatus == OrderStatus.OUT_FOR_DELIVERY && shipment.getCurrentStatus() == OrderStatus.SHIPPED) {
            shipment.setCurrentStatus(OrderStatus.OUT_FOR_DELIVERY);
            shipment.setPickedAt(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
        } else if (newStatus == OrderStatus.DELIVERED && shipment.getCurrentStatus() == OrderStatus.OUT_FOR_DELIVERY) {
            shipment.setCurrentStatus(OrderStatus.DELIVERED);
            shipment.setDeliveredAt(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.DELIVERED);
            
            // Automatically settle COD payments upon successful delivery confirmation
            if (order.getPayment().getMethod() == PaymentMethod.COD) {
                order.getPayment().setStatus(PaymentStatus.SUCCESS);
                order.getPayment().setPaidAt(LocalDateTime.now());
            }
        } else {
            throw new BadRequestException("Invalid status transition from " + shipment.getCurrentStatus() + " to " + newStatus);
        }

        shipmentRepository.save(shipment);
        orderRepository.save(order);

        return mapToShipmentDTO(shipment);
    }

    @Transactional
    public void updateLocation(String username, LocationUpdateRequestDTO request) {
        User user = getUserByEmailOrPhone(username);
        DeliveryAgent agent = deliveryAgentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found"));

        Shipment shipment = shipmentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order"));

        if (!shipment.getAgent().getId().equals(agent.getId())) {
            throw new AccessDeniedException("You are not assigned to this shipment");
        }

        if (shipment.getCurrentStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new BadRequestException("Location tracking is only active when order is OUT_FOR_DELIVERY");
        }

        AgentLocationTracking location = AgentLocationTracking.builder()
                .order(shipment.getOrder())
                .lat(request.getLat())
                .lng(request.getLng())
                .recordedAt(LocalDateTime.now())
                .build();
        locationTrackingRepository.save(location);

        // Push real-time location payload to the subscribed customer via private WS destination
        LocationResponseDTO response = LocationResponseDTO.builder()
                .orderId(request.getOrderId())
                .lat(request.getLat())
                .lng(request.getLng())
                .recordedAt(location.getRecordedAt())
                .build();

        String customerUsername = shipment.getOrder().getUser().getEmail();
        messagingTemplate.convertAndSendToUser(customerUsername, "/queue/order-tracking", response);
    }

    @Transactional(readOnly = true)
    public LocationResponseDTO trackOrderLocation(String username, UUID orderId) {
        User user = getUserByEmailOrPhone(username);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this order");
        }

        if (order.getOrderStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new BadRequestException("Order is not currently out for delivery");
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
                .build();
    }

    private ShipmentResponseDTO mapToShipmentDTO(Shipment shipment) {
        return ShipmentResponseDTO.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrder().getId())
                .orderNumber(shipment.getOrder().getOrderNumber())
                .agent(mapToAgentDTO(shipment.getAgent()))
                .currentStatus(shipment.getCurrentStatus())
                .assignedAt(shipment.getAssignedAt())
                .pickedAt(shipment.getPickedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .build();
    }
}
