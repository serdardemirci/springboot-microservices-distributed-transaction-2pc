package io.learning.order.controller;

import io.learning.order.domain.Order;
import io.learning.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes REST API Interface for interacting with OrderService.
 *
 * @author Anil Jaglan
 * @version 1.0
 */
@RestController
@Tag(name = "Orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @Operation(summary = "Create a new order")
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get an order")
    public ResponseEntity<Order> getOrder(@PathVariable("id") Long orderId) {
        return orderService.getOrderById(orderId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

}
