import { apiFetch } from '../api/client';
import React, { useState, useEffect } from 'react';
import '../CSS/WeeklyFoodMenu.css';
import { Swiper, SwiperSlide } from 'swiper/react';
import 'swiper/css';
import 'swiper/css/navigation';
import 'swiper/css/pagination';
import { Navigation, Pagination } from 'swiper/modules';
import logoImg from '../images/logo.png'; 

const WeeklyFoodMenu = () => {
  const [mealData, setMealData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  // 모달 상태 관리
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedMealDetail, setSelectedMealDetail] = useState(null);

  // 모달 열기 함수
  const openMealDetailModal = (mealDetail) => {
    setSelectedMealDetail(mealDetail);
    setIsModalOpen(true);
  };

  // 모달 닫기 함수
  const closeMealDetailModal = () => {
    setIsModalOpen(false);
    setSelectedMealDetail(null);
  };

  useEffect(() => {
    const fetchUserDiet = async () => {
      setIsLoading(true);
      setError(null);

      try {
        // 로그인한 유저의 일주일 식단 조회. 서버가 토큰으로 사용자를 정한다 (#8).
        const dietResponse = await apiFetch('/api/diet/weekly/read', {
          method: 'GET',
          credentials: 'include'
        });

        if (!dietResponse.ok) {
          throw new Error('식단 데이터를 가져올 수 없습니다.');
        }

        const dietData = await dietResponse.json();
        const weeklyData = dietData.dailyDiets || dietData;
        
        // 데이터가 없거나 빈 배열인 경우 처리
        if (!weeklyData || weeklyData.length === 0) {
          throw new Error('이번 주 식단이 없습니다.');
        }
        
        setMealData(weeklyData);

      } catch (error) {
        setError(error.message);
        setMealData(null); 
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserDiet();
  }, []);

  const getDateRange = () => {
    if (mealData && mealData.length > 0) {
      const startDate = new Date(mealData[0].date).toLocaleDateString('ko-KR', {
        month: 'long',
        day: 'numeric'
      });
      const endDate = new Date(mealData[mealData.length - 1].date).toLocaleDateString('ko-KR', {
        month: 'long',
        day: 'numeric'
      });
      return `${startDate} ~ ${endDate}`;
    }
    return '';
  };

  // 로딩 상태 처리
  if (isLoading) {
    return (
      <div className="weekly-food-menu-loading-container">
        <div className="weekly-food-menu-loading-spinner"></div>
        <p>식단 데이터를 불러오는 중...</p>
      </div>
    );
  }

  // 에러 상태 처리
  if (error || !mealData || mealData.length === 0) {
    return (
      <div className="weekly-food-menu-empty-container">
        <div className="weekly-food-menu-empty-content">
          <img className="weekly-food-menu-logo-img"
                                          src={logoImg}></img>
          <h2 className="weekly-food-menu-empty-title">
            {error === '이번 주 식단이 없습니다.' ? '아직 이번 주 식단이 없습니다' : '식단을 불러올 수 없습니다'}
          </h2>
          <p className="weekly-food-menu-empty-message">
            {error === '이번 주 식단이 없습니다.' 
              ? '식단을 주문하시면 여기에서 확인하실 수 있습니다.' 
              : '잠시 후 다시 시도해주세요.'}
          </p>
          <button 
            className="weekly-food-menu-empty-btn"
            onClick={() => window.location.reload()}
          >
            새로고침
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="weekly-food-menu-container">
      {/* 헤더 섹션 */}
      <div className="weekly-food-menu-header">
        <h1 className="weekly-food-menu-title">내 주간 식단</h1>
        <p className="weekly-food-menu-period">{getDateRange()} 식단표</p>
      </div>

      {/* 주간 식단 상세 영역 */}
      <div className="weekly-food-menu-content">
        <h2 className="weekly-food-menu-section-title">주간 식단 상세</h2>
        
        <div className="weekly-food-menu-swiper-container">
          <Swiper
            spaceBetween={50}
            slidesPerView={1}
            navigation
            pagination={{ clickable: true }}
            modules={[Navigation, Pagination]}
            className="weekly-food-menu-slide"
          >
            {mealData.map((day, dayIndex) => (
              <SwiperSlide key={dayIndex} className="weekly-food-menu-slide-content">
                {/* 날짜 헤더 */}
                <div className="weekly-food-menu-date-header">
                  <h3 className="weekly-food-menu-date-title">
                    {new Date(day.date).toLocaleDateString('ko-KR', {
                      month: 'long',
                      day: 'numeric',
                      weekday: 'long'
                    })}
                  </h3>
                </div>

                {/* 식단 카드들 */}
                <div className="weekly-food-menu-meals-grid">
                  {day.foodSelections && day.foodSelections.map((meal, mealIndex) => (
                    <div key={mealIndex} className="weekly-food-menu-meal-card">
                      <div className="weekly-food-menu-meal-header">
                        <div className="weekly-food-menu-meal-type">
                          <span className="weekly-food-menu-meal-type-text">
                            {meal.foodTime}
                          </span>
                        </div>
                        <div className="weekly-food-menu-meal-quantity">
                          {meal.count}개
                        </div>
                      </div>

                      <div className="weekly-food-menu-meal-image-container">
                        <img 
                          src={meal.foodMenu.image} 
                          alt={meal.foodMenu.name}
                          className="weekly-food-menu-meal-image"
                          onError={(e) => {
                            e.target.src = logoImg;
                          }}
                        />
                      </div>

                      <div className="weekly-food-menu-meal-info">
                        <h4 className="weekly-food-menu-meal-name">{meal.foodMenu.name}</h4>
                        <div className="weekly-food-menu-meal-description">
                          {[meal.foodMenu.main1, meal.foodMenu.main2, meal.foodMenu.side1].filter(Boolean).join(', ')}
                        </div>
                        <div className="weekly-food-menu-nutrition-info">
                          <div className="weekly-food-menu-nutrition-item">
                            <span className="weekly-food-menu-nutrition-label">칼로리</span>
                            <span className="weekly-food-menu-nutrition-value">{meal.foodMenu.calories}</span>
                          </div>
                          <div className="weekly-food-menu-nutrition-item">
                            <span className="weekly-food-menu-nutrition-label">탄수화물</span>
                            <span className="weekly-food-menu-nutrition-value">{meal.foodMenu.carbohydrate}</span>
                          </div>
                          <div className="weekly-food-menu-nutrition-item">
                            <span className="weekly-food-menu-nutrition-label">단백질</span>
                            <span className="weekly-food-menu-nutrition-value">{meal.foodMenu.protein}</span>
                          </div>
                        </div>
                        
                        {/* 상세 정보 버튼 */}
                        <div className="weekly-food-menu-detail-btn-section">
                          <button 
                            className="weekly-food-menu-detail-btn"
                            onClick={() => openMealDetailModal(meal.foodMenu)}
                          >
                            상세 정보 보기
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </SwiperSlide>
            ))}
          </Swiper>
        </div>
      </div>

      {/* 식단 상세 정보 모달 */}
      {isModalOpen && selectedMealDetail && (
        <div className="weekly-food-menu-modal-overlay" onClick={closeMealDetailModal}>
          <div className="weekly-food-menu-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="weekly-food-menu-modal-header">
              <h2 className="weekly-food-menu-modal-title">{selectedMealDetail.name}</h2>
              <button 
                className="weekly-food-menu-modal-close-btn"
                onClick={closeMealDetailModal}
              >
                ✕
              </button>
            </div>
            
            <div className="weekly-food-menu-modal-body">
              <div className="weekly-food-menu-modal-image-section">
                <img 
                  src={selectedMealDetail.image}
                  alt={selectedMealDetail.name}
                  className="weekly-food-menu-modal-image"
                  onError={(e) => {
                    e.target.src = logoImg;
                  }}
                />
                <div className="weekly-food-menu-modal-price">
                  {selectedMealDetail.price}원
                </div>
              </div>
              
              <div className="weekly-food-menu-modal-info-section">
                <div className="weekly-food-menu-modal-section">
                  <h3 className="weekly-food-menu-modal-section-title">구성 메뉴</h3>
                  <div className="weekly-food-menu-modal-menu-grid">
                    <div className="weekly-food-menu-modal-menu-item">
                      <span className="weekly-food-menu-modal-menu-label">메인 1</span>
                      <span className="weekly-food-menu-modal-menu-value">{selectedMealDetail.main1}</span>
                    </div>
                    <div className="weekly-food-menu-modal-menu-item">
                      <span className="weekly-food-menu-modal-menu-label">메인 2</span>
                      <span className="weekly-food-menu-modal-menu-value">{selectedMealDetail.main2}</span>
                    </div>
                    <div className="weekly-food-menu-modal-menu-item">
                      <span className="weekly-food-menu-modal-menu-label">반찬 1</span>
                      <span className="weekly-food-menu-modal-menu-value">{selectedMealDetail.side1}</span>
                    </div>
                    <div className="weekly-food-menu-modal-menu-item">
                      <span className="weekly-food-menu-modal-menu-label">반찬 2</span>
                      <span className="weekly-food-menu-modal-menu-value">{selectedMealDetail.side2}</span>
                    </div>
                    <div className="weekly-food-menu-modal-menu-item">
                      <span className="weekly-food-menu-modal-menu-label">반찬 3</span>
                      <span className="weekly-food-menu-modal-menu-value">{selectedMealDetail.side3}</span>
                    </div>
                  </div>
                </div>
                
                <div className="weekly-food-menu-modal-section">
                  <h3 className="weekly-food-menu-modal-section-title">영양 정보</h3>
                  <div className="weekly-food-menu-modal-nutrition-grid">
                    <div className="weekly-food-menu-modal-nutrition-item">
                      <span className="weekly-food-menu-modal-nutrition-label">칼로리</span>
                      <span className="weekly-food-menu-modal-nutrition-value">{selectedMealDetail.calories}</span>
                    </div>
                    <div className="weekly-food-menu-modal-nutrition-item">
                      <span className="weekly-food-menu-modal-nutrition-label">탄수화물</span>
                      <span className="weekly-food-menu-modal-nutrition-value">{selectedMealDetail.carbohydrate}</span>
                    </div>
                    <div className="weekly-food-menu-modal-nutrition-item">
                      <span className="weekly-food-menu-modal-nutrition-label">단백질</span>
                      <span className="weekly-food-menu-modal-nutrition-value">{selectedMealDetail.protein}</span>
                    </div>
                    <div className="weekly-food-menu-modal-nutrition-item">
                      <span className="weekly-food-menu-modal-nutrition-label">지방</span>
                      <span className="weekly-food-menu-modal-nutrition-value">{selectedMealDetail.fat}</span>
                    </div>
                    <div className="weekly-food-menu-modal-nutrition-item">
                      <span className="weekly-food-menu-modal-nutrition-label">당분</span>
                      <span className="weekly-food-menu-modal-nutrition-value">{selectedMealDetail.sugar}</span>
                    </div>
                    <div className="weekly-food-menu-modal-nutrition-item">
                      <span className="weekly-food-menu-modal-nutrition-label">나트륨</span>
                      <span className="weekly-food-menu-modal-nutrition-value">{selectedMealDetail.sodium}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default WeeklyFoodMenu;
