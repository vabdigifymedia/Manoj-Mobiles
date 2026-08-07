package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.cart.AddToCartRequestDTO;
import com.api.manojmobiles.dto.cart.CartItemResponseDTO;
import com.api.manojmobiles.dto.cart.CartResponseDTO;
import com.api.manojmobiles.dto.cart.UpdateCartItemRequestDTO;
import com.api.manojmobiles.entity.Cart;
import com.api.manojmobiles.entity.CartItem;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.ProductStatus;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.CartItemRepository;
import com.api.manojmobiles.repository.CartRepository;
import com.api.manojmobiles.repository.ProductVariantRepository;
import com.api.manojmobiles.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Value("${app.cart.max-qty-per-item:5}")
    private int maxQtyPerItem;

    @Transactional
    public CartResponseDTO getCartForUser(String username) {
        Cart cart = getOrCreateCart(username);
        return mapToCartResponseDTO(cart);
    }

    private Cart getOrCreateCart(String username) {
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart newCart = Cart.builder()
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return cartRepository.save(newCart);
        });
    }

    @Transactional
    public CartResponseDTO addToCart(String username, AddToCartRequestDTO request) {
        Cart cart = getOrCreateCart(username);

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));

        if (variant.getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Product is currently unavailable");
        }

        int requestedQty = request.getQty();
        
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());
        
        CartItem cartItem;
        int newTotalQty = requestedQty;
        
        if (existingItemOpt.isPresent()) {
            cartItem = existingItemOpt.get();
            newTotalQty = cartItem.getQty() + requestedQty;
        } else {
            cartItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .priceAtAdd(variant.getSellingPrice())
                    .build();
        }

        // Check limits
        validateQuantityLimits(newTotalQty, variant);

        cartItem.setQty(newTotalQty);
        cartItemRepository.save(cartItem);
        
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return mapToCartResponseDTO(cart);
    }

    @Transactional
    public CartResponseDTO updateItemQuantity(String username, UUID itemId, UpdateCartItemRequestDTO request) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access Denied: You do not own this cart item");
        }

        ProductVariant variant = cartItem.getVariant();
        int requestedQty = request.getQty();

        validateQuantityLimits(requestedQty, variant);

        cartItem.setQty(requestedQty);
        cartItemRepository.save(cartItem);

        Cart cart = cartItem.getCart();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return mapToCartResponseDTO(cart);
    }

    @Transactional
    public CartResponseDTO removeItem(String username, UUID itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhone(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access Denied: You do not own this cart item");
        }

        Cart cart = cartItem.getCart();
        cartItemRepository.delete(cartItem);
        
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        // Fetch refreshed cart (as delete might not be flushed immediately before mapping)
        Cart updatedCart = cartRepository.findById(cart.getId()).orElseThrow();
        return mapToCartResponseDTO(updatedCart);
    }

    @Transactional
    public void clearCart(UUID cartId) {
        cartItemRepository.deleteByCartId(cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    private void validateQuantityLimits(int requestedQty, ProductVariant variant) {
        if (requestedQty > maxQtyPerItem) {
            throw new BadRequestException("Max " + maxQtyPerItem + " units allowed per item");
        }
        if (requestedQty > variant.getStockQty()) {
            throw new BadRequestException("Only " + variant.getStockQty() + " units available");
        }
    }

    private CartResponseDTO mapToCartResponseDTO(Cart cart) {
        BigDecimal cartTotal = BigDecimal.ZERO;
        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();
        
        // Items might be null if just created
        List<CartItem> items = cart.getItems() != null ? cart.getItems() : new ArrayList<>();

        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            boolean isAvailable = variant.getProduct().getStatus() == ProductStatus.ACTIVE;
            BigDecimal currentPrice = variant.getSellingPrice();
            
            // Calculate subtotal
            BigDecimal subtotal = currentPrice.multiply(BigDecimal.valueOf(item.getQty()));
            
            String stockStatus;
            if (variant.getStockQty() == 0) {
                stockStatus = "Out of stock";
                isAvailable = false;
            } else if (variant.getStockQty() <= 5) {
                stockStatus = "Only " + variant.getStockQty() + " left";
            } else {
                stockStatus = "In stock";
            }

            if (isAvailable) {
                cartTotal = cartTotal.add(subtotal);
            }

            itemDTOs.add(CartItemResponseDTO.builder()
                    .id(item.getId())
                    .variantId(variant.getId())
                    .variantName(variant.getVariantName())
                    .productName(variant.getProduct().getName())
                    .sku(variant.getSku())
                    .primaryImage(variant.getImages() != null && !variant.getImages().isEmpty() ? variant.getImages().get(0).getUrl() : null)
                    .qty(item.getQty())
                    .priceAtAdd(item.getPriceAtAdd())
                    .currentPrice(currentPrice)
                    .subtotal(subtotal)
                    .stockStatus(stockStatus)
                    .isAvailable(isAvailable)
                    .build());
        }

        return CartResponseDTO.builder()
                .id(cart.getId())
                .cartTotal(cartTotal)
                .items(itemDTOs)
                .build();
    }
}
