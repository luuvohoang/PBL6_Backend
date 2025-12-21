// File: src/main/java/com/safetyconstruction/backend/configuration/WebSocketConfig.java
package com.safetyconstruction.backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Kích hoạt máy chủ WebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Định nghĩa các "chủ đề" (topics) mà client có thể lắng nghe
        // Frontend sẽ subscribe vào /topic/...
        registry.enableSimpleBroker("/topic");

        // Định nghĩa tiền tố cho các message gửi từ client đến server (nếu có)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Nếu Nginx chuyển tiếp /safetyconstruction/ws vào, bạn nên đăng ký như sau:
        registry.addEndpoint("/ws", "/safetyconstruction/ws")
                .setAllowedOriginPatterns("*") // Dùng Patterns thay vì Origins để tránh lỗi Credential
                .withSockJS();
    }
}
