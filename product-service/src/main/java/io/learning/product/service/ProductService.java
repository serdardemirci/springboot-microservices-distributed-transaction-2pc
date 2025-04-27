package io.learning.product.service;

import io.learning.core.domain.Product;
import io.learning.product.devil.ProductProcessingException;
import io.learning.product.event.ProductTransactionEvent;
import io.learning.product.mapper.ProductMapper;
import io.learning.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Product createProduct(Product product) {
        return ProductMapper.map(productRepository.save(ProductMapper.map(product)));
    }

    public Optional<Product> findById(Long productId) {
        return productRepository.findById(productId).map(ProductMapper::map);
    }

    @Async
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateQuantity(String transactionId, Long productId, int quantity) {
        log.info("Updating product quantity with id: {}, quantity: {}", productId, quantity);
        findById(productId).ifPresent(prod -> {
            if (prod.getQuantity() < quantity) {
                throw new ProductProcessingException("Insufficient product quantity. Available: " + prod.getQuantity() + ", Demand: " + quantity);
            }
            prod.setQuantity(prod.getQuantity() - quantity);

            productRepository.save(ProductMapper.map(prod));

            eventPublisher.publishEvent(new ProductTransactionEvent(transactionId, prod));
        });
    }

}
