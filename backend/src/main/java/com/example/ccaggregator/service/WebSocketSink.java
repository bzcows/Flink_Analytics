package com.example.ccaggregator.service;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.ccaggregator.util.SpringContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.Map;

public class WebSocketSink extends RichSinkFunction<String> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSink.class);
    
    private final String sessionId;
    private final String queryId;
    private final String[] fieldNames;
    private transient ObjectMapper objectMapper;
    private transient WebSocketService webSocketService;
    private transient WebSocketSession session;

    public WebSocketSink(String sessionId, String queryId,String[] fieldNames) {
        this.sessionId = sessionId;
        this.queryId = queryId;
        this.fieldNames = fieldNames;
       
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        // Initialize ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        // Get Spring context and retrieve WebSocketService
        ApplicationContext applicationContext = SpringContextUtil.getApplicationContext();
        if (applicationContext != null) {
            webSocketService = applicationContext.getBean(WebSocketService.class);
        } else {
            throw new RuntimeException("Failed to get Spring application context");
        }
        
        // Initialize session
        if (webSocketService != null) {
            session = webSocketService.getSession(sessionId);
            if (session == null) {
                throw new RuntimeException("Failed to get WebSocket session: " + sessionId);
            }
        } else {
            throw new RuntimeException("Failed to get WebSocketService bean");
        }
        
        logger.info("WebSocketSink initialized for query: {} session: {}", queryId, sessionId);
    }

    @Override
    public void invoke(String value, Context context) {
        try {

            if (value.contains("retract")) {
                return;
            }

            if (session == null || !session.isOpen()) {
                session = webSocketService.getSession(sessionId);
                if (session == null || !session.isOpen()) {
                    logger.error("WebSocket session {} is no longer available for query {}", sessionId, queryId);
                    return;
                }
            }

            Map<String, Object> message = Map.of(
                "type", "query_result",
                "queryId", queryId,
                "fieldNames", fieldNames,
                "data", value,
                "timestamp", System.currentTimeMillis()
            );
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketService.sendMessageToSession(session, jsonMessage);
            
            logger.debug("Sent query result for query: {} to session {}: {}", queryId, sessionId, value);
        } catch (Exception e) {
            logger.error("Failed to send query result for query: {} to session {}: {}", queryId, sessionId, value, e);
        }
    }

    @Override
    public void close() throws Exception {
        logger.info("Closing WebSocketSink for query: {} session: {}", queryId, sessionId);
        super.close();
    }
}
