package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.enquiry.BulkEnquiryResponseDTO;
import com.api.manojmobiles.dto.enquiry.SubmitBulkEnquiryRequestDTO;
import com.api.manojmobiles.entity.BulkEnquiry;
import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.enums.EnquiryStatus;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.BulkEnquiryRepository;
import com.api.manojmobiles.repository.ProductRepository;
import com.api.manojmobiles.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkEnquiryService {

    private final BulkEnquiryRepository bulkEnquiryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional
    public void submitEnquiry(SubmitBulkEnquiryRequestDTO request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
            
            // Verify variant belongs to product
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Variant does not belong to the selected product");
            }
        }

        BulkEnquiry enquiry = BulkEnquiry.builder()
                .product(product)
                .variant(variant)
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .mobileNumber(request.getMobileNumber().trim())
                .companyName(request.getCompanyName().trim())
                .gstin(request.getGstin() != null ? request.getGstin().trim() : null)
                .estimatedQuantity(request.getEstimatedQuantity())
                .requirements(request.getRequirements() != null ? request.getRequirements().trim() : null)
                .status(EnquiryStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        bulkEnquiryRepository.save(enquiry);
        log.info("New bulk enquiry submitted by {} for product {}", enquiry.getName(), product.getName());
    }

    @Transactional(readOnly = true)
    public Page<BulkEnquiryResponseDTO> getAllEnquiries(EnquiryStatus status, Pageable pageable) {
        Page<BulkEnquiry> page;
        if (status != null) {
            page = bulkEnquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = bulkEnquiryRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public BulkEnquiryResponseDTO getEnquiryById(UUID id) {
        BulkEnquiry enquiry = bulkEnquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bulk enquiry not found"));
        return mapToDTO(enquiry);
    }

    @Transactional
    public BulkEnquiryResponseDTO updateStatus(UUID id, EnquiryStatus newStatus) {
        BulkEnquiry enquiry = bulkEnquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bulk enquiry not found"));

        enquiry.setStatus(newStatus);
        enquiry.setUpdatedAt(LocalDateTime.now());
        bulkEnquiryRepository.save(enquiry);
        
        log.info("Bulk enquiry {} status updated to {}", id, newStatus);
        return mapToDTO(enquiry);
    }

    private BulkEnquiryResponseDTO mapToDTO(BulkEnquiry enquiry) {
        String variantName = null;
        if (enquiry.getVariant() != null) {
            variantName = enquiry.getVariant().getColor();
        }

        return BulkEnquiryResponseDTO.builder()
                .id(enquiry.getId())
                .productId(enquiry.getProduct().getId())
                .productName(enquiry.getProduct().getName())
                .variantId(enquiry.getVariant() != null ? enquiry.getVariant().getId() : null)
                .variantName(variantName)
                .name(enquiry.getName())
                .email(enquiry.getEmail())
                .mobileNumber(enquiry.getMobileNumber())
                .companyName(enquiry.getCompanyName())
                .gstin(enquiry.getGstin())
                .estimatedQuantity(enquiry.getEstimatedQuantity())
                .requirements(enquiry.getRequirements())
                .status(enquiry.getStatus())
                .createdAt(enquiry.getCreatedAt())
                .updatedAt(enquiry.getUpdatedAt())
                .build();
    }
}
