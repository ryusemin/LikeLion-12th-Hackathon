package com.demo.nimn.controller.review;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.demo.nimn.dto.auth.CustomUserDetails;
import com.demo.nimn.dto.review.DailyDietReviewDTO;
import com.demo.nimn.dto.review.DailyDietReviewUpdateDTO;
import com.demo.nimn.dto.review.ReviewDTO;
import com.demo.nimn.dto.review.ReviewSummaryDTO;
import com.demo.nimn.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "리뷰 API", description = "식단 리뷰 및 평가 관리")
@RestController
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // ReviewSummary 관련 API
    @Operation(summary = "음식별 종합 리뷰 조회", description = "특정 음식의 종합 리뷰 정보를 조회합니다. 평균 별점, 총 리뷰 수, 완료된 모든 리뷰 목록을 포함합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "종합 리뷰 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewSummaryDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "종합 리뷰를 찾을 수 없거나 서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping("/summary/{foodId}")
    public ResponseEntity<ReviewSummaryDTO> getReviewSummary(@Parameter(description = "리뷰 조회할 음식의 ID", required = true, example = "1")
                                                             @PathVariable("foodId") Long foodMenuId) {
        ReviewSummaryDTO reviewSummary = reviewService.getReviewSummaryByFoodId(foodMenuId);
        return ResponseEntity.status(201).body(reviewSummary);
    }

    @Operation(summary = "전체 종합 리뷰 조회", description = "모든 음식의 종합 리뷰 정보를 조회합니다.\n\n각 음식별 평균 별점과 리뷰 수를 확인할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "전체 종합 리뷰 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = ReviewSummaryDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping("/summary")
    public ResponseEntity<List<ReviewSummaryDTO>> getAllReviewSummaries() {
        List<ReviewSummaryDTO> reviewSummaries = reviewService.getAllReviewSummaries();
        return ResponseEntity.ok(reviewSummaries);
    }

    @Operation(summary = "정렬된 종합 리뷰 조회", description = "별점 기준으로 정렬된 종합 리뷰를 조회합니다.\n\n인기 메뉴나 개선이 필요한 메뉴를 파악할 때 유용합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "별점순 종합 리뷰 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = ReviewSummaryDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping("/summary/sort")
    public ResponseEntity<List<ReviewSummaryDTO>> getReviewSummariesSorted(@Parameter(description = "정렬 순서", schema = @Schema(allowableValues = {"desc", "asc"}, defaultValue = "desc"), example = "desc")
                                                                           @RequestParam(value = "order", defaultValue = "desc") String sortOrder) {
        List<ReviewSummaryDTO> reviewSummaries = reviewService.getReviewSummariesOrderByRating(sortOrder);
        return ResponseEntity.ok(reviewSummaries);
    }

    // DailyDietReview 관련 API
    @Operation(summary = "사용자 특정 날짜 식단 리뷰 조회", description = "특정 사용자의 특정 날짜 식단 리뷰를 조회합니다.\n\n해당 날짜에 먹은 모든 음식에 대한 리뷰 정보를 확인할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "하루 식단 리뷰 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DailyDietReviewDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "하루 식단 리뷰를 찾을 수 없거나 서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping("/daily")
    public ResponseEntity<DailyDietReviewDTO> getDailyDietReviewByUserAndDate(@AuthenticationPrincipal CustomUserDetails principal,
                                                                              @Parameter(description = "조회할 날짜", required = true, example = "2024-12-17") @RequestParam("date") LocalDate reviewDate) {
        DailyDietReviewDTO dailyReview = reviewService.getDailyDietReviewByUserAndDate(principal.getUsername(), reviewDate);
        return ResponseEntity.ok(dailyReview);
    }

    @Operation(summary = "하루 식단 리뷰 수정", description = "하루 식단의 개별 리뷰들을 수정합니다.\n\n평점과 코멘트를 업데이트하면 음식별 종합 리뷰도 자동으로 갱신됩니다.\n\n평점은 필수로 작성해야 하고 코멘트는 작성하지 않아도 됩니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "하루 식단 리뷰 수정 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DailyDietReviewDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "하루 식단 리뷰를 찾을 수 없거나 수정 실패",
                    content = @Content
            )
    })
    @PutMapping("/daily/{dailyReviewId}")
    public ResponseEntity<DailyDietReviewDTO> updateDailyDietReview(@AuthenticationPrincipal CustomUserDetails principal,
                                                                    @Parameter(description = "수정할 하루 식단 리뷰 ID", required = true, example = "1") @PathVariable("dailyReviewId") Long dailyReviewId,
                                                                    @Parameter(description = "수정할 리뷰 목록", required = true) @RequestBody DailyDietReviewUpdateDTO request) {
        DailyDietReviewDTO updatedReview = reviewService.updateDailyDietReview(dailyReviewId, principal.getUsername(), request.getReviews());
        return ResponseEntity.ok(updatedReview);
    }
}