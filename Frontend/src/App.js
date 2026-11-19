import { apiUrl } from './config/endpoints';
import React, { useEffect, useState } from 'react';
import { BrowserRouter as Router, Route, Routes, useLocation } from "react-router-dom";
import './App.css';
import Layout from './components/Layout';
import Login from './pages/Login';
import Signup from './pages/Signup';
import AllDiet from './pages/AllDietPage';
import DietInfo from "./pages/DietInfoPage";
import MainPage from './pages/MainPage';
import MyPage from './pages/MyPage';
import UserInfoUpdate from './pages/UserInfoUpdate';
import OrderList from './pages/OrderList';
import WeeklyFoodMenu from './pages/WeeklyFoodMenu';
import Survey from './pages/Survey';
import Notification from './pages/Notification';
import DietSelection from './pages/DietSelectionPage';
import DietPaymentMain from './pages/DietPaymentMainPage';
import DietPayment from './pages/DietPaymentPage';
import Admin from './pages/Admin';
import DietPaymentComplete from './pages/DietPaymentComplete';
import AccountRecovery from './pages/AccountRecovery';
import PasswordChange from './pages/PasswordChange';

const AppContent = () => {
  const location = useLocation();
  const isAdminPage = location.pathname === '/admin';
  

  const [notificationCount, setNotificationCount] = useState(0);
  const [surveyCount, setSurveyCount] = useState(0);


  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userInfo, setUserInfo] = useState(null);


  const handleNotificationCountChange = (notifCount, survCount) => {
    setNotificationCount(notifCount);
    setSurveyCount(survCount);
  };


  const checkAuthStatus = async () => {
    try {
      const response = await fetch(apiUrl('/api/users'), {
        method: 'GET',
        credentials: 'include'
      });

      if (response.ok) {
        const userData = await response.json();
        setIsLoggedIn(true);
        setUserInfo(userData);
      } else {
        setIsLoggedIn(false);
        setUserInfo(null);
      }
    } catch (error) {
      setIsLoggedIn(false);
      setUserInfo(null);
    }
  };


  const handleLoginSuccess = (userData) => {
    setIsLoggedIn(true);
    setUserInfo(userData);
  };


  const handleLogoutSuccess = () => {
    setIsLoggedIn(false);
    setUserInfo(null);
  };

  useEffect(() => {
    checkAuthStatus();
  }, []);

  useEffect(() => {
    const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
    let unloadHandled = false;

    const handleBeforeUnload = (event) => {

      if (unloadHandled) {
        localStorage.clear();
      } else {

        event.preventDefault();
        event.returnValue = ''; 
      }
    };

    const handleVisibilityChange = () => {

      if (document.visibilityState === 'hidden') {
        localStorage.clear();
      }
    };

    const handleUnload = () => {
      unloadHandled = true;
      localStorage.clear();
    };

    if (isSafari) {

      document.addEventListener('visibilitychange', handleVisibilityChange);
    } else {

      window.addEventListener('beforeunload', handleBeforeUnload);
      window.addEventListener('unload', handleUnload);
    }

    return () => {
      if (isSafari) {
        document.removeEventListener('visibilitychange', handleVisibilityChange);
      } else {
        window.removeEventListener('beforeunload', handleBeforeUnload);
        window.removeEventListener('unload', handleUnload);
      }
    };
  }, []);

  return (
    <div className={isAdminPage ? "admin-container" : "mobile-container"}>
      {isAdminPage ? (
        <Routes>
          <Route path="/admin" element={<Admin />} />
        </Routes>
      ) : (

        <Layout 
          notificationCount={notificationCount} 
          surveyCount={surveyCount}
        >
          <Routes>

            <Route 
              path="/" 
              element={
                <MainPage 
                  onNotificationCountChange={handleNotificationCountChange} 
                />
              } 
            />
            <Route path="/login" element={<Login onLoginSuccess={handleLoginSuccess} />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/alldiet" element={<AllDiet />} />
            <Route path="/dietinfo" element={<DietInfo />} />
            <Route path="/mypage" element={<MyPage onLogoutSuccess={handleLogoutSuccess} />} />
            <Route path="/userinfoupdate" element={<UserInfoUpdate />} />
            <Route path="/orderlist" element={<OrderList />} />
            <Route path="/weeklyfoodmenu" element={<WeeklyFoodMenu />} />
            <Route path="/survey" element={<Survey />} />
            <Route path="/notification" element={<Notification />} />
            <Route path="/dietselection" element={<DietSelection />} />
            <Route path="/dietpaymentmain" element={<DietPaymentMain />} />
            <Route path="/dietpayment" element={<DietPayment />} />
            <Route path="/dietpaymentcomplete" element={<DietPaymentComplete />} />
            <Route path="/account-recovery" element={<AccountRecovery />} />
            <Route path="/password-change" element={<PasswordChange />} />
          </Routes>
        </Layout>
      )}
    </div>
  );
};

const App = () => {
  return (
    <Router>
      <AppContent />
    </Router>
  );
};

export default App;
