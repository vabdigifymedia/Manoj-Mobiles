package com.api.manojmobiles.service.ai.tools;

import com.api.manojmobiles.dto.product.ai.ProductSearchResult;
import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.enums.ProductStatus;
import com.api.manojmobiles.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ProductTools {
    private final ProductRepository productRepository;

    @Tool(description = """
            Search Active Products available at Manoj Mobiles.
            Search by Product Name, brand name, or category.
            Use this tool when a customer asks to find, search, browse, or check products.
            """)
    public List<ProductSearchResult> searchProducts(String query) {
        List<Product> products = productRepository
                .searchProducts(
                        query,
                        ProductStatus.ACTIVE,
                        PageRequest.of(0, 5)
                )
                .getContent();

        return products.stream().map(product -> ProductSearchResult
                .builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand().getName())
                .category(product.getCategory().getName())
                .startingPrice(getStartingPrice(product))
                .build()
        ).toList();
    }

    private BigDecimal getStartingPrice(Product product) {

        return product.getVariants()
                .stream()
                .map(ProductVariant::getSellingPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }
}
