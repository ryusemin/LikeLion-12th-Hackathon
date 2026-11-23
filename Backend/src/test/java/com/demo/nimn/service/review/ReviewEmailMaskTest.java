package com.demo.nimn.service.review;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewEmailMaskTest {

    @Test
    void keepsAtMostThreeLettersOfTheIdAndDropsTheDomain() {
        assertThat(ReviewServiceImpl.maskEmail("reviewer@example.com")).isEqualTo("rev***");
        assertThat(ReviewServiceImpl.maskEmail("abc@example.com")).isEqualTo("ab***");
        assertThat(ReviewServiceImpl.maskEmail("a@example.com")).isEqualTo("***");
        assertThat(ReviewServiceImpl.maskEmail(null)).isNull();
    }
}
