package com.demo.nimn.websocket;

import com.demo.nimn.enums.NotificationType;
import com.demo.nimn.filter.JWTUtil;
import com.demo.nimn.repository.notification.NotificationRepository;
import com.demo.nimn.service.ai.AiService;
import com.demo.nimn.service.food.WeeklyFoodPlanService;
import com.demo.nimn.service.notification.NotificationService;
import com.siot.IamportRestClient.IamportClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * 알림 WebSocket(STOMP)이 클라이언트가 적어 보낸 이메일이 아니라 검증한 Access Token으로 사용자를 정하는지 확인한다 (#10).
 * 실제 포트로 서버를 띄우고 프론트와 같은 SockJS + STOMP 클라이언트로 연결한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:nimn-ws-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=DAY",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@ActiveProfiles({"test", "dev"})
class NotificationWebSocketAuthTest {

    private static final String A = "member-a@example.invalid";
    private static final String B = "member-b@example.invalid";
    private static final String USER_QUEUE = "/user/queue/notification";
    private static final long WAIT_SECONDS = 5;
    private static final long QUIET_MILLIS = 700;

    @LocalServerPort private int port;
    @Value("${spring.jwt.secret}") private String jwtSecret;
    @Autowired private JWTUtil jwtUtil;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private SessionManageService sessionManageService;
    @Autowired private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @MockitoBean private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean private JavaMailSender javaMailSender;
    @MockitoBean private IamportClient iamportClient;
    @MockitoBean private AiService aiService;
    @MockitoBean private WeeklyFoodPlanService weeklyFoodPlanService;

    private WebSocketStompClient stompClient;
    private String tokenA;
    private String tokenB;
    private final List<StompSession> openSessions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        tokenA = jwtUtil.createAccessJwt(A, "ROLE_USER");
        tokenB = jwtUtil.createAccessJwt(B, "ROLE_USER");
    }

    @AfterEach
    void cleanUp() throws InterruptedException {
        openSessions.forEach(session -> {
            if (session.isConnected()) session.disconnect();
        });
        // 서버가 DISCONNECT를 처리해 연결 기록을 지울 때까지 기다린다. 다음 테스트의 연결 여부 검사에 섞이지 않게 한다.
        long deadline = System.currentTimeMillis() + 2_000;
        while ((sessionManageService.isUserConnected(A) || sessionManageService.isUserConnected(B))
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        stompClient.stop();
        notificationRepository.deleteAll();
    }

    // 정상 연결: 토큰만으로 연결하고 자기 알림을 받는다.

    @Test
    void validTokenConnectsAsTokenOwnerAndReceivesOwnNotification() throws Exception {
        Connection a = connect(null, bearer(tokenA), null);
        BlockingQueue<String> inbox = subscribe(a.session, A);

        assertThat(sessionManageService.isUserConnected(A)).isTrue();
        notificationService.testNotification(NotificationType.DIET, A, "for-a", null);

        assertThat(inbox.poll(WAIT_SECONDS, TimeUnit.SECONDS)).contains("for-a");
    }

    // 사칭: A의 토큰에 B의 이메일을 붙여도 A로 연결된다.

    @Test
    void emailInQueryAndHeaderCannotImpersonateAnotherMember() throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", bearer(tokenA));
        connectHeaders.add("userEmail", B);
        connectHeaders.setLogin(B);
        Connection a = connect("?userEmail=" + B, connectHeaders, null);
        BlockingQueue<String> inbox = subscribe(a.session, A);

        assertThat(sessionManageService.isUserConnected(A)).isTrue();
        assertThat(sessionManageService.isUserConnected(B)).isFalse();

        notificationService.testNotification(NotificationType.DIET, B, "for-b", null);
        assertThat(inbox.poll(QUIET_MILLIS, TimeUnit.MILLISECONDS)).isNull();

        notificationService.testNotification(NotificationType.DIET, A, "for-a", null);
        assertThat(inbox.poll(WAIT_SECONDS, TimeUnit.SECONDS)).contains("for-a");
    }

    // 인증 실패: 토큰이 없거나 쓸 수 없으면 CONNECT를 거부한다.
    // 예전 프론트처럼 이메일을 함께 보내도 그 이메일로 연결되지 않아야 한다.

    @Test
    void connectWithoutTokenIsRejectedEvenWithEmail() throws Exception {
        assertConnectRejected(emailOnlyHeaders(A));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        String expired = Jwts.builder()
                .claim("email", A).claim("role", "ROLE_USER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)), SignatureAlgorithm.HS256)
                .compact();
        assertConnectRejected(withEmail(bearer(expired), A));
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String tampered = tokenA.substring(0, tokenA.length() - 4) + "AAAA";
        assertConnectRejected(withEmail(bearer(tampered), A));
    }

    @Test
    void refreshTokenCannotBeUsedToConnect() throws Exception {
        assertConnectRejected(withEmail(bearer(jwtUtil.createRefreshJwt(A)), A));
    }

    @Test
    void stompCommandWithoutTokenIsRejectedLikeConnect() throws Exception {
        // STOMP 1.2의 STOMP 명령은 Spring이 CONNECT와 똑같이 처리한다. 토큰 없이 연결되면 안 된다.
        // Spring 클라이언트는 CONNECT만 보내므로, SockJS의 순수 WebSocket 주소로 프레임을 직접 보낸다.
        BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        org.springframework.web.socket.WebSocketSession raw = new StandardWebSocketClient().execute(
                new org.springframework.web.socket.handler.TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(@NonNull org.springframework.web.socket.WebSocketSession session,
                                                     @NonNull org.springframework.web.socket.TextMessage message) {
                        frames.add(message.getPayload());
                    }
                }, "ws://localhost:" + port + "/ws/notification/websocket").get(WAIT_SECONDS, TimeUnit.SECONDS);
        try {
            raw.sendMessage(new org.springframework.web.socket.TextMessage(
                    "STOMP\naccept-version:1.2\nhost:localhost\nuserEmail:" + A + "\n\n\u0000"));

            String reply = frames.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            assertThat(reply).isNotNull().startsWith("ERROR");
            assertThat(sessionManageService.isUserConnected(A)).isFalse();
        } finally {
            if (raw.isOpen()) raw.close();
        }
    }

    // 구독·전송 권한: 사용자 전용 큐만 구독하고, 클라이언트가 메시지를 보내지 못한다.

    @Test
    void subscribingToSharedTopicIsRejected() throws Exception {
        Connection a = connect(null, bearer(tokenA), null);
        a.session.subscribe("/topic/notification/" + B, new TextFrameHandler(new LinkedBlockingQueue<>()));

        assertThat(a.handler.error.get(WAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo("구독할 수 없는 주소입니다.");
        assertSessionClosed(a);
    }

    @Test
    void clientCannotSendFakeNotificationToAnotherMember() throws Exception {
        Connection b = connect(null, bearer(tokenB), null);
        BlockingQueue<String> inboxB = subscribe(b.session, B);
        Connection a = connect(null, bearer(tokenA), null);

        a.session.send("/user/" + B + "/queue/notification", "fake".getBytes(StandardCharsets.UTF_8));

        assertThat(a.handler.error.get(WAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo("메시지를 보낼 수 없습니다.");
        assertSessionClosed(a);
        assertThat(inboxB.poll(QUIET_MILLIS, TimeUnit.MILLISECONDS)).isNull();
    }

    // 허용 Origin: 운영 HTTPS 화면은 연결되고, 목록에 없는 Origin은 거부한다.

    @Test
    void productionHttpsOriginCanConnect() throws Exception {
        Connection a = connect(null, bearer(tokenA), "https://nimn.store");
        assertThat(a.session.isConnected()).isTrue();
    }

    @Test
    void unknownOriginIsRejected() {
        CompletableFuture<StompSession> future = connectAsync(null, headers(bearer(tokenA)), "https://evil.example",
                new RecordingHandler());
        assertThat(future).failsWithin(WAIT_SECONDS, TimeUnit.SECONDS);
    }

    // --- helpers ---

    private record Connection(StompSession session, RecordingHandler handler) {
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static StompHeaders headers(String authorization) {
        StompHeaders headers = new StompHeaders();
        if (authorization != null) headers.add("Authorization", authorization);
        return headers;
    }

    private static StompHeaders withEmail(String authorization, String email) {
        StompHeaders headers = headers(authorization);
        headers.add("userEmail", email);
        return headers;
    }

    private static StompHeaders emailOnlyHeaders(String email) {
        return withEmail(null, email);
    }

    private Connection connect(String query, String authorization, String origin) throws Exception {
        return connect(query, headers(authorization), origin);
    }

    private Connection connect(String query, StompHeaders connectHeaders, String origin) throws Exception {
        RecordingHandler handler = new RecordingHandler();
        StompSession session = connectAsync(query, connectHeaders, origin, handler).get(WAIT_SECONDS, TimeUnit.SECONDS);
        openSessions.add(session);
        return new Connection(session, handler);
    }

    private CompletableFuture<StompSession> connectAsync(String query, StompHeaders connectHeaders, String origin,
                                                         RecordingHandler handler) {
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        if (origin != null) handshakeHeaders.setOrigin(origin);
        String url = "http://localhost:" + port + "/ws/notification" + (query == null ? "" : query);
        return stompClient.connectAsync(url, handshakeHeaders, connectHeaders, handler);
    }

    /**
     * 사용자 큐를 구독하고, 브로커가 구독을 등록할 때까지 기다린다.
     * simple broker는 SUBSCRIBE에 RECEIPT를 보내지 않으므로, 구독자 본인 앞으로 확인용 메시지를 보내 도착을 확인한 뒤 비운다.
     */
    private BlockingQueue<String> subscribe(StompSession session, String owner) throws Exception {
        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        // 확인용 메시지는 구독마다 다른 표식을 달고, 몇 개가 늦게 오더라도 inbox에 넣지 않는다.
        String probe = "subscription-probe-" + java.util.UUID.randomUUID();
        CompletableFuture<Void> registered = new CompletableFuture<>();
        session.subscribe(USER_QUEUE, new TextFrameHandler(inbox, probe, registered));
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(WAIT_SECONDS);
        while (!registered.isDone()) {
            if (System.currentTimeMillis() > deadline) fail("subscription was not registered");
            messagingTemplate.convertAndSendToUser(owner, "/queue/notification", probe);
            try {
                registered.get(100, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException notYet) {
                // 아직 구독이 등록되지 않았다. 다시 보낸다.
            }
        }
        return inbox;
    }

    /** 이메일·로그인 헤더를 보내도 서버가 연결을 받아들이지 않아야 한다(STOMP ERROR 후 종료). */
    private void assertConnectRejected(StompHeaders connectHeaders) throws Exception {
        // 예전 프론트처럼 연결 주소에도 이메일을 붙인다.
        String email = connectHeaders.getFirst("userEmail");
        RecordingHandler handler = new RecordingHandler();
        CompletableFuture<StompSession> future = connectAsync(email == null ? null : "?userEmail=" + email,
                connectHeaders, null, handler);
        try {
            StompSession session = future.get(WAIT_SECONDS, TimeUnit.SECONDS);
            openSessions.add(session);
            assertSessionClosed(new Connection(session, handler));
        } catch (java.util.concurrent.ExecutionException closedAfterError) {
            // ERROR 프레임 뒤 연결이 닫히면 연결 시도가 실패로 끝난다. 아래에서 ERROR를 받았는지 확인한다.
        } catch (InterruptedException | java.util.concurrent.TimeoutException e) {
            fail("connection attempt did not finish", e);
        }
        // HTTP 오류·핸드셰이크 실패 같은 전송 장애는 인증 거부로 인정하지 않는다. 서버가 STOMP ERROR로 거부해야 한다.
        assertThat(handler.error.get(WAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo("로그인이 필요합니다.");
        assertThat(sessionManageService.isUserConnected(A)).isFalse();
    }

    private void assertSessionClosed(Connection connection) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(WAIT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            if (!connection.session.isConnected()) return;
            Thread.sleep(50);
        }
        fail("server did not reject the frame; session is still connected");
    }

    private static final class TextFrameHandler implements StompFrameHandler {
        private final BlockingQueue<String> inbox;
        private final String probe;
        private final CompletableFuture<Void> probeSeen;

        private TextFrameHandler(BlockingQueue<String> inbox) {
            this(inbox, null, new CompletableFuture<>());
        }

        private TextFrameHandler(BlockingQueue<String> inbox, String probe, CompletableFuture<Void> probeSeen) {
            this.inbox = inbox;
            this.probe = probe;
            this.probeSeen = probeSeen;
        }

        @NonNull
        @Override
        public Type getPayloadType(@NonNull StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
            String text = payload == null ? "" : new String((byte[]) payload, StandardCharsets.UTF_8);
            if (probe != null && text.contains(probe)) {
                probeSeen.complete(null);
                return;
            }
            inbox.add(text);
        }
    }

    private static final class RecordingHandler extends StompSessionHandlerAdapter {
        /** 서버가 보낸 STOMP ERROR 프레임의 message 헤더. */
        private final CompletableFuture<String> error = new CompletableFuture<>();

        @NonNull
        @Override
        public Type getPayloadType(@NonNull StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(@NonNull StompHeaders headers, @Nullable Object payload) {
            // 세션 핸들러로 오는 프레임은 ERROR뿐이다(구독 메시지는 구독별 핸들러로 간다).
            error.complete(String.valueOf(headers.getFirst("message")));
        }

        @Override
        public void handleException(@NonNull StompSession session, @Nullable StompCommand command,
                                    @NonNull StompHeaders headers, @NonNull byte[] payload, @NonNull Throwable exception) {
            // 테스트에서는 세션 종료 여부로 판단한다.
        }
    }
}
