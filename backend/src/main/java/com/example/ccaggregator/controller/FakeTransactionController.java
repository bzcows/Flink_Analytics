package com.example.ccaggregator.controller;

import com.example.ccaggregator.service.FakeTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import com.example.ccaggregator.model.RateRequest;

@RestController
@RequestMapping("/api/fake-transactions")
public class FakeTransactionController {
    private static final Logger logger = LoggerFactory.getLogger(FakeTransactionController.class);

    private final FakeTransactionService fakeTransactionService;

    @Autowired
    public FakeTransactionController(FakeTransactionService fakeTransactionService) {
        this.fakeTransactionService = fakeTransactionService;
    }

    @GetMapping("/toggle")
    public String toggleTransactions() {
        logger.info("Received request to toggle fake transactions");
        fakeTransactionService.toggle();
        String status = fakeTransactionService.isRunning() ? "Started" : "Stopped";
        //logger.info("Fake transactions {}", status);
        return status;
    }

    @PostMapping("/setRate")
    public ResponseEntity<String> setRate(@RequestBody RateRequest rateRequest) {
        int rate = rateRequest.getRate();
        if (rate > 0) {
            fakeTransactionService.setTransactionsPerSecond(rate);
            return ResponseEntity.ok("Transaction rate set to " + rate + " per second.");
        } else {
            return ResponseEntity.badRequest().body("Rate must be greater than 0.");
        }
    }
}