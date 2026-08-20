package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.cart.AddToCartRequestDTO;
import com.api.manojmobiles.dto.cart.CartResponseDTO;
import com.api.manojmobiles.entity.Cart;
import com.api.manojmobiles.entity.CartItem;
import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.ProductStatus;

import com.api.manojmobiles.repository.CartItemRepository;
import com.api.manojmobiles.repository.CartRepository;
import com.api.manojmobiles.repository.ProductVariantRepository;
import com.api.manojmobiles.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cartService, "maxQtyPerItem", 5);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");

        Product product = new Product();
        product.setStatus(ProductStatus.ACTIVE);
        
        variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProduct(product);
        variant.setSellingPrice(BigDecimal.valueOf(100));
        variant.setStockQty(10);
    }

    @Test
    void testGuestCreatesCart() {
        String guestId = "guest-123";
        when(cartRepository.findByGuestId(guestId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> {
            Cart c = (Cart) i.getArguments()[0];
            c.setId(UUID.randomUUID());
            return c;
        });

        CartResponseDTO res = cartService.getCart(null, guestId);
        assertNotNull(res);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testGuestAddsItem() {
        String guestId = "guest-123";
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setGuestId(guestId);

        when(cartRepository.findByGuestId(guestId)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId())).thenReturn(Optional.empty());

        AddToCartRequestDTO req = new AddToCartRequestDTO();
        req.setVariantId(variant.getId());
        req.setQty(2);

        CartResponseDTO res = cartService.addToCart(null, guestId, req);
        assertNotNull(res);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void testMergeGuestCartIntoEmptyCustomerCart() {
        String guestId = "guest-123";
        String username = "test@test.com";

        Cart guestCart = new Cart();
        guestCart.setId(UUID.randomUUID());
        guestCart.setGuestId(guestId);
        
        CartItem item = new CartItem();
        item.setVariant(variant);
        item.setQty(1);
        guestCart.setItems(List.of(item));

        when(cartRepository.findByGuestId(guestId)).thenReturn(Optional.of(guestCart));
        when(userRepository.findByEmail(username)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        cartService.mergeGuestCart(username, guestId);

        verify(cartRepository).save(guestCart);
        assertEquals(user, guestCart.getUser());
        assertNull(guestCart.getGuestId());
    }

    @Test
    void testMergeGuestCartIntoExistingCustomerCartWithCombinedQuantities() {
        String guestId = "guest-123";
        String username = "test@test.com";

        Cart guestCart = new Cart();
        guestCart.setId(UUID.randomUUID());
        guestCart.setGuestId(guestId);
        
        CartItem guestItem = new CartItem();
        guestItem.setVariant(variant);
        guestItem.setQty(2);
        guestCart.setItems(List.of(guestItem));

        Cart userCart = new Cart();
        userCart.setId(UUID.randomUUID());
        userCart.setUser(user);
        
        CartItem userItem = new CartItem();
        userItem.setVariant(variant);
        userItem.setQty(1);
        List<CartItem> userItems = new ArrayList<>();
        userItems.add(userItem);
        userCart.setItems(userItems);

        when(cartRepository.findByGuestId(guestId)).thenReturn(Optional.of(guestCart));
        when(userRepository.findByEmail(username)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(userCart));

        cartService.mergeGuestCart(username, guestId);

        assertEquals(3, userItem.getQty());
        verify(cartItemRepository).save(userItem);
        verify(cartRepository).delete(guestCart);
        verify(cartRepository).save(userCart);
    }
    
    @Test
    void testMergeWithInvalidGuestId() {
        cartService.mergeGuestCart("test@test.com", "");
        verify(cartRepository, never()).findByGuestId(anyString());
    }
}
