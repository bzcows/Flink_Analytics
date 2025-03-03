package com.example.ccaggregator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import com.example.ccaggregator.model.Transaction;

import javax.annotation.PostConstruct;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class FakeTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(FakeTransactionService.class);

    private final KafkaTemplate<String, Transaction> kafkaTemplate;
    private final Random random = new Random();
    private int transactionsPerSecond = 1;
    private Timer timer;
    private boolean running = false;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public FakeTransactionService(KafkaTemplate<String, Transaction> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public boolean isRunning() {
        return running;
    }

    public void toggle() {
        if (running) {
            stop();
        } else {
            start();
        }
    }

    private void start() {
        running = true;
        timer = new Timer();
        long delay = (long) (1000.0 / transactionsPerSecond); // Calculate delay in milliseconds
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                generateTransaction();
            }
        }, 0, delay);
    }

    public void setTransactionsPerSecond(int transactionsPerSecond) {
        this.transactionsPerSecond = transactionsPerSecond;
        if (running) {
            stop(); // Restart to apply new rate
            start();
        }
    }

    private void stop() {
        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void generateTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTxNumber("TX" + (100000 + random.nextInt(900000)));
        transaction.setCcy("USD");
        transaction.setAmount(Double.parseDouble(String.format("%.2f", 1+ random.nextDouble() * 1000)));
        transaction.setTxDate(LocalDateTime.now().format(dateFormatter));
        transaction.setCcType(random.nextBoolean() ? "VISA" : "MASTERCARD");
        transaction.setMerchantId("MERCH" + (1000 + random.nextInt(9)));
        
        // Generate credit card number (16 digits)
        StringBuilder ccNumber = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            ccNumber.append(random.nextInt(10));
        }
        transaction.setCcNumber(ccNumber.toString());
        
        // Set merchant type
        transaction.setMerchantType(Transaction.MERCHANT_TYPES[random.nextInt(Transaction.MERCHANT_TYPES.length)]);
        
        // Set transaction location (except for ONLINE transactions)
        if (!Transaction.ONLINE.equals(transaction.getMerchantType())) {
            // Generate random coordinates
            double longitude = -180 + random.nextDouble() * 360; // Range: -180 to 180
            double latitude = -90 + random.nextDouble() * 180;   // Range: -90 to 90
            transaction.setTxLocation(new Transaction.TxLocation(longitude, latitude));
        }
        
        //logger.info("Generated fake transaction: {}", transaction);
        try {
            kafkaTemplate.send("transactions", transaction)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        //logger.info("Successfully sent transaction to Kafka topic 'transactions': {}", transaction.getTxNumber());
                    } else {
                        logger.error("Failed to send transaction to Kafka: {}", ex.getMessage());
                    }
                });
        } catch (Exception e) {
            logger.error("Error while sending transaction to Kafka", e);
        }
    }
}