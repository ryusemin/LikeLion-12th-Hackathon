import { apiUrl } from '../config/endpoints';
import React, { useState ,useEffect} from "react";
import { useNavigate } from 'react-router-dom';
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faArrowLeft, faEye, faEyeSlash, faLock } from "@fortawesome/free-solid-svg-icons";
import '../CSS/PasswordChange.css';

function PasswordChange() {
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [userEmail, setUserEmail] = useState(""); 
    

    const [showCurrentPassword, setShowCurrentPassword] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    

    const [isChanging, setIsChanging] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const navigate = useNavigate();


    useEffect(() => {
        const fetchUserInfo = async () => {
            try {
                const response = await fetch(apiUrl('/api/users'), {
                    method: "GET",
                    credentials: 'include'
                });

                if (response.ok) {
                    const result = await response.json();
                    setUserEmail(result.email || "");
                } 
            } catch (error) {
                console.error('사용자 정보 조회 실패:', error);
            }
        };

        fetchUserInfo();
    }, [navigate]);



    const handlePasswordChange = async () => {

        if (!currentPassword || !newPassword || !confirmPassword) {
            setError("모든 필드를 입력해주세요.");
            return;
        }

        if (newPassword !== confirmPassword) {
            setError("새 비밀번호가 일치하지 않습니다.");
            return;
        }


        if (currentPassword === newPassword) {
            setError("기존 비밀번호와 새 비밀번호가 동일합니다.");
            return;
        }

        setIsChanging(true);
        setError("");
        setSuccess("");

        try {
            const response = await fetch(apiUrl('/api/users/password'), {
                method: "PUT",
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    email: userEmail, 
                    oldPassword: currentPassword,
                    newPassword: newPassword
                }),
            });

            const result = await response.json();

            if (response.ok) {
                setSuccess("비밀번호가 성공적으로 변경되었습니다.");
                setCurrentPassword("");
                setNewPassword("");
                setConfirmPassword("");
                

                setTimeout(() => {
                    navigate('/mypage');
                }, 2000);
            } else {
                if (result.message === "토큰소멸") {
                    alert("로그인이 만료되었습니다. 다시 로그인해주세요.");
                    navigate('/login');
                    return;
                }
                setError(result.message || "비밀번호 변경에 실패했습니다.");
            }
        } catch (error) {
            console.error("Password change error: ", error);
            setError("비밀번호 변경 중 오류가 발생했습니다.");
        } finally {
            setIsChanging(false);
        }
    };

    return (
        <div className="passwordchange_container">


            <div className="passwordchange_form">
                <div className="passwordchange_section">
                    <h3 className="passwordchange_section_title">
                        <FontAwesomeIcon icon={faLock} className="passwordchange_section_icon" />
                        보안 정보 변경
                    </h3>
                    <p className="passwordchange_section_desc">
                        안전한 계정 관리를 위해 정기적으로 비밀번호를 변경해주세요.
                    </p>
                </div>

                <div className="passwordchange_form_group">
                    <label className="passwordchange_label">현재 비밀번호</label>
                    <div className="passwordchange_input_wrapper">
                        <input
                            type={showCurrentPassword ? "text" : "password"}
                            value={currentPassword}
                            onChange={(e) => setCurrentPassword(e.target.value)}
                            placeholder="현재 비밀번호를 입력하세요"
                            className="passwordchange_input"
                        />
                        <button
                            type="button"
                            onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                            className="passwordchange_eye_btn"
                        >
                            <FontAwesomeIcon icon={showCurrentPassword ? faEyeSlash : faEye} />
                        </button>
                    </div>
                </div>

                <div className="passwordchange_form_group">
                    <label className="passwordchange_label">새 비밀번호</label>
                    <div className="passwordchange_input_wrapper">
                        <input
                            type={showNewPassword ? "text" : "password"}
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            placeholder="새 비밀번호를 입력하세요"
                            className="passwordchange_input"
                        />
                        <button
                            type="button"
                            onClick={() => setShowNewPassword(!showNewPassword)}
                            className="passwordchange_eye_btn"
                        >
                            <FontAwesomeIcon icon={showNewPassword ? faEyeSlash : faEye} />
                        </button>
                    </div>
                </div>

                <div className="passwordchange_form_group">
                    <label className="passwordchange_label">새 비밀번호 확인</label>
                    <div className="passwordchange_input_wrapper">
                        <input
                            type={showConfirmPassword ? "text" : "password"}
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            placeholder="새 비밀번호를 다시 입력하세요"
                            className="passwordchange_input"
                        />
                        <button
                            type="button"
                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                            className="passwordchange_eye_btn"
                        >
                            <FontAwesomeIcon icon={showConfirmPassword ? faEyeSlash : faEye} />
                        </button>
                    </div>
                </div>

                {error && <div className="passwordchange_error_message">{error}</div>}
                {success && <div className="passwordchange_success_message">{success}</div>}

                <button
                    onClick={handlePasswordChange}
                    disabled={isChanging}
                    className="passwordchange_submit_btn"
                >
                    {isChanging ? "변경 중..." : "비밀번호 변경"}
                </button>

                <div className="passwordchange_tips">
                    <h4 className="passwordchange_tips_title">안전한 비밀번호 만들기</h4>
                    <ul className="passwordchange_tips_list">
                        <li>최소 6자 이상 입력해주세요</li>
                        <li>영문, 숫자, 특수문자를 조합해주세요</li>

                    </ul>
                </div>
            </div>
        </div>
    );
}

export default PasswordChange;