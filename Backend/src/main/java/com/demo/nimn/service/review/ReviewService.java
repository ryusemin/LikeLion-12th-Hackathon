package com.demo.nimn.service.review;

import com.demo.nimn.dto.diet.WeeklyDietDTO;
import com.demo.nimn.dto.review.DailyDietReviewDTO;
import com.demo.nimn.dto.review.ReviewDTO;
import com.demo.nimn.dto.review.ReviewSummaryDTO;

import java.time.LocalDate;
import java.util.List;

public interface ReviewService {

    // ReviewSummary 관련
    ReviewSummaryDTO getReviewSummaryByFoodId(Long foodId);
    List<ReviewSummaryDTO> getAllReviewSummaries();
    List<ReviewSummaryDTO> getReviewSummariesOrderByRating(String sortOrder); // "desc" or "asc"

    // DailyDietReview 관련
    void createWeeklyDietReviews(WeeklyDietDTO weeklyDietDTO);
    DailyDietReviewDTO getDailyDietReviewByUserAndDate(String userEmail, LocalDate reviewDate);
    List<DailyDietReviewDTO> getDailyDietReviewsByDate(LocalDate reviewDate);
    DailyDietReviewDTO updateDailyDietReview(Long id, String ownerEmail, List<ReviewDTO> reviews);
}