import { apiFetch, login } from '../api/client';
import React, { useState } from "react";
import { useNavigate } from 'react-router-dom';
import '../CSS/Login.css';
import logo from '../images/logo.png';

function Login({ onLoginSuccess }) {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();

    const handleSignupClick = () => {
        navigate('/signup');
    };
    const handleAccountRecoveryClick = () => {
        navigate('/account-recovery')
    };

    const changeEmail = (e) => {
        setEmail(e.target.value);
    };

    const changePassword = (e) => {
        setPassword(e.target.value);
    };

    const handleLogin = async (event) => {
        event.preventDefault();

        // 입력값 검증
        if (!email.trim() || !password.trim()) {
            alert('아이디와 비밀번호를 모두 입력해주세요.');
            return;
        }

        try {
            const result = await login(email, password);

            if (result.ok) {
                try {
                    const userResponse = await apiFetch('/api/users', {
                        method: 'GET'
                    });

                    if (userResponse.ok) {
                        const userData = await userResponse.json();

                        if (onLoginSuccess) {
                            onLoginSuccess(userData);
                        }
                    }
                } catch (error) {
                    console.error('사용자 정보 조회 실패:', error);
                }

                navigate(result.role === "ROLE_ADMIN" ? '/admin' : '/');
            } else {
                alert("로그인 실패: " + (result.message || "이메일 또는 비밀번호를 확인해주세요."));
            }
        } catch (error) {
            console.error("Fetch error: ", error);
            alert("로그인 중 오류가 발생했습니다.");
        }
    };

    return (
        <div className="login-container">
            <div className="login-inner">
                <img src={logo} className="logoImage-login" alt="logo" />

                <form onSubmit={handleLogin} className="login-input-form">
                    <input
                        type="text"
                        id="username"
                        value={email}
                        className="login-email"
                        placeholder="아이디"
                        onChange={changeEmail}
                    />
                    <input
                        type="password"
                        id="password"
                        value={password}
                        className="login-password"
                        placeholder="비밀번호"
                        onChange={changePassword}
                    />

                    <button type="submit" className="login-btn">로그인</button>
                </form>

                <div className="login-options">
                    <span
                        className="account-recovery-link"
                        onClick={handleAccountRecoveryClick}
                    >
                        계정 찾기
                    </span>
                </div>
                <p className="signup-link">
                    아직 회원이 아니신가요? <span onClick={handleSignupClick}>회원가입</span>
                </p>
            </div>
        </div>
    );
}

export default Login;
