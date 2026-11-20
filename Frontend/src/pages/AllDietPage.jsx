import { apiUrl } from '../config/endpoints';
import { apiFetch } from '../api/client';
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { LeftOutlined, RightOutlined, StarFilled } from '@ant-design/icons';
import '../CSS/AllDietPage.css';
import logo from'../images/logo.png';

function AllDietPage() {
    const navigate = useNavigate();
    const [currentPage, setCurrentPage] = useState(0);
    const [mealData, setMealData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedFoodReviews, setSelectedFoodReviews] = useState(null);
    const [reviewModalOpen, setReviewModalOpen] = useState(false);
    const [detailedReviews, setDetailedReviews] = useState([]);

    const handleItemClick = (item) => {
        navigate(`/dietinfo`, { state: { item } });
    };

    // 음식 목록 조회 API
    const getAllFoodsAPI = async () => {
        try {
            const response = await apiFetch('/api/foods/plans', {
                method: "GET",
                credentials: 'include',
            });
      
            if (response.ok) {
                const data = await response.json();
                return data;
            } else {
                return [];
            }
        } catch (error) {
            return [];
        }
    };

    // 전체 리뷰 조회 API (summary가 전체 조회)
    const getAllReviewsAPI = async () => {
        try {
            const response = await apiFetch('/api/review/summary', {
                method: "GET",
                credentials: 'include',
            });
      
            if (response.ok) {
                const data = await response.json();

                return data;
            } else {
                return [];
            }
        } catch (error) {
            return [];
        }
    };

    // 이미지 URL 생성 함수
    const getImageUrl = (imageId) => {
        if (!imageId) return logo;
        return apiUrl(imageId);
    };

    // 영양 정보 포맷팅 함수
    const formatNutrition = (food) => {
        const calories = food.calories ? `${food.calories}` : '';
        const protein = food.protein ? ` ${food.protein}` : '';
        const carbs = food.carbohydrate ? ` ${food.carbohydrate}` : '';
        const fat = food.fat ? ` ${food.fat}` : '';
        
        return { calories, protein, carbs, fat };
    };

    // 문자열을 숫자로 안전하게 변환하는 함수
    const parseNutritionValue = (value) => {
        if (!value) return 0;
        const parsed = parseFloat(value);
        return isNaN(parsed) ? 0 : parsed;
    };

    // 가격 포맷팅 함수
    const formatPrice = (price) => {
        if (!price) return '가격 정보 없음';
        return `${parseInt(price).toLocaleString()}원`;
    };

    // 카테고리 매핑 함수
    const getCategoryName = (main1, main2) => {
        if (main1) return main1;
        if (main2) return main2;
        return '기타';
    };

    // 설명 생성 함수
    const generateDescription = (food) => {
        const parts = [];
        if (food.main1) parts.push(food.main1);
        if (food.main2) parts.push(food.main2);
        if (food.side1) parts.push(food.side1);
        if (food.side2) parts.push(food.side2);
        if (food.side3) parts.push(food.side3);
        
        return parts.join(', ') || '상세 정보 없음';
    };

    // summary API 응답 구조에 맞는 리뷰 데이터 매핑 함수
    const mapReviewData = (reviewSummaries) => {
        const reviewMap = {};

        
        if (reviewSummaries && reviewSummaries.length > 0) {
            reviewSummaries.forEach(summary => {
                const foodId = summary.foodId;
                if (foodId) {
                    reviewMap[foodId] = {
                        averageRating: summary.averageRating || 0,
                        totalReviews: summary.totalReviews || 0,
                        foodName: summary.foodName || '',
                        foodImage: summary.foodImage || '',
                        detailedReviews: summary.reviews || [] // summary API의 reviews 배열 사용
                    };
                }
            });
        }
 
        return reviewMap;
    };

    // 별점 렌더링 함수
    const renderStars = (rating) => {
        const stars = [];
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 !== 0;

        for (let i = 0; i < fullStars; i++) {
            stars.push(
                <StarFilled key={i} className="allDietPage_star_filled" />
            );
        }

        if (hasHalfStar) {
            stars.push(
                <div key="half" className="allDietPage_star_half">
                    <StarFilled className="allDietPage_star_filled" />
                </div>
            );
        }

        const emptyStars = 5 - Math.ceil(rating);
        for (let i = 0; i < emptyStars; i++) {
            stars.push(
                <StarFilled key={`empty-${i}`} className="allDietPage_star_empty" />
            );
        }

        return stars;
    };

    // 리뷰 모달 열기
    const openReviewModal = (e, foodItem) => {
        e.stopPropagation();
        
        setSelectedFoodReviews(foodItem);
        setDetailedReviews(foodItem.reviewInfo.detailedReviews || []);
        setReviewModalOpen(true);
        document.body.classList.add('modal-open');
    };

    // 리뷰 모달 닫기
    const closeReviewModal = () => {
        setReviewModalOpen(false);
        setSelectedFoodReviews(null);
        setDetailedReviews([]);
        document.body.classList.remove('modal-open');
    };

    // 날짜 포맷팅 함수
    const formatDate = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    };

    // 슬라이드 기능을 위한 설정
    const itemsPerPage = 4; 
    const totalPages = Math.ceil(mealData.length / itemsPerPage);
    const showSlideControls = mealData.length > 4;

    const getCurrentPageItems = () => {
        if (!showSlideControls) return mealData;
        
        const startIndex = currentPage * itemsPerPage;
        const endIndex = startIndex + itemsPerPage;
        return mealData.slice(startIndex, endIndex);
    };

    const nextPage = () => {
        if (currentPage < totalPages - 1) {
            setCurrentPage(currentPage + 1);
        }
    };

    const prevPage = () => {
        if (currentPage > 0) {
            setCurrentPage(currentPage - 1);
        }
    };

    const goToPage = (pageIndex) => {
        setCurrentPage(pageIndex);
    };

    useEffect(() => {
        const handleGet = async () => {
            try {
                setLoading(true);
                
                // 음식 목록과 리뷰 요약을 동시에 조회
                const [foodResult, reviewResult] = await Promise.all([
                    getAllFoodsAPI(),
                    getAllReviewsAPI()
                ]);
                
                // 리뷰 데이터를 foodId 기준으로 매핑
                const mappedReviews = mapReviewData(reviewResult);
                
                if (foodResult && foodResult.length > 0) {
                    const formattedData = foodResult.map(food => {
                        const nutrition = formatNutrition(food);
                        const reviewInfo = mappedReviews[food.id] || {
                            averageRating: 0,
                            totalReviews: 0,
                            detailedReviews: []
                        };
                        
                        
                        return {
                            id: food.id,
                            name: food.name || '메뉴명 없음',
                            price: formatPrice(food.price),
                            image: getImageUrl(food.image),
                            category: getCategoryName(food.main1, food.main2),
                            description: generateDescription(food),
                            
                            // 리뷰 정보 추가
                            reviewInfo: reviewInfo,
                            
                            // 표시용 영양정보
                            calories: nutrition.calories,
                            protein: nutrition.protein,
                            fat: nutrition.fat,
                            carbs: nutrition.carbs,
                            
                            // 모든 영양성분 정보
                            nutritionInfo: {
                                caloriesString: food.calories || '0',
                                proteinString: food.protein || '0',
                                carbohydrateString: food.carbohydrate || '0',
                                fatString: food.fat || '0',
                                sugarString: food.sugar || '0',
                                sodiumString: food.sodium || '0',
                                
                                calories: parseNutritionValue(food.calories),
                                protein: parseNutritionValue(food.protein),
                                carbohydrate: parseNutritionValue(food.carbohydrate),
                                fat: parseNutritionValue(food.fat),
                                sugar: parseNutritionValue(food.sugar),
                                sodium: parseNutritionValue(food.sodium),
                                
                                formattedCalories: nutrition.calories,
                                formattedProtein: nutrition.protein,
                                formattedCarbs: nutrition.carbs,
                                formattedFat: nutrition.fat,
                                formattedSugar: food.sugar ? ` ${food.sugar}` : '',
                                formattedSodium: food.sodium ? ` ${food.sodium}` : ''
                            },
                            
                            // 메뉴 구성 정보
                            menuComponents: {
                                main1: food.main1 || '',
                                main2: food.main2 || '',
                                side1: food.side1 || '',
                                side2: food.side2 || '',
                                side3: food.side3 || ''
                            },
                            
                            // 원본 데이터
                            originalData: food
                        };
                    });
                    
                    setMealData(formattedData);
                } else {
                    setMealData([]);
                }

            } catch (error) {
                alert("데이터를 가져오는 중 오류가 발생했습니다.");
                setMealData([]);
            } finally {
                setLoading(false);
            }
        };
        
        handleGet();
    }, []);

    if (loading) {
        return (
            <div className="allDietPage_loading_container">
                <div className="allDietPage_loading_spinner"></div>
                <p className="allDietPage_loading_spinner_text">영양 데이터를 불러오는 중...</p>
            </div>
        );
    }

    return (
        <div className="allDietPage_main_wrapper">
            <div className="allDietPage_container">
                <div className="allDietPage_meals_grid">
                    {getCurrentPageItems().map((item) => (
                        <div key={item.id} className="allDietPage_meal_wrapper">
                            <div className="allDietPage_meal_card" onClick={() => handleItemClick(item)}>
                                <div className="allDietPage_meal_image">
                                    <img 
                                        src={item.image} 
                                        alt={item.name} 
                                        className="allDietPage_meal_image_content"
                                        onError={(e) => {
                                            e.target.src = logo;
                                        }}
                                    />
                                </div>
                                <div className="allDietPage_meal_info">
                                    <h3 className="allDietPage_meal_info_name">{item.name}</h3>
                                    <p className="allDietPage_meal_info_description">{item.description}</p>
                                    
                                    <div className="allDietPage_nutrition_info">
                                        <div className="allDietPage_nutrition_row">
                                            {item.calories && <span className="allDietPage_nutrition_info1">{item.calories}</span>}
                                            {item.protein && <span className="allDietPage_nutrition_info2">단백질 {item.protein}</span>}
                                        </div>
                                        <div className="allDietPage_nutrition_row">
                                            {item.fat && <span className="allDietPage_nutrition_info3">지방 {item.fat}</span>}
                                            {item.carbs && <span className="allDietPage_nutrition_info4">탄수화물 {item.carbs}</span>}
                                        </div>
                                    </div>
                                    
                                    <div className="allDietPage_price_info">
                                        <span className="allDietPage_price">{item.price}</span>
                                    </div>
                                </div>
                            </div>
                            
                            {/* 리뷰 정보 - 카드 외부 하단에 위치 */}
                            {item.reviewInfo ? (
                                item.reviewInfo.totalReviews > 0 ? (
                                    <div 
                                        className="allDietPage_review_info_external clickable" 
                                        onClick={(e) => openReviewModal(e, item)}
                                    >
                                        <div className="allDietPage_review_left">
                                            <div className="allDietPage_stars_container">
                                                {renderStars(item.reviewInfo.averageRating)}
                                            </div>
                                            <div className="allDietPage_review_summary">
                                                <span className="allDietPage_rating_number">
                                                    {item.reviewInfo.averageRating.toFixed(1)}
                                                </span>
                                                <span className="allDietPage_review_count">
                                                    ({item.reviewInfo.totalReviews}개 리뷰)
                                                </span>
                                            </div>
                                        </div>
                                        <div className="allDietPage_review_view_text">
                                            <span className="allDietPage_review_view_icon"></span>
                                            <span className="allDietPage_review_view_label">리뷰 보기</span>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="allDietPage_no_review_external">
                                        <div className="allDietPage_no_review_icon"></div>
                                        <div className="allDietPage_no_review_content">
                                            <span className="allDietPage_no_review_title">아직 리뷰가 없어요</span>
                                        </div>
                                    </div>
                                )
                            ) : null}
                        </div>
                    ))}
                </div>

                {showSlideControls && (
                    <div className="allDietPage_slide_controls">
                        <button 
                            className="allDietPage_nav_button allDietPage_prev_button"
                            onClick={prevPage}
                            disabled={currentPage === 0}
                        >
                            <LeftOutlined className="allDietPage_nav_icon" />
                        </button>

                        <div className="allDietPage_indicators">
                            {Array.from({ length: totalPages }, (_, index) => (
                                <div
                                    key={index}
                                    className={`allDietPage_indicator ${index === currentPage ? 'active' : ''}`}
                                    onClick={() => goToPage(index)}
                                />
                            ))}
                        </div>

                        <button 
                            className="allDietPage_nav_button allDietPage_next_button"
                            onClick={nextPage}
                            disabled={currentPage === totalPages - 1}
                        >
                            <RightOutlined className="allDietPage_nav_icon" />
                        </button>
                    </div>
                )}

                {mealData.length === 0 && !loading && (
                    <div className="allDietPage_no_data">
                        <p className="allDietPage_no_data_text">등록된 음식이 없습니다.</p>
                    </div>
                )}
            </div>

            {/* 리뷰 모달 */}
            {reviewModalOpen && selectedFoodReviews && (
                <div className="allDietPage_review_modal_overlay" onClick={closeReviewModal}>
                    <div className="allDietPage_review_modal_container" onClick={(e) => e.stopPropagation()}>
                        <div className="allDietPage_review_modal_header">
                            <div className="allDietPage_review_modal_header_content">
                                <h2 className="allDietPage_review_modal_title">{selectedFoodReviews.name} 리뷰</h2>
                                <div className="allDietPage_review_modal_summary">
                                    <div className="allDietPage_review_modal_stars">
                                        {renderStars(selectedFoodReviews.reviewInfo.averageRating)}
                                    </div>
                                    <span className="allDietPage_review_modal_rating">
                                        {selectedFoodReviews.reviewInfo.averageRating.toFixed(1)}
                                    </span>
                                    <span className="allDietPage_review_modal_count">
                                        ({selectedFoodReviews.reviewInfo.totalReviews}개 리뷰)
                                    </span>
                                </div>
                            </div>
                            <button className="allDietPage_review_modal_close" onClick={closeReviewModal}>
                                <span className="allDietPage_modal_close_icon">✕</span>
                            </button>
                        </div>
                        
                        <div className="allDietPage_review_modal_content">
                            {detailedReviews.length > 0 ? (
                                <div className="allDietPage_reviews_list">
                                    {detailedReviews.map((review, index) => (
                                        <div key={review.id || index} className="allDietPage_review_item">
                                            <div className="allDietPage_review_item_header">
                                                <div className="allDietPage_review_item_stars">
                                                    {renderStars(review.rating)}
                                                </div>
                                                <span className="allDietPage_review_item_rating">
                                                    {review.rating}.0
                                                </span>
                                                <span className="allDietPage_review_item_date">
                                                    {formatDate(review.createdAt)}
                                                </span>
                                            </div>
                                            <div className="allDietPage_review_item_user">
                                                <span className="allDietPage_review_user_label">작성자: </span>
                                                <span className="allDietPage_review_user_name">
                                                    {review.userEmail ? review.userEmail.substring(0, 3) + '***' : '익명'}
                                                </span>
                                            </div>
                                            {review.comment && (
                                                <div className="allDietPage_review_item_comment">
                                                    <p className="allDietPage_review_comment_text">{review.comment}</p>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="allDietPage_no_reviews">
                                    <p className="allDietPage_no_reviews_main">아직 리뷰가 없습니다.</p>
                                    <p className="allDietPage_no_reviews_sub">첫 번째 리뷰를 작성해보세요!</p>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default AllDietPage;