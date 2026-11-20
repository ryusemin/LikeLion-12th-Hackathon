import { apiFetch } from '../api/client';
import React, { useState } from "react";
import { useNavigate } from 'react-router-dom';
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faEye, faEyeSlash } from "@fortawesome/free-solid-svg-icons";
import '../CSS/AccountRecovery.css';

function AccountRecovery() {
    const [email, setEmail] = useState("");
    const [verificationCode, setVerificationCode] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");


    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    

    const [currentStep, setCurrentStep] = useState(1); 
    const [isEmailChecking, setIsEmailChecking] = useState(false);
    const [isCodeSending, setIsCodeSending] = useState(false);
    const [isCodeVerifying, setIsCodeVerifying] = useState(false);
    const [isPasswordChanging, setIsPasswordChanging] = useState(false);
    

    const [isEmailExists, setIsEmailExists] = useState(false);
    const [isCodeSent, setIsCodeSent] = useState(false);
    const [isCodeVerified, setIsCodeVerified] = useState(false);
    // 서버가 발급한 일회용 이메일 인증 증명. 재설정 요청에 함께 보낸다.
    const [verificationToken, setVerificationToken] = useState("");
    

    const [emailError, setEmailError] = useState("");
    const [emailSuccess, setEmailSuccess] = useState("");
    const [codeError, setCodeError] = useState("");
    const [codeSuccess, setCodeSuccess] = useState("");
    const [passwordError, setPasswordError] = useState("");
    const [passwordSuccess, setPasswordSuccess] = useState("");

    const navigate = useNavigate();


    const validateEmail = (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    };


    const handleEmailCheck = async () => {
        if (!email) {
            setEmailError("이메일을 입력해주세요.");
            return;
        }

        if (!validateEmail(email)) {
            setEmailError("올바른 이메일 형식을 입력해주세요.");
            return;
        }

        setIsEmailChecking(true);
        setEmailError("");
        setEmailSuccess("");

        try {
            const response = await apiFetch(`/api/users/isExist?email=${encodeURIComponent(email)}`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                },
                credentials: "include" 
            });

            if (response.ok) {
                const result = await response.json();
                
                if (result === true) {
                    setIsEmailExists(true);
                    setEmailSuccess("해당 이메일이 존재합니다.");
                } else {
                    setEmailError("해당 이메일로 등록된 계정이 없습니다.");
                    setIsEmailExists(false);
                }
            } else {
                setEmailError("이메일 확인 중 오류가 발생했습니다.");
                setIsEmailExists(false);
            }
        } catch (error) {
            console.error("Email check error: ", error);
            setEmailError("이메일 확인 중 오류가 발생했습니다.");
            setIsEmailExists(false);
        } finally {
            setIsEmailChecking(false);
        }
    };


    const handleSendVerificationCode = async () => {
        setIsCodeSending(true);
        setCodeError("");
        setCodeSuccess("");

        try {
            const response = await apiFetch('/api/email/send', {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                credentials: "include",
                body: JSON.stringify({
                    email: email,
                    purpose: "PASSWORD_RESET"
                }),
            });

            if (response.ok) {
                setIsCodeSent(true);
                setCodeSuccess("인증번호가 발송되었습니다. 이메일을 확인해주세요.");
                console.log("인증번호 발송 성공");
            } else {
                const result = await response.json().catch(() => ({}));
                setCodeError(result.message || "인증번호 발송에 실패했습니다.");
            }
        } catch (error) {
            console.error("Send verification code error: ", error);
            setCodeError("인증번호 발송 중 오류가 발생했습니다.");
        } finally {
            setIsCodeSending(false);
        }
    };


    const handleVerifyCode = async () => {
        if (!verificationCode) {
            setCodeError("인증번호를 입력해주세요.");
            return;
        }

        setIsCodeVerifying(true);
        setCodeError("");
        setCodeSuccess("");

        try {
            const response = await apiFetch('/api/email/check', {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "include",
                body: JSON.stringify({
                    email: email,
                    purpose: "PASSWORD_RESET",
                    code: parseInt(verificationCode)
                }),
            });

            if (response.ok) {
                const result = await response.json();
                if (result.success === true) {
                    setIsCodeVerified(true);
                    setVerificationToken(result.verificationToken || "");
                    setCodeSuccess(result.message || "이메일 인증이 완료되었습니다.");
                    setCurrentStep(3);
                } else {
                    setCodeError(result.message || "인증번호가 일치하지 않습니다.");
                }
            } else {
                const errorText = await response.text();
                try {
                    const errorJson = JSON.parse(errorText);
                    setCodeError(errorJson.message || "인증번호 확인에 실패했습니다.");
                } catch (e) {
                    setCodeError("인증번호 확인에 실패했습니다.");
                }
            }
        } catch (error) {
            console.error("Network error: ", error);
            setCodeError("네트워크 오류가 발생했습니다.");
        } finally {
            setIsCodeVerifying(false);
        }
    };


    const handlePasswordReset = async () => {
        if (!newPassword || !confirmPassword) {
            setPasswordError("모든 필드를 입력해주세요.");
            return;
        }

        if (newPassword !== confirmPassword) {
            setPasswordError("비밀번호가 일치하지 않습니다.");
            return;
        }

        if (newPassword.length < 6) {
            setPasswordError("비밀번호는 최소 6자 이상이어야 합니다.");
            return;
        }

        setIsPasswordChanging(true);
        setPasswordError("");
        setPasswordSuccess("");

        try {
            const response = await apiFetch('/api/users/reset-password', {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "include",
                body: JSON.stringify({
                    email: email,
                    newPassword: newPassword,
                    emailVerificationToken: verificationToken
                }),
            });

            const result = await response.json();

            if (response.ok) {
                setPasswordSuccess("비밀번호가 성공적으로 변경되었습니다.");
                setTimeout(() => {
                    navigate('/login');
                }, 2000);
            } else {
                // 증명 토큰은 한 번 쓰면 소비되므로, 실패하면 인증 단계로 돌아가 다시 받는다.
                const reason = result.password === "중복"
                    ? "기존과 다른 비밀번호를 입력해주세요."
                    : (result.message || "비밀번호 변경에 실패했습니다.");
                setVerificationToken("");
                setIsCodeVerified(false);
                setIsCodeSent(false);
                setVerificationCode("");
                setCodeSuccess("");
                setCodeError(reason + " 이메일 인증을 다시 진행해주세요.");
                setCurrentStep(2);
            }
        } catch (error) {
            console.error("Password reset error: ", error);
            setPasswordError("비밀번호 변경 중 오류가 발생했습니다.");
        } finally {
            setIsPasswordChanging(false);
        }
    };


    const goToLogin = () => {
        navigate('/login');
    };


    const handleGoBack = () => {
        if (currentStep > 1) {
            setCurrentStep(currentStep - 1);

            if (currentStep === 2) {
                setIsCodeSent(false);
                setIsCodeVerified(false);
                setVerificationToken("");
                setVerificationCode("");
                setCodeError("");
                setCodeSuccess("");
            } else if (currentStep === 3) {
                setNewPassword("");
                setConfirmPassword("");
                setPasswordError("");
                setPasswordSuccess("");
            }
        }
    };

    return (
        <div className="accountrecover_container">
            <div className="accountrecover_form">

                

                <div className="accountrecover_step-indicator">
                    <div 
                        className={`accountrecover_step ${currentStep >= 1 ? 'accountrecover_step-active' : ''}`}
                        data-step="이메일 확인"
                    ></div>
                    <div 
                        className={`accountrecover_step ${currentStep >= 2 ? 'accountrecover_step-active' : ''}`}
                        data-step="인증번호 확인"
                    ></div>
                    <div 
                        className={`accountrecover_step ${currentStep >= 3 ? 'accountrecover_step-active' : ''}`}
                        data-step="비밀번호 재설정"
                    ></div>
                </div>


                {currentStep > 1 && (
                    <div className="accountrecover_back-button">
                        <button 
                            onClick={handleGoBack}
                            className="accountrecover_btn accountrecover_btn-back"
                        >
                            <span className="accountrecover_btn-text">이전 단계</span>
                        </button>
                    </div>
                )}


                {currentStep === 1 && (
                    <div className="accountrecover_step-content">
                        <h3 className="accountrecover_step-title">이메일을 입력해주세요</h3>
                        <div className="accountrecover_form-group">
                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="이메일을 입력하세요"
                                className="accountrecover_form-input"
                            />
                            <button 
                                onClick={handleEmailCheck}
                                disabled={isEmailChecking}
                                className="accountrecover_btn accountrecover_btn-primary"
                            >
                                <span className="accountrecover_btn-text">
                                    {isEmailChecking ? "확인 중..." : "이메일 확인"}
                                </span>
                            </button>
                        </div>
                        {emailError && <div className="accountrecover_error-message">{emailError}</div>}
                        

                        {isEmailExists && emailSuccess && (
                            <div className="accountrecover_email-found">
                                <div className="accountrecover_success-message">{emailSuccess}</div>
                                <div className="accountrecover_option-buttons">
                                    <button 
                                        onClick={goToLogin} 
                                        className="accountrecover_btn accountrecover_btn-secondary"
                                    >
                                        <span className="accountrecover_btn-text">로그인 페이지로 이동</span>
                                    </button>
                                    <button 
                                        onClick={() => setCurrentStep(2)} 
                                        className="accountrecover_btn accountrecover_btn-primary"
                                    >
                                        <span className="accountrecover_btn-text">비밀번호 변경하기</span>
                                    </button>
                                </div>
                            </div>
                        )}
                        
                        {!isEmailExists && !emailSuccess && (
                            <div className="accountrecover_action-buttons">
                                <button onClick={goToLogin} className="accountrecover_btn accountrecover_btn-secondary">
                                    <span className="accountrecover_btn-text">로그인 페이지로 이동</span>
                                </button>
                            </div>
                        )}
                    </div>
                )}

                {currentStep === 2 && (
                    <div className="accountrecover_step-content">
                        <h3 className="accountrecover_step-title">이메일 인증</h3>
                        <p className="accountrecover_email-display">
                            <span className="accountrecover_email-label">이메일: </span>
                            <span className="accountrecover_email-value">{email}</span>
                        </p>
                        
                        <div className="accountrecover_form-group">
                            <button 
                                onClick={handleSendVerificationCode}
                                disabled={isCodeSending || isCodeSent}
                                className="accountrecover_btn accountrecover_btn-primary"
                            >
                                <span className="accountrecover_btn-text">
                                    {isCodeSending ? "발송 중..." : isCodeSent ? "인증번호 발송됨" : "인증번호 발송"}
                                </span>
                            </button>
                        </div>

                        {isCodeSent && (
                            <div className="accountrecover_form-group animate-in">
                                <input
                                    type="text"
                                    value={verificationCode}
                                    onChange={(e) => setVerificationCode(e.target.value)}
                                    placeholder="인증번호를 입력하세요"
                                    className="accountrecover_form-input"
                                />
                                <button 
                                    onClick={handleVerifyCode}
                                    disabled={isCodeVerifying}
                                    className="accountrecover_btn accountrecover_btn-primary"
                                >
                                    <span className="accountrecover_btn-text">
                                        {isCodeVerifying ? "확인 중..." : "인증번호 확인"}
                                    </span>
                                </button>
                            </div>
                        )}

                        {codeError && <div className="accountrecover_error-message">{codeError}</div>}
                        {codeSuccess && <div className="accountrecover_success-message">{codeSuccess}</div>}
                    </div>
                )}

                {currentStep === 3 && (
                    <div className="accountrecover_step-content">
                        <h3 className="accountrecover_step-title">새 비밀번호 설정</h3>
                        <p className="accountrecover_email-display">
                            <span className="accountrecover_email-label">이메일: </span>
                            <span className="accountrecover_email-value">{email}</span>
                        </p>
                        
                        <div className="accountrecover_form-group">
                            <div className="accountrecover_input-wrapper">
                                <input
                                    type={showNewPassword ? "text" : "password"}
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    placeholder="새 비밀번호를 입력하세요"
                                    className="accountrecover_form-input"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowNewPassword(!showNewPassword)}
                                    className="accountrecover_eye-btn"
                                >
                                    <FontAwesomeIcon icon={showNewPassword ? faEyeSlash : faEye} />
                                </button>
                            </div>
                        </div>

                        <div className="accountrecover_form-group">
                            <div className="accountrecover_input-wrapper">
                                <input
                                    type={showConfirmPassword ? "text" : "password"}
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    placeholder="새 비밀번호를 다시 입력하세요"
                                    className="accountrecover_form-input"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    className="accountrecover_eye-btn"
                                >
                                    <FontAwesomeIcon icon={showConfirmPassword ? faEyeSlash : faEye} />
                                </button>
                            </div>
                        </div>

                        <button 
                            onClick={handlePasswordReset}
                            disabled={isPasswordChanging}
                            className="accountrecover_btn accountrecover_btn-primary accountrecover_btn-full-width"
                        >
                            <span className="accountrecover_btn-text">
                                {isPasswordChanging ? "변경 중..." : "비밀번호 변경"}
                            </span>
                        </button>

                        {passwordError && <div className="accountrecover_error-message">{passwordError}</div>}
                        {passwordSuccess && <div className="accountrecover_success-message">{passwordSuccess}</div>}
                    </div>
                )}
            </div>
        </div>
    );
}

export default AccountRecovery;