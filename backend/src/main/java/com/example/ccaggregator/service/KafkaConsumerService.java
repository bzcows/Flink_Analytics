package com.example.ccaggregator.service;

import com.example.ccaggregator.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;
    private final FlinkQueryService flinkQueryService;

    public KafkaConsumerService(WebSocketService webSocketService, FlinkQueryService flinkQueryService) {
        this.webSocketService = webSocketService;
        this.flinkQueryService = flinkQueryService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @KafkaListener(topics = "${kafka.topic.transactions}")
    public void consume(Transaction transaction) {
        // Set timestamp for the transaction
        transaction.setTxTimestamp(System.currentTimeMillis());

        try {
            // Forward transaction to FlinkQueryService
            String jsonTransaction = objectMapper.writeValueAsString(transaction);
            flinkQueryService.processTransaction(jsonTransaction);

            // Broadcast transaction to WebSocket clients
            String message = objectMapper.writeValueAsString(
                Map.of(
                    "type", "transactions",
                    "transactions", List.of(transaction)
                )
            );
            logger.debug("Broadcasting new transaction to WebSocket clients: {}", transaction.getTxNumber());
            webSocketService.broadcastMessage(message);
        } catch (Exception e) {
            logger.error("Failed to process and broadcast transaction: {}", transaction.getTxNumber(), e);
        }
    }
}