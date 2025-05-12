package io.learning.order.service;

import io.learning.core.domain.DistributedTransaction;
import io.learning.order.event.OrderTransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class EventBus {

    private final List<DistributedTransaction> transactions;
    private final List<OrderTransactionEvent> events;

    public EventBus() {
        this.transactions = new ArrayList<>();
        this.events = new ArrayList<>();
    }

    public void sendTransaction(DistributedTransaction transaction) {
        transactions.add(transaction);
    }

    public DistributedTransaction receiveTransaction(String eventId) {
        DistributedTransaction transaction = null;
        while (transaction == null) {
            transaction = transactions.stream().filter(tx -> tx.getId().equals(eventId)).findAny().orElse(null);
            transactions.remove(transaction);
            if (transaction != null) {
                return transaction;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException ex) {
                log.error("Error while received event for: {}, Cause:{}", eventId, ex);
            }
        }
        return transaction;
    }

    public void sendEvent(OrderTransactionEvent event) {
        events.add(event);
    }

    public OrderTransactionEvent receiveEvent(String eventId) {
        OrderTransactionEvent event = null;
        while (event == null) {
            event = events.stream().filter(evnt -> evnt.getTransactionId().equals(eventId)).findAny().orElse(null);
            events.remove(event);
            if (event != null) {
                return event;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException ex) {
                log.error("Error while received event for: {}, Cause:{}", eventId, ex);
            }
        }
        return event;
    }

}
