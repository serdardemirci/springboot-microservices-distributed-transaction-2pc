package io.learning.product.listener;

import io.learning.core.domain.DistributedTransaction;
import io.learning.core.domain.DistributedTransactionStatus;
import io.learning.core.eventlistener.TransactionListener;
import io.learning.product.devil.ProductProcessingException;
import io.learning.product.event.ProductTransactionEvent;
import io.learning.product.service.EventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductTransactionListener implements TransactionListener<ProductTransactionEvent> {

    private final RestTemplate restTemplate;
    private final EventBus eventBus;

    @Override
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleEvent(ProductTransactionEvent event) throws ProductProcessingException {
        log.debug("Handling event before commit: {}", event);
        eventBus.sendEvent(event);

        DistributedTransaction transaction = null;
        int count = 3000;
        while (count > 0) {
            transaction = eventBus.receiveTransaction(event.getTransactionId());
            if (transaction == null) {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException ex) {
                    log.error("Error while receiving transaction for: {}. Cause: {}", event.getTransactionId(), ex);
                }
                --count;
            } else {
                break;
            }
        }
        if (transaction == null || transaction.getStatus() != DistributedTransactionStatus.CONFIRMED) {
            log.info("Transction received after waiting: {}", transaction);
            throw new ProductProcessingException("Distributed transaction wasn't confirmed for txnId: " + event.getTransactionId());
        }
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleAfterRollback(ProductTransactionEvent event) {
        log.debug("Handling event after rollback : {}", event);
        restTemplate.put(
                "http://transaction-server/transactions/{transactionId}/participants/{serviceId}/status/{status}",
                null,
                event.getTransactionId(),
                "product-service",
                DistributedTransactionStatus.TO_ROLLBACK);
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCompletion(ProductTransactionEvent event) {
        log.debug("Handling event after completion : {}", event);
        restTemplate.put(
                "http://transaction-server/transactions/{transactionId}/participants/{serviceId}/status/{status}",
                null,
                event.getTransactionId(),
                "product-service",
                DistributedTransactionStatus.CONFIRMED);
    }

}
