package com.example.ccaggregator.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        logger.info("Added WebSocket session: {}", session.getId());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
        logger.info("Removed WebSocket session: {}", session.getId());
    }

    public WebSocketSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void sendMessageToSession(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    safeSendMessage(session, message);
                }
                logger.debug("Sent message to session {}: {}", session.getId(), message);
            } else {
                logger.warn("Attempted to send message to closed session: {}", session.getId());
            }
        } catch (IOException e) {
            logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
        }
    }
    
    private void safeSendMessage(WebSocketSession session, String message) throws IOException {
        int retryCount = 0;
        while (true) {
            try {
                session.sendMessage(new TextMessage(message));
                break;
            } catch (IllegalStateException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("TEXT_PARTIAL_WRITING") && retryCount < 5) {
                    retryCount++;
                    logger.warn("Retrying sendMessage due to TEXT_PARTIAL_WRITING state (attempt {})", retryCount);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while retrying sendMessage", ie);
                    }
                } else {
                    throw ex;
                }
            }
        }
    }
    
    public void broadcastMessage(String message) {
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    synchronized(session) {
                        safeSendMessage(session, message);
                    }
                    logger.debug("Broadcast message to session {}: {}", session.getId(), message);
                }
            } catch (IOException e) {
                logger.error("Failed to broadcast message to session {}: {}", session.getId(), e.getMessage());
            }
        });
    }
}