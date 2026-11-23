package com.demo.nimn.config.security;

import com.demo.nimn.entity.auth.Users;
import com.demo.nimn.entity.diet.DailyDiet;
import com.demo.nimn.entity.diet.FoodSelection;
import com.demo.nimn.entity.diet.WeeklyDiet;
import com.demo.nimn.entity.notification.Notification;
import com.demo.nimn.entity.order.Order;
import com.demo.nimn.entity.payment.Payment;
import com.demo.nimn.entity.review.DailyDietReview;
import com.demo.nimn.enums.NotificationType;
import com.demo.nimn.filter.JWTUtil;
import com.demo.nimn.repository.auth.UserRepository;
import com.demo.nimn.repository.diet.DailyDietRepository;
import com.demo.nimn.repository.diet.FoodSelectionRepository;
import com.demo.nimn.repository.diet.WeeklyDietRepository;
import com.demo.nimn.repository.notification.NotificationRepository;
import com.demo.nimn.repository.order.OrderRepository;
import com.demo.nimn.repository.payment.PaymentRepository;
import com.demo.nimn.repository.review.DailyDietReviewRepository;
import com.demo.nimn.service.ai.AiService;
import com.demo.nimn.service.food.WeeklyFoodPlanService;
import com.siot.IamportRestClient.IamportClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그인한 회원 A가 회원 B의 이메일이나 데이터 ID를 넣어도 B의 데이터를 조회·변경할 수 없는지 확인한다 (#8).
 * 사용자 식별은 요청 값이 아니라 토큰의 Principal에서 가져오고, 남의 데이터 ID는 없는 데이터와 같은 404로 답한다.
 */
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:nimn-owner-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=DAY",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class ResourceOwnershipTest {

    private static final String A = "member-a@example.invalid";
    private static final String B = "member-b@example.invalid";
    private static final LocalDate DAY = LocalDate.of(2026, 1, 5);

    @Autowired private MockMvc mockMvc;
    @Autowired private JWTUtil jwtUtil;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private DailyDietRepository dailyDietRepository;
    @Autowired private WeeklyDietRepository weeklyDietRepository;
    @Autowired private FoodSelectionRepository foodSelectionRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private DailyDietReviewRepository dailyDietReviewRepository;

    @MockitoBean private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean private JavaMailSender javaMailSender;
    @MockitoBean private IamportClient iamportClient;
    @MockitoBean private AiService aiService;
    @MockitoBean private WeeklyFoodPlanService weeklyFoodPlanService;

    private String tokenA;
    private String bPasswordHash;
    private WeeklyDiet weeklyA;
    private WeeklyDiet weeklyB;

    @BeforeEach
    void setUp() {
        bPasswordHash = passwordEncoder.encode("password-b");
        userRepository.save(Users.builder().name("a").email(A).password(passwordEncoder.encode("password-a"))
                .phoneNumber("010-0000-0001").role("ROLE_USER").build());
        userRepository.save(Users.builder().name("b").email(B).password(bPasswordHash)
                .phoneNumber("010-0000-0002").role("ROLE_USER").build());
        tokenA = jwtUtil.createAccessJwt(A, "ROLE_USER");

        weeklyA = weeklyDietRepository.save(WeeklyDiet.builder().userEmail(A)
                .startDate(DAY).endDate(DAY.plusDays(6)).dailyDiets(List.of()).build());
        weeklyB = weeklyDietRepository.save(WeeklyDiet.builder().userEmail(B)
                .startDate(DAY).endDate(DAY.plusDays(6)).dailyDiets(List.of()).build());
    }

    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        notificationRepository.deleteAll();
        dailyDietReviewRepository.deleteAll();
        weeklyDietRepository.deleteAll();
        dailyDietRepository.deleteAll();
        foodSelectionRepository.deleteAll();
        userRepository.deleteAll();
    }

    // 요청에 담긴 이메일은 무시하고 로그인한 회원 기준으로 처리한다.

    @Test
    void profileUpdateWithAnotherEmailChangesOnlyTheCaller() throws Exception {
        asA(put("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + B + "\",\"phoneNumber\":\"010-9999-9999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(A));

        assertThat(userRepository.findByEmail(B).getPhoneNumber()).isEqualTo("010-0000-0002");
        assertThat(userRepository.findByEmail(A).getPhoneNumber()).isEqualTo("010-9999-9999");
    }

    @Test
    void passwordChangeWithAnotherEmailCannotChangeThatPassword() throws Exception {
        // A가 B의 현재 비밀번호를 알더라도, 확인 대상은 A 자신의 비밀번호다.
        asA(put("/api/users/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + B + "\",\"oldPassword\":\"password-b\",\"newPassword\":\"hijacked\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(userRepository.findByEmail(B).getPassword()).isEqualTo(bPasswordHash);
    }

    @Test
    void dailyDietReadIgnoresTheRequestedEmail() throws Exception {
        dailyDietRepository.save(DailyDiet.builder().userEmail(B).date(DAY).foodSelections(List.of()).build());

        asA(post("/api/diet/daily/read").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userEmail\":\"" + B + "\",\"date\":\"" + DAY + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void weeklyDietIsCreatedForTheCallerAtEveryLevel() throws Exception {
        LocalDate next = DAY.plusWeeks(1);
        asA(post("/api/diet/weekly/create").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userEmail\":\"" + B + "\",\"startDate\":\"" + next + "\",\"endDate\":\"" + next.plusDays(6)
                        + "\",\"dailyDiets\":[{\"userEmail\":\"" + B + "\",\"date\":\"" + next + "\",\"foodSelections\":[]}]}"))
                .andExpect(status().isCreated());

        assertThat(dailyDietRepository.findByUserEmailAndDate(B, next)).isEmpty();
        assertThat(dailyDietRepository.findByUserEmailAndDate(A, next)).hasSize(1);
        assertThat(weeklyDietRepository.findAll()).filteredOn(w -> w.getStartDate().equals(next))
                .extracting(WeeklyDiet::getUserEmail).containsExactly(A);
    }

    @Test
    void weeklyDietReadReturnsTheCallersDiet() throws Exception {
        asA(get("/api/diet/weekly/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value(A));
    }

    @Test
    void notificationListAndCountIgnoreTheRequestedEmail() throws Exception {
        notification(A);
        notification(B);
        notification(B);

        asA(get("/api/notification/all").param("userEmail", B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        asA(get("/api/notification").param("userEmail", B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void markAllAsReadWithAnotherEmailLeavesThatUsersNotificationsUnread() throws Exception {
        Notification ofB = notification(B);

        asA(patch("/api/notification").param("userEmail", B)).andExpect(status().isOk());

        assertThat(notificationRepository.findById(ofB.getId()).orElseThrow().getCheck()).isFalse();
    }

    @Test
    void paymentListIgnoresTheRequestedPurchaser() throws Exception {
        payment(B);

        asA(get("/api/payment").param("purchaser", B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void dailyReviewReadIgnoresTheRequestedEmail() throws Exception {
        dailyDietReviewRepository.save(DailyDietReview.builder().userEmail(B).reviewDate(DAY).reviews(List.of()).build());
        DailyDietReview ofA = dailyDietReviewRepository.save(
                DailyDietReview.builder().userEmail(A).reviewDate(DAY).reviews(List.of()).build());

        asA(get("/api/review/daily").param("userEmail", B).param("date", DAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ofA.getId()))
                .andExpect(jsonPath("$.userEmail").value(A));
    }

    @Test
    void orderIsPlacedForTheCaller() throws Exception {
        asA(post("/api/order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"purchaser\":\"" + B + "\",\"weeklyDietId\":" + weeklyA.getId() + "}"))
                .andExpect(status().isCreated());

        assertThat(orderRepository.findAll()).extracting(Order::getPurchaser).containsExactly(A);
    }

    // 남의 데이터 ID는 없는 데이터와 같은 404로 답하고 바꾸지 않는다.

    @Test
    void anotherUsersFoodSelectionIsNotFound() throws Exception {
        FoodSelection ofB = foodSelectionRepository.save(FoodSelection.builder().userEmail(B).mealTime("아침").count(1).build());

        expectNotFound(asA(get("/api/diet/selections/read/" + ofB.getId())));
    }

    @Test
    void markingAnotherUsersNotificationIsNotFoundAndKeepsItUnread() throws Exception {
        Notification ofB = notification(B);

        expectNotFound(asA(patch("/api/notification/" + ofB.getId())));
        assertThat(notificationRepository.findById(ofB.getId()).orElseThrow().getCheck()).isFalse();
    }

    @Test
    void callerCanMarkOwnNotification() throws Exception {
        Notification ofA = notification(A);

        asA(patch("/api/notification/" + ofA.getId())).andExpect(status().isOk());
        assertThat(notificationRepository.findById(ofA.getId()).orElseThrow().getCheck()).isTrue();
    }

    @Test
    void anotherUsersPaymentIsNotFoundLikeAMissingOne() throws Exception {
        payment(B);

        expectNotFound(asA(get("/api/payment/PAY-B")));
        expectNotFound(asA(get("/api/payment/PAY-MISSING")));
    }

    @Test
    void updatingAnotherUsersDailyReviewIsNotFound() throws Exception {
        DailyDietReview ofB = dailyDietReviewRepository.save(
                DailyDietReview.builder().userEmail(B).reviewDate(DAY).reviews(List.of()).build());

        expectNotFound(asA(put("/api/review/daily/" + ofB.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reviews\":[]}")));
    }

    @Test
    void orderingAnotherUsersWeeklyDietIsNotFoundAndCreatesNoOrder() throws Exception {
        expectNotFound(asA(post("/api/order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"weeklyDietId\":" + weeklyB.getId() + "}")));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void payingAnotherUsersOrderIsNotFoundBeforeCallingThePaymentGateway() throws Exception {
        Order ofB = orderRepository.save(Order.builder().purchaser(B).totalPrice(1000L).weeklyDietId(weeklyB.getId()).build());

        expectNotFound(asA(post("/api/payment").contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentUid\":\"imp_test\",\"orderId\":\"" + ofB.getId() + "\"}")));

        verifyNoInteractions(iamportClient);
        assertThat(paymentRepository.count()).isZero();
    }

    private ResultActions asA(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + tokenA));
    }

    private void expectNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private Notification notification(String owner) {
        return notificationRepository.save(Notification.builder().userEmail(owner).content("c")
                .type(NotificationType.DIET).sendTime(java.time.LocalDateTime.now()).check(false).build());
    }

    private void payment(String owner) {
        paymentRepository.save(Payment.builder().id(owner.equals(B) ? "PAY-B" : "PAY-A").uid("uid-" + owner)
                .purchaser(owner).totalPrice(1000L).weeklyDietId(weeklyB.getId()).build());
    }
}
