package io.learning.init;

import io.learning.core.domain.Account;
import io.learning.core.domain.Product;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.client.RestTemplate;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@RequiredArgsConstructor
public class StartInitService {

    private final RestTemplate restTemplate;


    public static void main(String[] args) {
        SpringApplication.run(StartInitService.class, args);
    }


    @PostConstruct
    public void initializeEnvironment() {
        log.info("Initializing environment...");

        var account = new Account();
        account.setId(1L);
        account.setCustomerId(55L);
        account.setBalance(15000);
        restTemplate.postForObject("http://localhost:8080/accounts", account, Account.class);

        var product = new Product();
        product.setId(1L);
        product.setName("Schokolade");
        product.setQuantity(1_000_000);
        product.setPrice(2);
        restTemplate.postForObject("http://localhost:8082/products", product, Product.class);

        System.exit(0);
    }

}
