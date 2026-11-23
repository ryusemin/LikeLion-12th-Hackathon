package com.demo.nimn.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "개별 리뷰 정보")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    @Schema(description = "리뷰 ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "리뷰 작성자 이메일. 공개 리뷰 요약에서는 아이디 앞 최대 3글자만 남기고 가립니다(예: use***).", example = "user@example.com", accessMode = Schema.AccessMode.READ_ONLY)
    private String userEmail;

    @Schema(description = "음식 ID", example = "1", required = true)
    private Long foodId;

    @Schema(description = "음식 이름", example = "김치찌개 정식", accessMode = Schema.AccessMode.READ_ONLY)
    private String foodName;

    @Schema(description = "음식 이미지 URL", example = "/api/image/123", accessMode = Schema.AccessMode.READ_ONLY)
    private String foodImage;

    @Schema(description = "평점 (1.0 ~ 5.0)", example = "4.5", minimum = "1.0", maximum = "5.0")
    private Double rating;

    @Schema(description = "리뷰 코멘트", example = "맛있게 잘 먹었습니다!")
    private String comment;

    @Schema(description = "리뷰 작성 시간", example = "2024-12-17T14:30:55.123456", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;
}