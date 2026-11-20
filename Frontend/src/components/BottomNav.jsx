import { apiFetch } from '../api/client';
import React, { useEffect, useState } from "react";
import "../CSS/BottomNav.css";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faHouse, faBowlFood, faCartPlus, faUser, faBars } from "@fortawesome/free-solid-svg-icons";
import { useNavigate, useLocation } from 'react-router-dom';

const BottomNav = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const checkLoginStatus = async () => {
      try {
        const response = await apiFetch('/api/users', {
          method: 'GET',
          credentials: 'include',
          headers: {
            'accept': '*/*'
          }
        });

        if (response.ok) {
          const userData = await response.json();
          if (userData && userData.id) {
            setIsLoggedIn(true);
          } else {
            setIsLoggedIn(false);
          }
        } else {
          setIsLoggedIn(false);
        }
      } catch (error) {
        setIsLoggedIn(false);
      }
    };

    checkLoginStatus();
  }, []);

  const handleNavigation = async (path, requiresLogin = false) => {
    if (requiresLogin) {
      try {
        const response = await apiFetch('/api/users', {
          method: 'GET',
          credentials: 'include',
          headers: {
            'accept': '*/*'
          }
        });

        if (response.ok) {
          const userData = await response.json();
          if (userData) {
            navigate(path);
          } else {
            navigate('/login');
          }
        } else {
          navigate('/login');
        }
      } catch (error) {
        navigate('/login');
      }
    } else {
      navigate(path);
    }
  };

  const isActive = (path) => {
    return location.pathname === path;
  };

  const navItems = [
    {
      icon: faBars,
      label: "전체식단",
      path: "/alldiet",
      requiresLogin: false
    },
    {
      icon: faCartPlus,
      label: "주문하기",
      path: "/dietselection",
      requiresLogin: true
    },
    {
      icon: faHouse,
      label: "홈",
      path: "/",
      requiresLogin: false
    },
    {
      icon: faBowlFood,
      label: "내 식단",
      path: "/weeklyfoodmenu",
      requiresLogin: true
    },
    {
      icon: faUser,
      label: "내 정보",
      path: "/mypage",
      requiresLogin: true
    }
  ];

  return (
    <nav className="bottom-nav-bottom-nav">
      <div className="bottom-nav-bottom-nav-container">
        {navItems.map((item, index) => (
          <button
            key={index}
            className={`bottom-nav-nav-item ${isActive(item.path) ? 'bottom-nav-active' : ''}`}
            onClick={() => handleNavigation(item.path, item.requiresLogin)}
            aria-label={item.label}
          >
            <div className="bottom-nav-nav-icon">
              <FontAwesomeIcon icon={item.icon} />
            </div>
            <span className="bottom-nav-nav-label">{item.label}</span>
          </button>
        ))}
      </div>
    </nav>
  );
};

export default BottomNav;
