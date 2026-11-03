import { apiUrl } from '../config/endpoints';
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import '../CSS/OrderList.css';

function OrderList() {
    const [payments, setPayments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();


    const [userEmail, setUserEmail] = useState(''); 

    useEffect(() => {
        const handleGetUser = async () => {
            try {
                const response = await fetch(apiUrl('/api/users'), {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include', 
                });

                if (response.ok) {
                    const userData = await response.json();

                    if (userData.message === "토큰소멸") {
                        alert("로그인이 만료되었습니다. 다시 로그인 해주세요");
                        navigate('/login');
                        return;
                    }
                    
                    // 사용자 이메일이 있으면 설정, 없으면 기본값 유지
                    if (userData.email) {
                        setUserEmail(userData.email);
                    }
                } else {                    
                    try {
                        const userData = await response.json();
                        if (userData.message === "토큰소멸") {
                            alert("로그인이 만료되었습니다. 다시 로그인 해주세요");
                            navigate('/login');
                            return;
                        }
                    } catch (e) {
                    }
                }
            } catch (error) {

            }
        };

        handleGetUser();
    }, [navigate, userEmail]);

    // 결제 내역 조회
    useEffect(() => {
        const handlePaymentGet = async () => {
            try {
                const response = await fetch(apiUrl(`/api/payment?purchaser=${userEmail}`), {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include', 
                });

                if (!response.ok) {
                    throw new Error('결제 내역을 불러오는데 실패했습니다.');
                }

                const paymentData = await response.json();
                
                // 날짜 기준으로 최신순 정렬 (내림차순)
                const sortedPayments = paymentData.sort((a, b) => {
                    return new Date(b.createdAt) - new Date(a.createdAt);
                });
                
                setPayments(sortedPayments);
            } catch (error) {
                setError(error.message);
            } finally {
                setLoading(false);
            }
        };
    
        handlePaymentGet();
    }, [userEmail]);
    
    const formatDate = (dateString) => {
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}년 ${month}월 ${day}일`;
    };

    const handleDetailClick = (weeklyId) => {
        navigate('/weeklyfoodmenu', { state: { weeklyId } });
    };

    if (loading) {
        return (
            <div className="orderList_container">
                <div className="loading">주문 내역을 불러오는 중...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="orderList_container">
                <div className="error">오류: {error}</div>
            </div>
        );
    }

    if (payments.length === 0) {
        return (
            <div className="orderList_container">
                <div className="empty">주문 내역이 없습니다.</div>
            </div>
        );
    }

    return (
        <div className="orderList_container">
            {payments.map((payment, index) => (
                <div key={payment.id} className="orderList_wrapper">
                    <div className="orderList_card" onClick={() => handleDetailClick(payment.weeklyDietId)}>
                        <div className="orderList_header">
                            <div className="orderList_orderNumber">
                                주문 번호: {payment.id}
                            </div>
                        </div>
                        
                        <div className="orderList_content">
                            <div className="orderList_mainInfo">
                                <div className="orderList_dateSection">
                                    <span className="orderList_label">결제 일시</span>
                                    <div className="orderList_dateValue">{formatDate(payment.createdAt)}</div>
                                </div>
                                <div className="orderList_priceSection">
                                    <span className="orderList_label">결제 금액</span>
                                    <div className="orderList_price">{payment.totalPrice.toLocaleString()}원</div>
                                </div>
                            </div>
                            
                            <div className="orderList_menuInfo">
                                <div className="orderList_menuTitle">주문 메뉴</div>
                                <div className="orderList_menuContent">주간 식단 #{payment.weeklyDietId}</div>
                            </div>
                            
                            <div className="orderList_detailSection">
                                <button className="orderList_detailButton">
                                    자세히 보기
                                    <span className="orderList_arrow">→</span>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            ))}
        </div>
    );
}

export default OrderList;