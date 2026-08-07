package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.cart.AddToCartRequestDTO;
import com.api.manojmobiles.dto.cart.CartResponseDTO;
import com.api.manojmobiles.dto.cart.UpdateCartItemRequestDTO;
import com.api.manojmobiles.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Cart Management", description = "Endpoints for managing the customer's shopping cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get user's cart", description = "Fetches the current user's cart. Auto-creates an empty cart if one doesn't exist.")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponseDTO>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        CartResponseDTO cart = cartService.getCartForUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully", cart));
    }

    @Operation(summary = "Add item to cart", description = "Adds a product variant to the cart. Enforces max quantity and stock limits.")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponseDTO>> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddToCartRequestDTO request) {
        CartResponseDTO cart = cartService.addToCart(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cart));
    }

    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of a specific cart item. Fails if the user does not own the cart item.")
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponseDTO>> updateCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID of the cart item to update") @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequestDTO request) {
        CartResponseDTO cart = cartService.updateItemQuantity(userDetails.getUsername(), itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", cart));
    }

    @Operation(summary = "Remove item from cart", description = "Deletes a specific item from the cart. Fails if the user does not own the cart item.")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponseDTO>> removeCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID of the cart item to remove") @PathVariable UUID itemId) {
        CartResponseDTO cart = cartService.removeItem(userDetails.getUsername(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cart));
    }

    @Operation(summary = "Clear the cart", description = "Removes all items from the current user's cart.")
    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<String>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        CartResponseDTO cart = cartService.getCartForUser(userDetails.getUsername());
        cartService.clearCart(cart.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", null));
    }
}
