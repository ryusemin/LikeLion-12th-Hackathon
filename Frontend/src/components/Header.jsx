import { apiUrl } from '../config/endpoints';
import React, { useState, useEffect } from "react";
import '../CSS/Header.css';
import logo from '../images/logo.png';
import { useNavigate, useLocation } from "react-router-dom";
import { TbArrowBackUp } from "react-icons/tb";
import { IoNotifications } from "react-icons/io5";
import { FaPenToSquare } from "react-icons/fa6";

const Header = ({ notificationCount, surveyCount, userInfo }) => {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [userName, setUserName] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        checkLoginStatus();
    }, []);

    useEffect(() => {
        checkLoginStatus();
    }, [location.pathname]);

    const checkLoginStatus = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(apiUrl('/api/users'), {
                method: 'GET',
                credentials: 'include',
                headers: {
                    'accept': '*/*'
                }
            });

            if (response.ok) {
                const userData = await response.json();
                setIsLoggedIn(true);
                setUserName(userData.name || '사용자');
                localStorage.setItem("isLoggedIn", "true");
            } else {
                setIsLoggedIn(false);
                setUserName('');
                localStorage.removeItem("isLoggedIn");
                localStorage.removeItem("token");
            }
        } catch (error) {
            setIsLoggedIn(false);
            setUserName('');
            localStorage.removeItem("isLoggedIn");
            localStorage.removeItem("token");
        } finally {
            setIsLoading(false);
        }
    };

    const handleLoginClick = () => {
        navigate('/login');
    };

    const handleBackClick = () => {
        navigate(-1);
    };

    const handleIconClick = (path) => {
        if (isLoggedIn) {
            navigate(path);
        } else {
            navigate('/login');
        }
    };

    const getPageTitle = () => {
        switch (location.pathname) {
            case '/login':
                return '로그인';
            case '/signup':
                return '회원가입';
            case '/alldiet':
                return '모든 식단 목록';
            case '/dietinfo':
                return '식단 상세 정보';
            case '/mypage':
                return '내정보';
            case '/userinfoupdate':
                return '내 정보 수정';
            case '/orderlist':
                return '주문 내역';
            case '/weeklyfoodmenu':
                return '내 식단';
            case '/survey':
                return '식단 리뷰';
            case '/notification':
                return '알림';
            case '/dietselection':
                return '식단 선택';
            case '/menuselection':
                return '메뉴 선택';
            case '/dietpayment':
                return '식단 결제';
            case '/dietpaymentmain':
                return '주문 확인';
            case '/dietpaymentcomplete':
                return '주문 완료';
            case '/admin':
                return '관리자 페이지';
            case '/account-recovery':
                return '계정 찾기';
            case '/password-change':
                return '비밀번호 변경';
            default:
                return 'NutriHub';
        }
    };

    const isMainPage = location.pathname === '/';


    if (isLoading && isMainPage) {
        return (
            <header className="header-header">
                <div className="header-header-container">
                    <div className="header-header-left">
                        <div className="header-loading-placeholder"></div>
                    </div>
                    <div className="header-header-center">
                        <img
                            src={logo}
                            className="header-logo-image"
                            alt="NutriHub"
                            onClick={() => navigate('/')}
                        />
                    </div>
                    <div className="header-header-right">
                        <div className="header-loading-placeholder"></div>
                    </div>
                </div>
            </header>
        );
    }

    return (
        <header className="header-header">
            <div className="header-header-container">
                {!isMainPage && (
                    <div className="header-other-page-header">
                        <button
                            className="header-back-button"
                            onClick={handleBackClick}
                            aria-label="뒤로가기"
                        >
                            <TbArrowBackUp size="20" />
                        </button>
                        <h1 className="header-page-title">{getPageTitle()}</h1>
                    </div>
                )}
                {isMainPage && (
                    <>
                        <div className="header-header-left">
                            {isLoggedIn ? (
                                <span className="header-user-greeting">
                                    {userName || '사용자'}님
                                </span>
                            ) : (
                                <button
                                    className="header-login-button"
                                    onClick={handleLoginClick}
                                >
                                    로그인
                                </button>
                            )}
                        </div>
                        <div className="header-header-center">
                            <img
                                src={logo}
                                className="header-logo-image"
                                alt="NutriHub"
                                onClick={() => navigate('/')}
                            />
                        </div>
                        <div className="header-header-right">
                            {isLoggedIn && (
                                <div className="header-header-actions">
                                    <button
                                        className="header-action-button"
                                        onClick={() => handleIconClick('/notification')}
                                        aria-label="알림"
                                    >
                                        <IoNotifications size={20}/>
                                        {notificationCount > 0 && (
                                            <span className="header-notification-badge">
                                                {notificationCount}
                                            </span>
                                        )}
                                    </button>
                                    <button
                                        className="header-action-button"
                                        onClick={() => handleIconClick('/survey')}
                                        aria-label="설문조사"
                                    >
                                        <FaPenToSquare size={18}/>
                                        {surveyCount > 0 && (
                                            <span className="header-notification-badge">
                                                {surveyCount}
                                            </span>
                                        )}
                                    </button>
                                </div>
                            )}
                        </div>
                    </>
                )}
            </div>
        </header>
    );
};

export default Header;
