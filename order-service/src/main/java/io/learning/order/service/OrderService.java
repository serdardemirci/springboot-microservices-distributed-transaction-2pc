package io.learning.order.service;

import io.learning.core.domain.*;
import io.learning.order.devil.InSufficientFundException;
import io.learning.order.domain.Order;
import io.learning.order.event.OrderTransactionEvent;
import io.learning.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private static final String TXN_ID_HEADER = "X-Txn-ID";

    private final Random random = new Random();
    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional
    public Order createOrder(Order order) {
        DistributedTransaction transaction = restTemplate.postForObject("http://transaction-server/transactions", new DistributedTransaction(), DistributedTransaction.class);
        log.info("Transaction created: {}", transaction);

        Order savedOrder = orderRepository.save(order);

        addTransactionParticipant(transaction, "product-service", DistributedTransactionStatus.NEW);
        Product product = updateProduct(transaction, savedOrder);
        log.info("Product updated: {}", product);
        int totalAmount = product.getPrice() * order.getQuantity();

        addTransactionParticipant(transaction, "account-service", DistributedTransactionStatus.NEW);
        Account account = restTemplate.getForObject("http://account-service/accounts/customer/{customerId}", Account.class, order.getCustomerId());
        log.info("Account :{}", account);
        if (account.getBalance() >= totalAmount) {
            log.info("Withdrawing money: {}", totalAmount);
            withdraw(transaction, account.getId(), totalAmount);
        } else {
            throw new InSufficientFundException("Insufficient funds. Balance: " + account.getBalance() + ", orderAmount:  " + totalAmount);
        }
        eventPublisher.publishEvent(new OrderTransactionEvent(transaction.getId()));

        return savedOrder;
    }

    protected Product updateProduct(DistributedTransaction transaction, Order order) {
        HttpEntity<Void> requestEntity = new HttpEntity<>(prepareHeaders(transaction.getId()));
        return restTemplate.exchange(
                "http://product-service/products/{id}/quantity/{quantity}",
                HttpMethod.PUT,
                requestEntity,
                Product.class,
                order.getProductId(),
                order.getQuantity()).getBody();
    }

    protected Account withdraw(DistributedTransaction transaction, Long accountId, int amount) {
        HttpEntity<Void> requestEntity = new HttpEntity<>(prepareHeaders(transaction.getId()));
        return restTemplate.exchange("http://account-service/accounts/{id}/withdrawl/{amount}", HttpMethod.PUT, requestEntity, Account.class, accountId, amount).getBody();
    }

    protected void addTransactionParticipant(DistributedTransaction transaction, String serviceId, DistributedTransactionStatus status) {
        HttpEntity<DistributedTransactionParticipant> requestEntity = new HttpEntity<>(new DistributedTransactionParticipant(serviceId, status));
        restTemplate.exchange("http://transaction-server/transactions/{id}/participants", HttpMethod.PUT, requestEntity, Object.class, transaction.getId());
    }

    private HttpHeaders prepareHeaders(String transactionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(TXN_ID_HEADER, transactionId);
        return headers;
    }

}
