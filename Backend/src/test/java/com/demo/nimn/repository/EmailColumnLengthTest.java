package com.demo.nimn.repository;

import com.demo.nimn.entity.auth.Users;
import com.demo.nimn.entity.diet.DailyDiet;
import com.demo.nimn.repository.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Columns that hold a user's email must accept the longest deliverable address (254 chars, #19). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // keep the MySQL-mode H2 from the test profile
@ActiveProfiles("test")
class EmailColumnLengthTest {

    private static final String LONGEST_EMAIL = "a".repeat(254 - "@t.invalid".length()) + "@t.invalid";

    @Autowired private UserRepository userRepository;
    @Autowired private TestEntityManager em;

    @Test
    void userAndDailyDietStoreA254CharacterEmail() {
        assertThat(LONGEST_EMAIL).hasSize(254);

        userRepository.saveAndFlush(Users.builder().name("long").email(LONGEST_EMAIL)
                .password("hashed").role("ROLE_USER").build());
        em.persistAndFlush(DailyDiet.builder().date(LocalDate.of(2026, 11, 20)).userEmail(LONGEST_EMAIL).build());
        em.clear();

        assertThat(userRepository.existsByEmail(LONGEST_EMAIL)).isTrue();
    }
}
