import { apiUrl, notificationSocketUrl } from '../config/endpoints';
import React, { useEffect, useState, useCallback, useRef } from "react";
import { useNavigate } from 'react-router-dom';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import '../CSS/Notification.css';

function Notification() {
    const navigate = useNavigate();
    const [messages, setMessages] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [isConnected, setIsConnected] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [isDataLoaded, setIsDataLoaded] = useState(false); // 데이터 로딩 완료 여부
    const [userEmail, setUserEmail] = useState('user5@example.com'); // 기본값
    const stompClientRef = useRef(null);

    // 사용자 정보 조회 API
    const getUserInfo = useCallback(async () => {
        try {
            const response = await fetch(apiUrl('/api/users'), {
                method: "GET",
                credentials: 'include',
            });
      
            if (response.status === 200) {
                const result = await response.json();
                
                // 토큰 만료 체크
                if (result.message === "토큰소멸") {
                    alert("로그인이 만료되었습니다. 다시 로그인 해주세요");
                    navigate('/login');
                    return;
                }
                
                const email = result.email || 'test@example.com';
                setUserEmail(email);
            } else {                
                try {
                    const result = await response.json();
                    
                    // 토큰 만료 체크
                    if (result.message === "토큰소멸") {
                        alert("로그인이 만료되었습니다. 다시 로그인 해주세요");
                        navigate('/login');
                        return;
                    }
                } catch (e) {
                }
            }
        } catch (error) {
        }
    }, [navigate, userEmail]);

    // 모든 알림 조회 API
    const getAllNotificationsAPI = useCallback(async () => {
        try {
            const response = await fetch(apiUrl(`/api/notification/all?userEmail=${userEmail}`));
            
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

    // 알림 읽음 처리 API
    const markAsReadAPI = useCallback(async (notificationId) => {
        try {
            const response = await fetch(apiUrl(`/api/notification/${notificationId}`), {
                method: 'PATCH'
            });
            return response.ok;
        } catch (error) {
            return false;
        }
    }, []);

    // 모든 알림 읽음 처리 API
    const markAllAsReadAPI = useCallback(async () => {
        try {
            const response = await fetch(apiUrl(`/api/notification?userEmail=${userEmail}`), {
                method: 'PATCH'
            });
            return response.ok;
        } catch (error) {
            return false;
        }
    }, [userEmail]);

    const connectWebSocket = useCallback(() => {
        
        // 기존 연결이 있으면 먼저 해제
        if (stompClientRef.current) {
            stompClientRef.current.deactivate();
        }

        // SockJS 연결 시 query param으로 userEmail 전달
        const socket = new SockJS(notificationSocketUrl(userEmail));

        const client = new Client({
            webSocketFactory: () => socket,
            connectHeaders: {
                login: userEmail,        
                userEmail: userEmail     
            },
            debug: (str) => console.log("🔍 STOMP Debug:", str),
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            reconnectDelay: 5000,
            onConnect: (frame) => {
                setIsConnected(true);

                // 받은 메시지를 state에 추가하는 함수
                const handleMessage = (message) => {
                    
                    let body = message.body;
                    let parsed;
                    try {
                        parsed = JSON.parse(body);
                    } catch {
                        parsed = { content: body, type: "TEXT" };
                    }
                    
                    // PAYMENT와 DIET 타입만 알림에 표시
                    if (parsed.type !== 'PAYMENT' && parsed.type !== 'DIET') {
                        return;
                    }
                    
                    const notification = {
                        notificationId: parsed.notificationId || Date.now(),
                        content: parsed.content || parsed.notificationContent,
                        type: parsed.type || "TEXT",
                        sendTime: parsed.sendTime || new Date().toISOString(),
                        check: parsed.check || false
                    };
                    
                    setMessages(prev => {
                        return [notification, ...prev];
                    });
                    setUnreadCount(prev => {
                        return prev + 1;
                    });
                };

                // 유저 전용 알림만 구독
                const sub = client.subscribe(
                    "/user/queue/notification",
                    (msg) => {
                        handleMessage(msg);
                    }
                );
            },
            onWebSocketError: (err) => {
                setIsConnected(false);
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
    }, [userEmail]);

    // 단일 알림 읽음 처리
    const markAsRead = useCallback(async (notificationId) => {
        // UI 먼저 업데이트
        setMessages(prev => 
            prev.map(msg => 
                msg.notificationId === notificationId 
                    ? { ...msg, check: true }
                    : msg
            )
        );
        setUnreadCount(prev => Math.max(0, prev - 1));
        
        // API 호출
        const success = await markAsReadAPI(notificationId);
        if (!success) {
            // API 실패 시 UI 롤백
            setMessages(prev => 
                prev.map(msg => 
                    msg.notificationId === notificationId 
                        ? { ...msg, check: false }
                        : msg
                )
            );
            setUnreadCount(prev => prev + 1);
        }
    }, [markAsReadAPI]);

    // 모든 알림 읽음 처리
    const markAllAsRead = useCallback(async () => {
        // UI 먼저 업데이트
        const prevMessages = messages;
        const prevUnreadCount = unreadCount;
        
        setMessages(prev => 
            prev.map(msg => ({ ...msg, check: true }))
        );
        setUnreadCount(0);
        
        // API 호출
        const success = await markAllAsReadAPI();
        if (!success) {
            // API 실패 시 UI 롤백
            setMessages(prevMessages);
            setUnreadCount(prevUnreadCount);
        }
    }, [markAllAsReadAPI, messages, unreadCount]);

    // 초기 사용자 정보 조회 useEffect
    useEffect(() => {
        getUserInfo();
    }, [getUserInfo]);

    // 알림 데이터 로드 useEffect (userEmail이 변경될 때마다)
    useEffect(() => {
        const loadInitialNotifications = async () => {
            try {
                const notifications = await getAllNotificationsAPI();
                if (notifications && notifications.length > 0) {
                    // PAYMENT와 DIET 타입만 필터링
                    const filteredNotifications = notifications.filter(
                        notification => notification.type === 'PAYMENT' || notification.type === 'DIET'
                    );
                    
                    // API 응답 데이터를 컴포넌트 상태에 맞게 변환 및 최신순 정렬
                    const formattedNotifications = filteredNotifications
                        .map(notification => ({
                            notificationId: notification.notificationId,
                            content: notification.content,
                            type: notification.type || "TEXT",
                            sendTime: notification.sendTime,
                            check: notification.check || false
                        }))
                        .sort((a, b) => new Date(b.sendTime) - new Date(a.sendTime)); // 최신순 정렬
                    
                    setMessages(formattedNotifications);
                    
                    // 읽지 않은 알림 개수 계산
                    const unreadCount = formattedNotifications.filter(notification => !notification.check).length;
                    setUnreadCount(unreadCount);
                } else {
                    setMessages([]);
                    setUnreadCount(0);
                }
            } catch (error) {
                console.error('알림 로드 중 오류:', error);
                setMessages([]);
                setUnreadCount(0);
            } finally {
                setIsDataLoaded(true); // 데이터 로딩 완료
            }
        };

        if (userEmail && userEmail !== '') {
            loadInitialNotifications();
        }
    }, [userEmail, getAllNotificationsAPI]);

    // WebSocket 연결 useEffect (userEmail이 변경될 때마다 항상 실행)
    useEffect(() => {
        if (userEmail && userEmail !== '') {
            connectWebSocket();

            return () => {
                if (stompClientRef.current) {
                    stompClientRef.current.deactivate();
                }
            };
        }
    }, [userEmail, connectWebSocket]);

    return (
        <>
            <div className="notification_container" data-count={messages.length}>

                {/* 데이터 로딩이 완료되지 않은 경우 아무것도 렌더링하지 않음 */}
                {!isDataLoaded && null}

                {/* 데이터 로딩 완료 후 렌더링 */}
                {isDataLoaded && (
                    <>
                        {/* 읽지 않은 알림 개수 및 전체 읽음 처리 버튼 */}
                        {unreadCount > 0 && (
                            <div className="notification_header">
                                <span className="notification_unread_count">
                                    읽지 않은 알림 {unreadCount}개
                                </span>
                                <button 
                                    className="notification_mark_all_button"
                                    onClick={markAllAsRead}
                                    disabled={isLoading}
                                >
                                    모두 읽음
                                </button>
                            </div>
                        )}

                        {/* 알림 목록 */}
                        {messages.length > 0 ? (
                            messages.map((msg) => (
                                <div 
                                    className={`notification_item ${msg.check ? 'notification_read' : 'notification_unread'}`}
                                    key={msg.notificationId}
                                    data-type={msg.type}
                                    onClick={() => markAsRead(msg.notificationId)}
                                >
                                    <div className="notification_content">
                                        <span>{msg.content}</span>
                                    </div>
                                    
                                    <div className="notification_meta">
                                        {msg.type && (
                                            <div className="notification_type">
                                                {msg.type === 'DIET' ? ' 식단' : 
                                                 msg.type === 'PAYMENT' ? '결제' : ' 기타'}
                                            </div>
                                        )}
                                        {msg.sendTime && (
                                            <div className="notification_timestamp">
                                                {new Date(msg.sendTime).toLocaleString('ko-KR', {
                                                    month: 'short',
                                                    day: 'numeric',
                                                    hour: '2-digit',
                                                    minute: '2-digit'
                                                })}
                                            </div>
                                        )}
                                    </div>

                                    {/* 읽지 않은 알림 인디케이터 */}
                                    {!msg.check && <div className="notification_unread_indicator"></div>}
                                </div>
                            ))
                        ) : (
                            <div className="notification_empty">
                                 알림이 없습니다.
                            </div>
                        )}
                    </>
                )}
            </div>
        </>
    );
}

export default Notification;