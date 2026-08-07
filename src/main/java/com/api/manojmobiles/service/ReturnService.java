package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.returnreq.CreateReturnRequestDTO;
import com.api.manojmobiles.dto.returnreq.ReturnRequestResponseDTO;
import com.api.manojmobiles.dto.returnreq.UpdateReturnStatusRequestDTO;
import com.api.manojmobiles.entity.*;
import com.api.manojmobiles.entity.enums.OrderStatus;
import com.api.manojmobiles.entity.enums.ReturnStatus;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public ReturnRequestResponseDTO createReturnRequest(String username, CreateReturnRequestDTO request) {
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        if (!orderItem.getOrder().getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have access to this order item");
        }

        Order order = orderItem.getOrder();
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Return can only be requested for DELIVERED orders");
        }

        Product product = orderItem.getVariant().getProduct();
        if (Boolean.FALSE.equals(product.getIsReturnable())) {
            throw new BadRequestException("This product is not returnable");
        }

        // Return Window Enforcement
        Shipment shipment = shipmentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new BadRequestException("Shipment details not found for this order"));

        if (shipment.getDeliveredAt() == null) {
            throw new BadRequestException("Order is marked DELIVERED but delivery timestamp is missing");
        }

        long daysSinceDelivery = ChronoUnit.DAYS.between(shipment.getDeliveredAt(), LocalDateTime.now());
        if (daysSinceDelivery > product.getReturnPolicyDays()) {
            throw new BadRequestException("Return window has expired");
        }

        boolean existsActiveReturn = returnRequestRepository.existsByOrderItemIdAndStatusNot(
                orderItem.getId(), ReturnStatus.REJECTED);
        
        if (existsActiveReturn) {
            throw new BadRequestException("An active return request already exists for this item");
        }

        ReturnRequest returnReq = ReturnRequest.builder()
                .orderItem(orderItem)
                .reason(request.getReason())
                .status(ReturnStatus.REQUESTED)
                .refundAmount(orderItem.getSubtotal())
                .build();

        returnReq = returnRequestRepository.save(returnReq);

        log.info("Return request {} created by user {}", returnReq.getId(), username);

        return mapToDTO(returnReq);
    }

    @Transactional(readOnly = true)
    public Page<ReturnRequestResponseDTO> getUserReturnRequests(String username, Pageable pageable) {
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        return returnRequestRepository.findByOrderItemOrderUserIdOrderByRequestedAtDesc(user.getId(), pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public ReturnRequestResponseDTO getReturnRequestById(String username, UUID id) {
        ReturnRequest request = returnRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        if (!request.getOrderItem().getOrder().getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have access to this return request");
        }

        return mapToDTO(request);
    }

    @Transactional(readOnly = true)
    public Page<ReturnRequestResponseDTO> getAllReturnRequests(ReturnStatus status, Pageable pageable) {
        if (status != null) {
            return returnRequestRepository.findByStatusOrderByRequestedAtDesc(status, pageable)
                    .map(this::mapToDTO);
        }
        return returnRequestRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    @Transactional
    public ReturnRequestResponseDTO updateReturnStatus(UUID returnId, UpdateReturnStatusRequestDTO request, String updatedBy) {
        ReturnRequest returnReq = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        ReturnStatus currentStatus = returnReq.getStatus();
        ReturnStatus newStatus = request.getStatus();

        // Valid transitions check
        boolean isValidTransition = false;
        if (currentStatus == ReturnStatus.REQUESTED && (newStatus == ReturnStatus.APPROVED || newStatus == ReturnStatus.REJECTED)) {
            isValidTransition = true;
        } else if (currentStatus == ReturnStatus.APPROVED && newStatus == ReturnStatus.PICKED) {
            isValidTransition = true;
        } else if (currentStatus == ReturnStatus.PICKED && newStatus == ReturnStatus.REFUNDED) {
            isValidTransition = true;
        }

        if (!isValidTransition) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        returnReq.setStatus(newStatus);
        returnReq.setAdminNote(request.getAdminNote());
        
        // Restock inventory on REFUNDED
        if (newStatus == ReturnStatus.REFUNDED) {
            ProductVariant variant = productVariantRepository.findForUpdateById(returnReq.getOrderItem().getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

            variant.setStockQty(variant.getStockQty() + returnReq.getOrderItem().getQty());
            if (variant.getStockQty() > 0 && variant.getStockStatus() == com.api.manojmobiles.entity.enums.StockStatus.OUT_OF_STOCK) {
                variant.setStockStatus(com.api.manojmobiles.entity.enums.StockStatus.IN_STOCK);
            } else if (variant.getStockQty() > 5 && variant.getStockStatus() == com.api.manojmobiles.entity.enums.StockStatus.LIMITED_STOCK) {
                variant.setStockStatus(com.api.manojmobiles.entity.enums.StockStatus.IN_STOCK);
            }
            productVariantRepository.save(variant);

            InventoryLog logEntry = InventoryLog.builder()
                    .variant(variant)
                    .changeQty(returnReq.getOrderItem().getQty())
                    .reason(com.api.manojmobiles.entity.enums.InventoryReason.RETURN)
                    .build();
            inventoryLogRepository.save(logEntry);
        }

        returnReq = returnRequestRepository.save(returnReq);

        log.info("Return request {} status updated to {} by {}", returnReq.getId(), newStatus, updatedBy);

        return mapToDTO(returnReq);
    }

    private ReturnRequestResponseDTO mapToDTO(ReturnRequest returnReq) {
        ReturnRequestResponseDTO dto = new ReturnRequestResponseDTO();
        dto.setId(returnReq.getId());
        dto.setOrderId(returnReq.getOrderItem().getOrder().getId());
        dto.setOrderNumber(returnReq.getOrderItem().getOrder().getOrderNumber());
        dto.setOrderItemId(returnReq.getOrderItem().getId());
        dto.setProductName(returnReq.getOrderItem().getVariant().getProduct().getName());
        dto.setVariantName(returnReq.getOrderItem().getVariant().getVariantName());
        dto.setReason(returnReq.getReason());
        dto.setStatus(returnReq.getStatus());
        dto.setRefundAmount(returnReq.getRefundAmount());
        dto.setRequestedAt(returnReq.getRequestedAt());
        dto.setAdminNote(returnReq.getAdminNote());
        return dto;
    }
}
