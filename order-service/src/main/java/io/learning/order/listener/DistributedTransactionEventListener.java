package io.learning.order.listener;

import io.learning.core.domain.DistributedTransaction;
import io.learning.order.service.EventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DistributedTransactionEventListener {

    private final EventBus eventBus;

    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue("txn-events-order"), exchange = @Exchange(type = ExchangeTypes.TOPIC, name = "txn-events"))
    })
    public void onMessage(DistributedTransaction transaction) {
        log.debug("Transaction message received: {}", transaction);
        eventBus.sendTransaction(transaction);
    }

}
