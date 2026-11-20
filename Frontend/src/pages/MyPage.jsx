import { apiFetch, logout } from '../api/client';
import React, { useState, useEffect } from "react";
import { useNavigate } from 'react-router-dom';
import { Avatar } from 'antd';
import '../CSS/Mypage.css';
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPenToSquare, faReceipt, faSignOutAlt, faUserMinus, faChevronRight, faMapMarkerAlt, faPhone, faLock } from "@fortawesome/free-solid-svg-icons";

function MyPage({ onLogoutSuccess }) {

    const [Image] = useState("https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png")

    const [userName, setUserName] = useState('');
    const [address, setAddress] = useState({ roadAddress: '', detailAddress: '' });
    const [phoneNumber, setphoneNumber] = useState('');
    const navigate = useNavigate();


    const handleLogout = async () => {
        try {
            await logout();
        } catch (error) {
            // 서버 호출이 실패해도 이 기기의 토큰은 이미 비웠다.
        }
        if (onLogoutSuccess) {
            onLogoutSuccess();
        }
        navigate("/");
    };



    useEffect(() => {
        const handleget = async () => {
            try {
                const response = await apiFetch('/api/users', {
                    method: "GET",
                    credentials: 'include',
                });

                if (response.status === 200) {
                    const result = await response.json();


                    if (result.message === "토큰소멸") {
                        alert("로그인이 만료되었습니다. 다시 로그인 해주세요");
                        navigate('/login');
                        return;
                    }

                    setUserName(result.name);
                    setphoneNumber(result.phoneNumber)
                    setAddress({
                        roadAddress: result.roadAddress,
                        detailAddress: result.detailAddress,
                    });
                } else {
                    try {
                        const result = await response.json();


                        if (result.message === "토큰소멸") {
                            alert("로그인이 만료되었습니다. 다시 로그인 해주세요");
                            navigate('/login');
                            return;
                        }

                        if (response.status === 403) {
                            alert("접근 권한이 없습니다. 다시 로그인해주세요.");
                        } else {
                            alert("사용자 정보 조회 실패: " + result.message);
                        }
                    } catch (e) {
                        alert("사용자 정보 조회 실패");
                    }
                }
            } catch (error) {
                alert("사용자 정보 조회 중 오류가 발생했습니다.");
            }
        };
        handleget();
    }, [navigate])

    return (
        <>
            <div className="mypage_container">
                <div className="mypage_profile_card">
                    <Avatar
                        src={Image || ""}
                        className="mypage_avatar"
                        size={64}
                    />
                    <div className="mypage_profile_text">
                        <h2 className="mypage_name">{userName}</h2>
                        <p className="mypage_greeting">마이페이지에 오신 것을 환영합니다</p>
                    </div>
                </div>

                <div className="mypage_section">
                    <h3 className="mypage_section_title">계정 정보</h3>
                    <div className="mypage_info_list">
                        <div className="mypage_info_item">
                            <div className="mypage_info_icon_wrapper">
                                <FontAwesomeIcon icon={faMapMarkerAlt} className="mypage_info_icon" />
                            </div>
                            <div className="mypage_info_content">
                                <span className="mypage_info_label">주소</span>
                                <span className="mypage_info_value">
                                    {address.roadAddress} {address.detailAddress}
                                </span>
                            </div>
                        </div>
                        <div className="mypage_info_item">
                            <div className="mypage_info_icon_wrapper">
                                <FontAwesomeIcon icon={faPhone} className="mypage_info_icon" />
                            </div>
                            <div className="mypage_info_content">
                                <span className="mypage_info_label">연락처</span>
                                <span className="mypage_info_value">{phoneNumber}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="mypage_section">
                    <h3 className="mypage_section_title">서비스</h3>
                    <div className="mypage_menu_list">
                        <button className="mypage_menu_item" onClick={() => navigate('/userinfoupdate')}>
                            <div className="mypage_menu_content">
                                <div className="mypage_menu_icon_wrapper">
                                    <FontAwesomeIcon icon={faPenToSquare} className="mypage_menu_icon" />
                                </div>
                                <div className="mypage_menu_text">
                                    <span className="mypage_menu_title">내 정보 수정</span>
                                    <span className="mypage_menu_subtitle">개인정보 및 계정 설정</span>
                                </div>
                            </div>
                            <FontAwesomeIcon icon={faChevronRight} className="mypage_menu_arrow" />
                        </button>

                        <button className="mypage_menu_item" onClick={() => navigate('/password-change')}>
                            <div className="mypage_menu_content">
                                <div className="mypage_menu_icon_wrapper">
                                    <FontAwesomeIcon icon={faLock} className="mypage_menu_icon" />
                                </div>
                                <div className="mypage_menu_text">
                                    <span className="mypage_menu_title">비밀번호 변경</span>
                                    <span className="mypage_menu_subtitle">계정 보안 강화</span>
                                </div>
                            </div>
                            <FontAwesomeIcon icon={faChevronRight} className="mypage_menu_arrow" />
                        </button>

                        <button className="mypage_menu_item" onClick={() => navigate('/orderlist')}>
                            <div className="mypage_menu_content">
                                <div className="mypage_menu_icon_wrapper">
                                    <FontAwesomeIcon icon={faReceipt} className="mypage_menu_icon" />
                                </div>
                                <div className="mypage_menu_text">
                                    <span className="mypage_menu_title">주문 내역</span>
                                    <span className="mypage_menu_subtitle">주문 내역 전체 보기</span>
                                </div>
                            </div>
                            <FontAwesomeIcon icon={faChevronRight} className="mypage_menu_arrow" />
                        </button>
                    </div>
                </div>

                <div className="mypage_section">
                    <h3 className="mypage_section_title">계정 관리</h3>
                    <div className="mypage_action_list">
                        <button className="mypage_action_item" onClick={handleLogout}>
                            <div className="mypage_action_icon_section">
                                <FontAwesomeIcon icon={faSignOutAlt}
                                    className="mypage_action_icon" />
                            </div>
                            <span className="mypage_action_text">로그아웃</span>
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
}

export default MyPage;