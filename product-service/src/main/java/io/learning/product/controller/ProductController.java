package io.learning.product.controller;

import io.learning.core.domain.Product;
import io.learning.product.devil.ProductNotFoundException;
import io.learning.product.service.EventBus;
import io.learning.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes REST API Interface for interacting with ProductService.
 *
 * @author Anil Jaglan
 * @version 1.0
 */
@RestController
@RequestMapping("/products")
@Tag(name = "Products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final EventBus eventBus;

    @PostMapping
    @Operation(summary = "Create a new product")
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find product by ID")
    public Product findById(@PathVariable("id") Long productId) {
        return productService.findById(productId).orElseThrow(() -> new ProductNotFoundException("Product not found for id: " + productId));
    }

    @PutMapping("/{id}/quantity/{quantity}")
    @Operation(summary = "Update product quantity")
    public Product updateQuantity(@RequestHeader("X-Txn-ID") String transactionId, @PathVariable("id") Long productId, @PathVariable("quantity") int quantity) {
        productService.updateQuantity(transactionId, productId, quantity);
        return eventBus.receiveEvent(transactionId).getProduct();
    }

}
