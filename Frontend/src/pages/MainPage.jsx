import { notificationSocketUrl } from '../config/endpoints';
import { apiFetch, authorizeNotificationSocket } from '../api/client';
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Swiper, SwiperSlide } from 'swiper/react';
import 'swiper/css';
import 'swiper/css/pagination';
import 'swiper/css/autoplay';
import { Pagination, Autoplay } from 'swiper/modules';
import '../CSS/MainPage.css';
import { useNavigate } from 'react-router-dom';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

import slideImg1 from '../images/mainSlideImg1.jpeg';
import slideImg2 from '../images/mainSlideImg2.jpeg';
import slideImg3 from '../images/mainSlideImg3.png';
import cardImg1 from '../images/mainCardImg1.jpeg';
import cardImg2 from '../images/mainCardImg2.jpeg';
import cardImg3 from '../images/mainCardImg3.jpeg';

const MainPage = ({ onNotificationCountChange }) => {
  const [user, setUser] = useState(null);
  const [todayMeals, setTodayMeals] = useState(null);
  const [isLogin, setIsLogin] = useState(false);
  const [loading, setLoading] = useState(true);
  const [notificationCount, setNotificationCount] = useState(0);
  const [surveyCount, setSurveyCount] = useState(0);
  const [userEmail, setUserEmail] = useState('');
  const [isConnected, setIsConnected] = useState(false);
  const stompClientRef = useRef(null);
  const isInitialized = useRef(false);
  const connectionAttempts = useRef(0);
  const pollingInterval = useRef(null);

  const navigate = useNavigate();

  // 오늘의 식단 샘플 데이터
  const sampleTodayMeals = {
    breakfast: {
      name: "건강한 아침 정식",
      image: cardImg1,
      calories: "520kcal",
      protein: "18g",
      carbs: "45g",
      description: "현미밥, 된장찌개, 김치, 시금치나물"
    },
    lunch: {
      name: "프로틴 파워볼",
      image: cardImg2,
      calories: "480kcal",
      protein: "35g",
      carbs: "25g",
      description: "그릴드 치킨, 퀴노아, 아보카도, 견과류"
    },
    dinner: {
      name: "연어 스테이크 세트",
      image: cardImg3,
      calories: "620kcal",
      protein: "32g",
      carbs: "42g",
      description: "구운 연어, 현미밥, 아스파라거스, 방울토마토"
    }
  };

  const heroSlides = [
    {
      title: "바쁜 당신을 위한\n영양사의 한 끼",
      subtitle: "AI 영양사가 추천하는 맞춤 식단을\n새벽에 받아보세요",
      image: slideImg1,
      cta: "구독 시작하기"
    },
    {
      title: "매일 새벽 6시,\n문 앞까지",
      subtitle: "하루 세 끼를 한 번에 받아\n직장에서도 건강하게",
      image: slideImg2,
      cta: "구독 시작하기"
    },
    {
      title: "21가지 메뉴 중\n자유롭게",
      subtitle: "일주일간 다양한 메뉴를\n취향에 맞게 선택하세요",
      image: slideImg3,
      cta: "메뉴 보기"
    }
  ];

  const features = [
    {
      title: "새벽 배송",
      description: "매일 새벽 6시 문 앞까지 배송",
      highlight: "출근 전 수령 가능"
    },
    {
      title: "AI 맞춤 추천",
      description: "개인 취향과 영양 필요량 분석한 맞춤 식단",
      highlight: "개인별 최적화"
    },
    {
      title: "영양 균형",
      description: "전문 영양사 감수 균형잡힌 영양 구성",
      highlight: "건강한 한 끼"
    },
    {
      title: "합리적 가격",
      description: "한 끼 6,500원부터 무료 배송",
      highlight: "부담 없는 가격"
    }
  ];

  // 사용자 정보 조회 API
  const getUserInfo = useCallback(async () => {
    try {
      const response = await apiFetch('/api/users', {
        method: "GET",
        credentials: 'include',
      });

      if (response.status === 200) {
        const result = await response.json();

        if (result.message === "토큰소멸") {
          setIsLogin(false);
          return;
        }

        const email = result.email;
        const name = result.name;
        
        setUserEmail(email);
        setUser({ name, email });
        setIsLogin(true);
      } else {
        setIsLogin(false);
      }
    } catch (error) {
      setIsLogin(false);
    }
  }, []);

  // 모든 알림 조회 API
  const getAllNotificationsAPI = useCallback(async () => {
    if (!userEmail) return [];
    
    try {
      const response = await apiFetch(`/api/notification/all?userEmail=${userEmail}`);

      if (response.ok) {
        const data = await response.json();
        return data;
      } else {
        return [];
      }
    } catch (error) {
      return [];
    }
  }, [userEmail]);

  // 알림 개수 계산 함수
  const calculateNotificationCounts = useCallback((notifications) => {
    let notifCount = 0;
    let survCount = 0;

    if (Array.isArray(notifications)) {
      notifications.forEach((notification) => {
        if (!notification.check) {
          if (notification.type === 'DIET' || notification.type === 'PAYMENT') {
            notifCount++;
          } else if (notification.type === 'REVIEW') {
            survCount++;
          }
        }
      });
    }

    return { notifCount, survCount };
  }, []);

  // 알림 개수 업데이트 함수
  const updateNotificationCounts = useCallback(async () => {
    const notifications = await getAllNotificationsAPI();
    if (notifications && notifications.length > 0) {
      const { notifCount, survCount } = calculateNotificationCounts(notifications);
      setNotificationCount(notifCount);
      setSurveyCount(survCount);
      if (onNotificationCountChange) {
        onNotificationCountChange(notifCount, survCount);
      }
    } else {
      setNotificationCount(0);
      setSurveyCount(0);
      if (onNotificationCountChange) {
        onNotificationCountChange(0, 0);
      }
    }
  }, [getAllNotificationsAPI, calculateNotificationCounts, onNotificationCountChange]);

  // WebSocket 연결
  const connectWebSocket = useCallback(() => {
    if (!userEmail) return;

    // 연결 시도 횟수 제한
    if (connectionAttempts.current >= 3) {
      return;
    }

    // 기존 연결 정리
    if (stompClientRef.current) {
      const currentState = stompClientRef.current.state;
      if (currentState === 1) { // CONNECTING 상태
        return;
      }
      
      if (stompClientRef.current.connected) {
        stompClientRef.current.deactivate();
      }
    }

    connectionAttempts.current += 1;

    const client = new Client({
      // 재연결마다 새 SockJS 연결을 만든다. 닫힌 소켓을 다시 쓰면 재연결되지 않는다.
      webSocketFactory: () => new SockJS(notificationSocketUrl()),
      beforeConnect: () => authorizeNotificationSocket(client),
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      reconnectDelay: 5000,
      maxReconnectAttempts: 3,
      connectionTimeout: 10000,
      onConnect: async (frame) => {
        setIsConnected(true);
        connectionAttempts.current = 0;

        // 알림 데이터 조회
        await updateNotificationCounts();

        const handleMessage = (message) => {
          let body = message.body;
          let parsed;
          try {
            parsed = JSON.parse(body);
          } catch {
            parsed = { content: body, type: "TEXT" };
          }

          // 타입별 알림 개수 업데이트
          if (parsed.type === 'DIET' || parsed.type === 'PAYMENT') {
            setNotificationCount(prev => {
              const newCount = prev + 1;
              if (onNotificationCountChange) {
                setSurveyCount(currentSurvCount => {
                  onNotificationCountChange(newCount, currentSurvCount);
                  return currentSurvCount;
                });
              }
              return newCount;
            });
          } else if (parsed.type === 'REVIEW') {
            setSurveyCount(prev => {
              const newCount = prev + 1;
              if (onNotificationCountChange) {
                setNotificationCount(currentNotifCount => {
                  onNotificationCountChange(currentNotifCount, newCount);
                  return currentNotifCount;
                });
              }
              return newCount;
            });
          }
        };

        client.subscribe(
          "/user/queue/notification",
          (msg) => {
            handleMessage(msg);
          }
        );
      },
      onWebSocketError: (err) => {
        setIsConnected(false);
        // 재연결 로직
        if (connectionAttempts.current < 3) {
          setTimeout(() => {
            connectWebSocket();
          }, 5000);
        }
      },
      onStompError: (frame) => {
        setIsConnected(false);
      },
      onDisconnect: (receipt) => {
        setIsConnected(false);
      },
    });

    client.activate();
    stompClientRef.current = client;
  }, [userEmail, updateNotificationCounts, onNotificationCountChange]);

  // 환경별 알림 시스템 관리
  useEffect(() => {
    if (isLogin && userEmail) {
      // 환경 감지
      const isDevelopment = window.location.hostname === 'localhost' || 
                           window.location.hostname === '127.0.0.1';
      
      if (isDevelopment) {
        // 로컬 환경: WebSocket 연결
        const timer = setTimeout(() => {
          connectWebSocket();
        }, 500);

        return () => {
          clearTimeout(timer);
          // 토큰을 기다리는 중(아직 connected가 아님)인 클라이언트도 멈춰야 화면을 떠난 뒤 연결이 열리지 않는다.
          if (stompClientRef.current) {
            stompClientRef.current.deactivate();
          }
        };
      } else {
        // 초기 로드
        updateNotificationCounts();
        
        // 5초마다 폴링
        pollingInterval.current = setInterval(() => {
          updateNotificationCounts();
        }, 5000);
        
        return () => {
          if (pollingInterval.current) {
            clearInterval(pollingInterval.current);
          }
        };
      }
    }
  }, [userEmail, isLogin, connectWebSocket, updateNotificationCounts]);

  // 초기 데이터 로드
  useEffect(() => {
    if (isInitialized.current) return;
    
    const fetchData = async () => {
      try {
        isInitialized.current = true;
        
        await getUserInfo();
        setTodayMeals(sampleTodayMeals);
      } catch (error) {
        // 에러 처리
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [getUserInfo]);

  const handleSubscribeClick = () => {
    if (isLogin) {
      navigate('/dietselection');
    } else {
      navigate('/login');
    }
  };

  const handleMenuClick = () => {
    navigate('/alldiet');
  };

  if (loading) {
    return (
      <div className="main-page-loading-container">
        <div className="main-page-loading-spinner"></div>
        <p>영양 데이터를 불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="main-page-main-container">
      <section className="main-page-hero-slider">
        <Swiper
          spaceBetween={0}
          slidesPerView={1}
          pagination={{ clickable: true }}
          autoplay={{ delay: 4000, disableOnInteraction: false, pauseOnMouseEnter: true }}
          modules={[Pagination, Autoplay]}
          className="main-page-hero-swiper"
        >
          {heroSlides.map((slide, index) => (
            <SwiperSlide key={index}>
              <div className="main-page-hero-slide">
                <div className="main-page-hero-background">
                  <img src={slide.image} alt={slide.title} />
                  <div className="main-page-hero-overlay"></div>
                </div>
                <div className="main-page-hero-content">
                  <h1>{slide.title}</h1>
                  <p>{slide.subtitle}</p>
                  <button
                    className="main-page-hero-cta-button"
                    onClick={index !== 2 ? handleSubscribeClick : handleMenuClick}
                  >
                    {slide.cta}
                  </button>
                </div>
              </div>
            </SwiperSlide>
          ))}
        </Swiper>
      </section>

      <section className="main-page-today-meals-section">
        <div className="main-page-section-header">
          <h2>
            오늘의 추천 식단
          </h2>
          <p>영양사가 추천하는 균형잡힌 하루 세 끼</p>
        </div>

        <div className="main-page-meals-grid">
          <div className="main-page-meal-card main-page-breakfast">
            <div className="main-page-meal-time">
              <span className="main-page-time-label">아침</span>
            </div>
            <div className="main-page-meal-image">
              <img src={todayMeals.breakfast.image} alt={todayMeals.breakfast.name} />
            </div>
            <div className="main-page-meal-info">
              <h3>{todayMeals.breakfast.name}</h3>
              <p>{todayMeals.breakfast.description}</p>
              <div className="main-page-nutrition-info">
                <span>{todayMeals.breakfast.calories}</span>
                <span>단백질 {todayMeals.breakfast.protein}</span>
              </div>
            </div>
          </div>

          <div className="main-page-meal-card main-page-lunch">
            <div className="main-page-meal-time">
              <span className="main-page-time-label">점심</span>
            </div>
            <div className="main-page-meal-image">
              <img src={todayMeals.lunch.image} alt={todayMeals.lunch.name} />
            </div>
            <div className="main-page-meal-info">
              <h3>{todayMeals.lunch.name}</h3>
              <p>{todayMeals.lunch.description}</p>
              <div className="main-page-nutrition-info">
                <span>{todayMeals.lunch.calories}</span>
                <span>단백질 {todayMeals.lunch.protein}</span>
              </div>
            </div>
          </div>

          <div className="main-page-meal-card main-page-dinner">
            <div className="main-page-meal-time">
              <span className="main-page-time-label">저녁</span>
            </div>
            <div className="main-page-meal-image">
              <img src={todayMeals.dinner.image} alt={todayMeals.dinner.name} />
            </div>
            <div className="main-page-meal-info">
              <h3>{todayMeals.dinner.name}</h3>
              <p>{todayMeals.dinner.description}</p>
              <div className="main-page-nutrition-info">
                <span>{todayMeals.dinner.calories}</span>
                <span>단백질 {todayMeals.dinner.protein}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="main-page-features-section">
        <div className="main-page-section-header">
          <h2>우리 동네 영양사만의 <br /> 특별함</h2>
          <p>건강하고 편리한 식사 솔루션을 제공합니다</p>
        </div>

        <div className="main-page-features-grid">
          {features.map((feature, index) => (
            <div key={index} className="main-page-feature-card">
              <h3>{feature.title}</h3>
              <p>{feature.description}</p>
              <span className="main-page-feature-highlight">{feature.highlight}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="main-page-cta-section">
        <div className="main-page-cta-content">
          <h2>건강한 식습관,<br />지금 시작하세요!</h2>
          <p>바쁜 일상 속에서도 균형잡힌 영양을 챙기세요</p>
          <button
            className="main-page-cta-button"
            onClick={handleSubscribeClick}
          >
            구독 시작하기
          </button>
        </div>
      </section>
    </div>
  );
};

export default MainPage;
