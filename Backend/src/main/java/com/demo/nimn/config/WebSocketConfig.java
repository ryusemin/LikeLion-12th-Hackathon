package com.demo.nimn.config;

import com.demo.nimn.websocket.CustomChannelInterceptor;
import com.demo.nimn.websocket.CustomWebSocketHandlerDecoratorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CustomChannelInterceptor customChannelInterceptor;
    private final CustomWebSocketHandlerDecoratorFactory customWebSocketHandlerDecoratorFactory;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic");  // queue
        config.setApplicationDestinationPrefixes("/app");  // 클라이언트 전송
        config.setUserDestinationPrefix("/user");  // user prefix 설정
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 사용자는 연결 주소의 값이 아니라 STOMP CONNECT의 Access Token으로 정한다(CustomChannelInterceptor, #10).
        // 허용 Origin은 HTTP CORS 설정(WebConfig)과 같은 목록을 쓴다.
        registry.addEndpoint("/ws/notification")
                .setAllowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000",
                        "https://nimn.store", "http://nimn.store")
                .withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setDecoratorFactories(customWebSocketHandlerDecoratorFactory);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(customChannelInterceptor);
    }
}
