package com.demo.nimn.websocket;

import com.demo.nimn.filter.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 알림 WebSocket(STOMP)의 사용자 인증과 구독·전송 권한을 확인한다 (#10).
 * SockJS 핸드셰이크는 Authorization 헤더를 보낼 수 없으므로, STOMP CONNECT 프레임의 Authorization 헤더로
 * Access Token을 받아 검증하고 토큰의 이메일로만 사용자를 정한다. 클라이언트가 보낸 userEmail·login 값은 쓰지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    // 서버가 convertAndSendToUser로 보내는 사용자 전용 큐만 구독할 수 있다.
    private static final String USER_QUEUE_PREFIX = "/user/queue/";

    private final JWTUtil jwtUtil;
    private final Map<String, String> sessionIdToUserIdMap = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        // wrap()은 복사본을 만들므로 setUser가 메시지에 반영되지 않는다. 메시지의 accessor를 직접 꺼낸다.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        // STOMP 1.2의 STOMP 명령도 Spring은 CONNECT로 처리하므로 같이 인증한다.
        if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            checkSubscription(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            // 서버에는 클라이언트 메시지를 받는 핸들러가 없다. 브로커·다른 사용자 큐로 직접 보내 가짜 알림을 만들지 못하게 한다.
            throw reject("메시지를 보낼 수 없습니다.");
        } else if (StompCommand.DISCONNECT.equals(command)) {
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                removeSession(sessionId);
            }
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String email = emailFromAccessToken(accessor.getFirstNativeHeader("Authorization"));
        accessor.setUser(new StompPrincipal(email));

        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            sessionIdToUserIdMap.put(sessionId, email);
        }
        log.debug("[WebSocket CONNECT] sessionId={}", sessionId);
    }

    private String emailFromAccessToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw reject("로그인이 필요합니다.");
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        try {
            if (jwtUtil.isExpired(token)) {
                throw reject("로그인이 필요합니다.");
            }
            String email = jwtUtil.getUsername(token);
            // Refresh Token에는 role이 없다. 토큰 용도를 별도 값으로 구분하는 작업은 #13에서 다룬다.
            String role = jwtUtil.getRole(token);
            if (email == null || email.isBlank() || role == null) {
                throw reject("로그인이 필요합니다.");
            }
            return email;
        } catch (MessageDeliveryException e) {
            throw e;
        } catch (RuntimeException e) {
            throw reject("로그인이 필요합니다.");
        }
    }

    private void checkSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (accessor.getUser() == null || destination == null || !destination.startsWith(USER_QUEUE_PREFIX)) {
            throw reject("구독할 수 없는 주소입니다.");
        }
    }

    private static MessageDeliveryException reject(String reason) {
        // STOMP ERROR 프레임의 message로 전달된다. 토큰·이메일 등 내부 정보는 넣지 않는다.
        return new MessageDeliveryException(reason);
    }

    public void removeSession(String sessionId) {
        sessionIdToUserIdMap.remove(sessionId);
        log.debug("[WebSocket DISCONNECT] sessionId={}", sessionId);
    }

    public boolean isConnected(String userEmail) {
        return sessionIdToUserIdMap.containsValue(userEmail);
    }
}
