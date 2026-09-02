package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.enquiry.BulkEnquiryResponseDTO;
import com.api.manojmobiles.dto.enquiry.SubmitBulkEnquiryRequestDTO;
import com.api.manojmobiles.entity.BulkEnquiry;
import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.enums.EnquiryStatus;
import com.api.manojmobiles.repository.BulkEnquiryRepository;
import com.api.manojmobiles.repository.ProductRepository;
import com.api.manojmobiles.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BulkEnquiryServiceTest {

    @Mock
    private BulkEnquiryRepository bulkEnquiryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @InjectMocks
    private BulkEnquiryService bulkEnquiryService;

    private Product product;
    private ProductVariant variant;
    private SubmitBulkEnquiryRequestDTO submitRequest;
    private BulkEnquiry enquiry;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Phone");

        variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProduct(product);
        variant.setColor("Black");

        submitRequest = SubmitBulkEnquiryRequestDTO.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .name("John Doe")
                .email("john@example.com")
                .mobileNumber("9876543210")
                .companyName("John Corp")
                .estimatedQuantity(50)
                .build();

        enquiry = BulkEnquiry.builder()
                .id(UUID.randomUUID())
                .product(product)
                .variant(variant)
                .name("John Doe")
                .email("john@example.com")
                .mobileNumber("9876543210")
                .companyName("John Corp")
                .estimatedQuantity(50)
                .status(EnquiryStatus.PENDING)
                .build();
    }

    @Test
    void testSubmitEnquiry_Success() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(variantRepository.findById(variant.getId())).thenReturn(Optional.of(variant));

        bulkEnquiryService.submitEnquiry(submitRequest);

        ArgumentCaptor<BulkEnquiry> captor = ArgumentCaptor.forClass(BulkEnquiry.class);
        verify(bulkEnquiryRepository).save(captor.capture());

        BulkEnquiry saved = captor.getValue();
        assertEquals("John Doe", saved.getName());
        assertEquals("john@example.com", saved.getEmail());
        assertEquals(EnquiryStatus.PENDING, saved.getStatus());
        assertEquals(product, saved.getProduct());
        assertEquals(variant, saved.getVariant());
    }

    @Test
    void testGetAllEnquiries() {
        Page<BulkEnquiry> page = new PageImpl<>(List.of(enquiry));
        when(bulkEnquiryRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

        Page<BulkEnquiryResponseDTO> result = bulkEnquiryService.getAllEnquiries(null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("John Doe", result.getContent().get(0).getName());
        assertEquals("Black", result.getContent().get(0).getVariantName());
    }

    @Test
    void testUpdateStatus() {
        when(bulkEnquiryRepository.findById(enquiry.getId())).thenReturn(Optional.of(enquiry));

        BulkEnquiryResponseDTO result = bulkEnquiryService.updateStatus(enquiry.getId(), EnquiryStatus.RESOLVED);

        assertEquals(EnquiryStatus.RESOLVED, result.getStatus());
        verify(bulkEnquiryRepository).save(enquiry);
    }
}
