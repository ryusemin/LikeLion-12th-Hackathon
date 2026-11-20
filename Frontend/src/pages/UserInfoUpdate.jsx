import { apiFetch } from '../api/client';
import React, { useState, useEffect } from "react";
import { useNavigate } from 'react-router-dom';
import Modal from "react-modal";
import DaumPostcode from "react-daum-postcode";
import '../CSS/UserInfoUpdate.css'; 

function UserInfoUpdate() {
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [phoneNumber, setphoneNumber] = useState("");
    const [zipCode, setZipcode] = useState("");
    const [roadAddress, setRoadAddress] = useState("");
    const [detailAddress, setDetailAddress] = useState(""); 
    const [isOpen, setIsOpen] = useState(false); 
    const [error, setError] = useState("");

    const navigate = useNavigate();

    const completeHandler = (data) => {
        setZipcode(data.zonecode);
        setRoadAddress(data.roadAddress);
        setIsOpen(false);
    };

    const customStyles = {
        overlay: {
            backgroundColor: "rgba(0, 0, 0, 0.6)",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            zIndex: 1000,
            backdropFilter: "blur(4px)",
            animation: "fadeIn 0.3s ease-out"
        },
        content: {
            position: "relative",
            top: "auto",
            left: "auto",
            right: "auto",
            bottom: "auto",
            margin: "20px",
            width: "90%",
            maxWidth: "600px",
            height: "80%",
            maxHeight: "700px",
            padding: "0",
            border: "none",
            borderRadius: "20px",
            boxShadow: "0 25px 50px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(255, 255, 255, 0.05)",
            overflow: "hidden",
            display: "flex",
            flexDirection: "column",
            background: "linear-gradient(145deg, #ffffff 0%, #f8f9fa 100%)",
            animation: "slideUpIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)"
        }
    };

    // 검색 클릭
    const toggle = () => {
        setIsOpen(!isOpen);
    };

    const validatePasswords = (password, confirmPassword) => {
        if (password !== confirmPassword) {
            setError("비밀번호가 일치하지 않습니다.");
        } else {
            setError("");
        }
    }

    // 정보 수정 API
    const handlePUT = async () => {
        try {
            const response = await apiFetch('/api/users', {
                method: "PUT",
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include', // 쿠키 인증
                body: JSON.stringify({
                    name: name,
                    email: email,
                    phoneNumber: phoneNumber,
                    roadAddress: roadAddress,
                    detailAddress: detailAddress,
                }),
            });

            const result = await response.json();

            if (response.status === 200) {
                alert("회원정보 수정 성공");
                navigate('/'); 
            } else {
                alert("회원정보 수정 실패: " + result.message);
            }
        } catch (error) {
            alert("회원정보 수정 중 오류가 발생했습니다.");
        }
    };
    
    // 사용자 정보 조회
    useEffect(() => {
        const handleget = async () => {
            try {
                const response = await apiFetch('/api/users', {
                    method: "GET",
                    credentials: 'include' // 쿠키 인증
                });

                const result = await response.json();

                if (response.status === 200) {
                    setName(result.name || "")
                    setEmail(result.email || "")
                    setphoneNumber(result.phoneNumber || "")
                    setRoadAddress(result.roadAddress || "");
                    setDetailAddress(result.detailAddress || "");
                    setZipcode(result.zipCode || ""); // 우편번호도 설정
                } else {
                    alert("사용자 정보 조회 실패: " + result.message);
                }
            } catch (error) {
                alert("사용자 정보 조회 중 오류가 발생했습니다.");
            }
        };
        handleget();
    }, []);

    return(
        <div className="userinfoupdate_info-update">
            <div className="userinfoupdate_update-form-group">
                <label htmlFor="name" className="userinfoupdate_update-form-group_label">이름</label>
                <input
                    type="text"
                    id="name"
                    value={name}
                    readOnly
                    className="userinfoupdate_update-form-group_input userinfoupdate_readonly"
      
                />
            </div>
            <div className="userinfoupdate_update-form-group">
                <label htmlFor="email" className="userinfoupdate_update-form-group_label">이메일</label>
                <input
                    type="email"
                    id="email"
                    value={email}
                    readOnly
                    className="userinfoupdate_update-form-group_input userinfoupdate_readonly"

                />
            </div>
            <div className="userinfoupdate_update-form-group">
                <label htmlFor="phonenumber" className="userinfoupdate_update-form-group_label">연락처</label>
                <input
                    type="text"
                    id="phonenumber"
                    value={phoneNumber}
                    onChange={(e) => setphoneNumber(e.target.value)}
                    className="userinfoupdate_update-form-group_input"
                />
            </div>


            <div className="userinfoupdate_update-form-group">
                <div className="userinfoupdate_update-address">
                    <label htmlFor="address" className="userinfoupdate_update-form-group_label">주소</label>
                    <div className="userinfoupdate_update-address-serch">
                        <input value={zipCode} readOnly placeholder="우편번호" className="userinfoupdate_update-form-group_input"/>
                        <button onClick={toggle} className="userinfoupdate_update-address-serch_btn">주소 찾기</button>
                    </div>

                    <div className="userinfoupdate_update-address-detail">
                        <input value={roadAddress} readOnly placeholder="도로명 주소" className="userinfoupdate_update-form-group_input" />

                        <Modal isOpen={isOpen} ariaHideApp={false} style={customStyles}>
                        <button 
                            onClick={toggle} 
                            style={{ 
                                alignSelf: 'center', 
                                padding: '12px 24px', 
                                fontSize: '16px', 
                                marginTop: '20px',
                                background: 'rgb(157, 146, 116)',
                                color: 'white',
                                border: 'none',
                                borderRadius: '12px',
                                fontWeight: '600',
                                cursor: 'pointer',
                                transition: 'all 0.3s ease',
                                boxShadow: '0 3px 12px rgba(157, 146, 116, 0.25)',
                                letterSpacing: '0.3px',
                                minWidth: '80px'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.background = 'rgb(127, 116, 86)';
                                e.target.style.transform = 'translateY(-2px)';
                                e.target.style.boxShadow = '0 6px 18px rgba(157, 146, 116, 0.35)';
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.background = 'rgb(157, 146, 116)';
                                e.target.style.transform = 'translateY(0)';
                                e.target.style.boxShadow = '0 3px 12px rgba(157, 146, 116, 0.25)';
                            }}
                            onMouseDown={(e) => {
                                e.target.style.transform = 'translateY(0)';
                            }}
                        >
                            닫기
                        </button>                              
                        <DaumPostcode onComplete={completeHandler} height="100%" />
                        </Modal>
                        
                        <input
                            type="text"
                            onChange={(e) => setDetailAddress(e.target.value)}
                            value={detailAddress}
                            className="userinfoupdate_update-form-group_input"
                        />
                    </div>
                </div>
            </div> 
            <button className="userinfoupdate_update-btn" onClick={handlePUT}>수정하기</button>
        </div>
    );
}

export default UserInfoUpdate;