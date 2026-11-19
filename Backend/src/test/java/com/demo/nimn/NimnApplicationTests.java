package com.demo.nimn;

import org.junit.jupiter.api.Test;
import com.demo.nimn.service.ai.AiService;
import com.demo.nimn.service.food.WeeklyFoodPlanService;
import com.demo.nimn.service.notification.NotificationService;
import com.siot.IamportRestClient.IamportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.config.import=",
        // Test properties take precedence over an inherited datasource environment.
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:nimn-test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=DAY",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class NimnApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private IamportClient iamportClient;

    @MockitoBean
    private AiService aiService;

    // Scheduled jobs are replaced so context startup never runs background work.
    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private WeeklyFoodPlanService weeklyFoodPlanService;

    @Test
    void contextLoadsWithAnIsolatedDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:nimn-test");
        }
    }

    @Test
    void openApiDocumentIsGeneratedWithTheGlobalExceptionHandler() throws Exception {
        var result = mockMvc.perform(get("/api-docs")).andReturn();

        assertThat(result.getResolvedException()).isNull();
        status().isOk().match(result);
        jsonPath("$.openapi", startsWith("3.")).match(result);
        jsonPath("$.paths['/users']").exists().match(result);
    }

}
