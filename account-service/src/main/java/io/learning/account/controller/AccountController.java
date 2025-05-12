package io.learning.account.controller;

import io.learning.account.domain.Account;
import io.learning.account.service.AccountService;
import io.learning.account.service.EventBus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes REST API Interface for interacting with AccountService.
 *
 * @author Anil Jaglan
 * @version 1.0
 */
@RestController
@RequestMapping("/accounts")
@Tag(name = "Accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final EventBus eventBus;

    @PostMapping
    @Operation(summary = "Create a new account")
    public Account createAccount(@RequestBody Account account) {
        return accountService.createAccount(account);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get account by customer ID")
    public Account findByCustomerId(@PathVariable("customerId") Long customerId) {
        return accountService.findByCustomerId(customerId);
    }

    @PutMapping("/{id}/deposit/{amount}")
    @Operation(summary = "Deposit money in customer account")
    public Account deposit(@PathVariable("id") Long accountId, @PathVariable("amount") int amount, @RequestHeader("X-Txn-ID") String transactionId) {
        accountService.deposit(accountId, amount, transactionId);
        return eventBus.receiveEvent(transactionId).getAccount();
    }

    @PutMapping("/{id}/withdrawal/{amount}")
    @Operation(summary = "Withdraw money from customer account")
    public Account withdrawal(@PathVariable("id") Long accountId, @PathVariable("amount") int amount, @RequestHeader("X-Txn-ID") String transactionId) {
        accountService.withdrawal(accountId, amount, transactionId);
        return eventBus.receiveEvent(transactionId).getAccount();
    }

}
