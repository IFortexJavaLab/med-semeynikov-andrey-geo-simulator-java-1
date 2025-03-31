package com.ifortex.internship.geosimulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebSocketClientService {

    private static final WebSocketHttpHeaders HEADERS = new WebSocketHttpHeaders();

    StompSession stompSession;

    @Value("${app.websocket.ws_url}")
    String wsURL;

    public void connect() {
        try {
            WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
            converter.setObjectMapper(objectMapper);

            stompClient.setMessageConverter(converter);

            CompletableFuture<StompSession> future = stompClient.connectAsync(
                wsURL, HEADERS, new StompSessionHandlerAdapter() {
                }
            );

            this.stompSession = future.get();
            log.info("WebSocket STOMP session established with {}", wsURL);

        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to connect to WebSocket server: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public StompSession getSession() {
        if (stompSession == null || !stompSession.isConnected()) {
            connect();
        }
        return stompSession;
    }
}

