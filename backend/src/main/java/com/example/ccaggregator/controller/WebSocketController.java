package com.example.ccaggregator.controller;

import com.example.ccaggregator.service.WebSocketService;
import com.example.ccaggregator.service.FlinkQueryService;
import com.example.ccaggregator.service.QueryClusterManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Component
public class WebSocketController extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    private final WebSocketService webSocketService;
    private final FlinkQueryService flinkQueryService;
    private final QueryClusterManager clusterManager;
    private final ObjectMapper objectMapper;
    private final Map<String, String> sessionToQueryId = new ConcurrentHashMap<>();

    public WebSocketController(
            WebSocketService webSocketService,
            FlinkQueryService flinkQueryService,
            QueryClusterManager clusterManager) {
        this.webSocketService = webSocketService;
        this.flinkQueryService = flinkQueryService;
        this.clusterManager = clusterManager;
        
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established for session: {}", session.getId());
        webSocketService.addSession(session);
        
        webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
            "type", "connection_established",
            "message", "Connected successfully"
        )));
    }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
    String sessionId = session.getId();

    if ("query".equals(payload.get("type"))) {
      String query = (String) payload.get("query");
      String queryId = (String) payload.get("queryId"); // Get queryId from frontend
      String currentQueryId = sessionToQueryId.get(sessionId);

      if (currentQueryId != null) {
        webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
            "type", "query_error",
            "message", "A query is already running for this session"
        )));
        return;
      }

      try {
        sessionToQueryId.put(sessionId, queryId);

        Future<Void> queryFuture = flinkQueryService.executeQuery(session, query, queryId);
        CompletableFuture.runAsync(() -> {
          try {
            queryFuture.get(); // Wait for the query to complete
            webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
                "type", "query_complete",
                "message", "Query execution completed"
            )));
          } catch (Exception e) {
            try {
              webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
                  "type", "query_error",
                  "message", "Query execution failed: " + e.getMessage()
              )));
            } catch (Exception ex) {
              logger.error("Failed to send query error message", ex);
            }
          } finally {
            sessionToQueryId.remove(sessionId);
          }
        });

        webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
            "type", "query_started",
            "message", "Query execution started",
            "queryId", queryId // Send queryId back for confirmation
        )));
      } catch (Exception e) {
        sessionToQueryId.remove(sessionId);
        webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
            "type", "query_error",
            "message", "Failed to execute query: " + e.getMessage()
        )));
      }
    } else if ("delete_query".equals(payload.get("type"))) {
      String queryId = (String) payload.get("queryId"); // Get queryId from frontend

      if (queryId == null) {
        webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
            "type", "query_error",
            "message", "No queryId provided for deletion" // Improved error message
        )));
        return;
      }

      try {
        // Send query_deleting message before attempting to cancel
        webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
            "type", "query_deleting",
            "message", "Initiating query deletion...",
            "queryId", queryId
        )));

        // Remove the query mapping immediately to stop processing new results
        sessionToQueryId.entrySet().removeIf(entry -> queryId.equals(entry.getValue()));

        CompletableFuture.runAsync(() -> {
          try {
            flinkQueryService.cancelQuery(queryId);
            webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
                "type", "query_deleted",
                "message", "Query successfully terminated",
                "queryId", queryId
            )));
          } catch (Exception e) {
            try {
              webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
                  "type", "query_error",
                  "message", "Failed to cancel query: " + e.getMessage(),
                  "queryId", queryId
              )));
            } catch (Exception ex) {
              logger.error("Failed to send query cancellation error message", ex);
            }
          }
        });
      } catch (Exception e) {
        logger.error("Error during query cancellation for queryId {}: {}", queryId, e.getMessage());
        try {
          webSocketService.sendMessageToSession(session, objectMapper.writeValueAsString(Map.of(
              "type", "query_error",
              "message", "Error initiating query deletion: " + e.getMessage(),
              "queryId", queryId
          )));
        } catch (Exception ex) {
          logger.error("Failed to send error message", ex);
        }
      }
    }
  }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String sessionId = session.getId();
        logger.info("WebSocket connection closed for session: {}", sessionId);
        
        // Cancel any running query for this session
        String queryId = sessionToQueryId.remove(sessionId);
        if (queryId != null) {
            try {
                flinkQueryService.cancelQuery(queryId);
            } catch (Exception e) {
                logger.error("Error cancelling query during session cleanup: {}", queryId, e);
            }
        }
        
        webSocketService.removeSession(session);
    }
}
