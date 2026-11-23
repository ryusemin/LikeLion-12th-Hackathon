package com.demo.nimn.config.security;

import com.demo.nimn.entity.food.Food;
import com.demo.nimn.entity.review.Review;
import com.demo.nimn.entity.review.ReviewSummary;
import com.demo.nimn.repository.food.FoodRepository;
import com.demo.nimn.repository.review.ReviewRepository;
import com.demo.nimn.repository.review.ReviewSummaryRepository;
import com.demo.nimn.service.ai.AiService;
import com.demo.nimn.service.food.WeeklyFoodPlanService;
import com.siot.IamportRestClient.IamportClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그인 없이 호출하는 리뷰 요약 API가 작성자 이메일 전체를 응답하지 않는지 확인한다 (#26).
 */
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:nimn-review-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=DAY",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class ReviewSummaryPrivacyTest {

    private static final String WRITER = "reviewer@example.invalid";

    @Autowired private MockMvc mockMvc;
    @Autowired private FoodRepository foodRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ReviewSummaryRepository reviewSummaryRepository;

    @MockitoBean private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean private JavaMailSender javaMailSender;
    @MockitoBean private IamportClient iamportClient;
    @MockitoBean private AiService aiService;
    @MockitoBean private WeeklyFoodPlanService weeklyFoodPlanService;

    private Long foodId;

    @BeforeEach
    void setUp() {
        Food food = Food.builder().name("food").build();
        food.setReviewSummary(ReviewSummary.builder().food(food).build());
        food = foodRepository.save(food);
        foodId = food.getId();
        reviewRepository.save(Review.builder().userEmail(WRITER).food(food).rating(4.0).comment("good").build());
    }

    @AfterEach
    void cleanUp() {
        reviewSummaryRepository.deleteAll();
        reviewRepository.deleteAll();
        foodRepository.deleteAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/review/summary", "/api/review/summary/sort?order=desc", "/api/review/summary/{foodId}"})
    void anonymousSummaryShowsOnlyAMaskedWriter(String path) throws Exception {
        String body = mockMvc.perform(get(path, foodId))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"userEmail\":\"rev***\"").doesNotContain(WRITER).doesNotContain("example.invalid");
    }
}
