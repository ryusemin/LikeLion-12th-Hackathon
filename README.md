<h1 align="center">우리동네영양사</h1>

<p align="center">
  <img width="200px;" src="Frontend/src/images/logo.png" alt="로고"/>
</p>

## 1. 프로젝트 개요

### 진행기간
- 개발 기간: 2024년 7월 ~ 2024년 8월  
- 리펙토링 기간: 2025년 6월 ~ 2025년 7월

### 서비스 소개
우리 동네 영양사는 바쁜 현대인들을 위한 건강한 도시락 정기 구독 서비스이며, AI 영양사가 추천하는 균형잡힌 영양식단을 받아볼 수 있습니다.

### 핵심 가치

- **시간 절약**: 식단 계획, 쇼핑, 요리 시간을 절약하여 바쁜 일상 속에서도 건강한 식사 가능
- **개인 맞춤화**: AI 기반 영양 분석을 통한 개인별 건강 목표에 최적화된 식단 제공

### 타겟 사용자

- **직장인**: 불규칙한 식사와 외식으로 영양 불균형을 겪는 바쁜 직장인
- **1인 가구**: 혼자 요리하기 어려워 간편식에 의존하는 1인 가구
- **다이어터**: 체계적인 식단 관리가 필요한 다이어트 중인 사람들

### 차별화 포인트

- **AI 기술 활용**: 단순 메뉴 제공이 아닌 고루 균형잡힌 영양 식단 최적화
- **편의성**: 완성된 도시락을 자택으로 배송하여 최대 편의성 제공
- **지속가능성**: 개인 피드백을 통한 지속적인 서비스 개선

## 2. 서비스 아키텍처


  ![Image](https://github.com/user-attachments/assets/62c5a478-6494-41aa-bbc0-b4c1c8a2a7ee)


## 3. 팀 소개

| **프로필** | ![종현](https://avatars.githubusercontent.com/u/127838675?v=4) | ![세민](https://avatars.githubusercontent.com/u/113248843?v=4) | ![동균](https://avatars.githubusercontent.com/u/107734276?v=4) | ![동섭](https://avatars.githubusercontent.com/u/105368619?v=40559e2f4-9356-4df9-b373-a06030bc0abb) | ![경락](https://avatars.githubusercontent.com/u/81566899?v=4) |
| :-: | :-: | :-: | :-: | :-: | :-: |
| **이름** | **[이종현](https://github.com/2-jjong)** | **[류세민](https://github.com/ryusemin)** | **[한동균](https://github.com/hdg5639)** | **[서동섭](https://github.com/dongsubnambuk)** | **[손경락](https://github.com/ganglike248)** |
| **역할** | PM, Backend | Backend | Backend | Frontend |Frontend |

## 4. 기술 스택

###  Frontend
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![Font Awesome](https://img.shields.io/badge/Font%20Awesome-339AF0?style=for-the-badge&logo=font-awesome&logoColor=white)
![SockJS](https://img.shields.io/badge/SockJS-010101?style=for-the-badge&logo=spring&logoColor=white)
![STOMP](https://img.shields.io/badge/STOMP-6DB33F?style=for-the-badge&logo=websocket&logoColor=white)

###  Backend
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)
![WebSocket](https://img.shields.io/badge/WebSocket-333333?style=for-the-badge&logo=websocket&logoColor=white)
![JPA](https://img.shields.io/badge/JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white)

###  Database
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)

### DevOps & Deployment
![AWS](https://img.shields.io/badge/Amazon_AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white)
![EC2](https://img.shields.io/badge/Amazon_EC2-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=github-actions&logoColor=white)
![Route 53](https://img.shields.io/badge/Route_53-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white)

### External APIs
![OpenAI](https://img.shields.io/badge/OpenAI_API-412991?style=for-the-badge&logo=openai&logoColor=white)
![Iamport](https://img.shields.io/badge/Iamport-0084FF?style=for-the-badge&logoColor=white)


## 5. 실행 방법

### 요구사항

- Java 17
- Spring Boot: 3.5.16
- Node.js: 24.x LTS (`Frontend/.nvmrc` 기준)
- npm: 11.x
- Docker Compose: 로컬 MySQL·Redis 실행 시 사용

### 설치 및 실행

환경변수 준비부터 로컬 DB·서버 실행, Windows·Linux 검증 명령은 [로컬 개발 환경 안내](docs/development.md)를 참고하세요.

- 백엔드: `Backend/nimn.env.example`을 참고해 로컬 설정과 JWT 키를 준비합니다.
- 프론트엔드: Node 24.x·npm 11.x를 선택한 뒤 `Frontend/`에서 `npm ci`로 설치합니다.
- 현재 프론트에는 운영 API 주소와 이전 인증 코드가 남아 있습니다. 전체 로컬 연동은 별도 확인이 필요합니다.
- JDK 17·Node 24 기준 설치·테스트·빌드 결과와 범위는 [검증 기록](docs/verification.md)을 참고하세요.
- PR CI와 Codex의 백엔드·프론트·전체 검증 버튼 설정은 [자동 검증 안내](docs/ci-and-actions.md)를 참고하세요.

## 6. 주요 기능

### 6.1. 메인페이지 및 전체 식단 소개

<table style="margin: 0 auto;">
  <tr>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/ee0341a7-8aa1-4e41-b792-fff6408c4c56" alt="메인페이지" /></td>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/715ea513-062a-40a7-894c-e6ac4a611484" alt="상단 네비게이션" /></td>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/7428b858-da32-40bc-bde4-4ef7b5a1da4f" alt="식단 리뷰 조회" /></td>
  </tr>
</table>

**핵심 기능:**  
- **접근성**: 사용자들의 진입 접근성을 고려해 웹사이트에서도 모바일 화면으로 볼 수 있게 구현  
- **상단바**: 사용자들이 알림 또는 식단 리뷰를 쉽게 확인할 수 있는 진입점과 알림이 몇 개 왔는지 확인 가능  
- **식단별 리뷰**: 전체 식단에서 각 식단별 리뷰 확인 가능  

---

### 6.2. 구독 설정 기능

<table style="margin: 0 auto;">
  <tr>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/4287ae27-ef39-4d61-b9c1-529a4b32475f" /></td>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/eecc9ea9-7be7-4492-90e5-c52bf999678b" /></td>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/2301bab9-275d-40b7-9aa8-77e8000de3da" /></td>
  </tr>
</table>

**핵심 기능:**  
- **주 단위 구독**: 한 주 단위로 유연한 구독 설정  
- **식사 선택**: 아침, 점심, 저녁 중 원하는 식사만 선택 가능 및 대체 식단 선택 가능  

---

### 6.3. 생성형AI 맞춤 식단 생성

<div align="center">
<table style="margin: 0 auto;">
  <tr><td><img width="700" height="600" src="https://github.com/user-attachments/assets/6376af80-010c-4d1a-997b-6c8a9a386e6d" alt="AI 개인맞춤 추천" /></td></tr>
  <tr><td><img width="700" height="600" src="https://github.com/user-attachments/assets/b7c0e9b0-ac87-4666-918d-d75bb4c35e1b" alt="AI식단2" /></td></tr>
  <tr><td><img width="700" height="600" src="https://github.com/user-attachments/assets/d8234e5a-6cad-4859-b00d-645e65514f3a" alt="가격대별 메뉴" /></td></tr>
</table>
</div>

**핵심 기능:**  
- **개인 맞춤 추천**: 사용자의 식단 리뷰를 통한 피드백을 반영한 OpenAI 기반 식단 생성  
- **영양 균형 관리**: 칼로리, 탄수화물, 단백질, 지방 등 영양성분을 표시하여 한눈에 영양 성분 확인 가능  
- **3가지 가격대**: 4,000원, 5,500원, 7,000원 메뉴로 구성으로 각 금액에 맞는 다양한 식단 생성  

---

### 6.4. 결제 및 알림 시스템

<table style="margin: 0 auto;">
  <tr>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/07054986-6252-42d5-85e9-3897ce31becc" alt="다양한 결제 수단" /></td>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/659856ab-7e55-42a3-9162-7a1663afd62b" alt="실시간 식단 알림" /></td>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/b54c9208-949d-4905-885f-4ae9c8745361" alt="리뷰 요청 알림" /></td>
  </tr>
</table>

**핵심 기능:**  
- **아임포트 결제**: 포트원(아임포트)의 API를 연동하여 카카오페이, 네이버페이, 신용카드 등 다양한 결제 플랫폼 제공  
- **실시간 알림**: WebSocket 기반으로 알림을 전송(09시, 12시, 18시)하여 오늘의 식단을 안내 받을 수 있음. 또한 구독기간 중 매일 21시에 식단 리뷰 알림이 전송되어 금일 식단에 대한 리뷰 요청 알림 제공  

---

### 6.5. 리뷰 및 평가 시스템

<div align="center">
<table style="margin: 0 auto;">
  <tr>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/dadd0e3c-7ee0-4b92-b92a-40eb4abace54" alt="별점 평가" /></td>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/277817be-2690-4b26-b6b7-6092f0ff11dd" alt="상세 리뷰 작성" /></td>
  </tr>
</table>
</div>

**핵심 기능:**  
- **별점 평가 및 상세 리뷰**: 사용자 피드백 기반 맞춤형 식단 개선을 위해 5점 만점 별점 평가와 상세 후기를 작성할 수 있는 직관적인 리뷰 시스템 제공  

---

### 6.6. 쿠키 및 이메일 인증 시스템

<table style="margin: 0 auto;">
  <tr>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/19c61132-4e03-4d8b-9bca-c2f501367860" alt="로그인 화면" /></td>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/5e13dee9-d550-4b0f-b848-a2c36f52c4c1" alt="회원가입" /></td>
    <td><img width="300" height="780" src="https://github.com/user-attachments/assets/d904bec2-4aa7-4e9e-ada4-9b0710ebf8bd" alt="이메일 인증" /></td>
  </tr>
</table>

**핵심 기능:**  
- **JWT 쿠키 기반 인증**: 클라이언트 측에서 토큰을 직접 관리할 필요가 없는 쿠키를 활용하여 JavaScript를 통한 토큰 탈취 방지  
- **이메일 인증 시스템**: 실제 사용 가능한 이메일인지 확인하며, 추후 비밀번호 분실 시 안전한 계정 복구를 돕기 위한 이메일 인증 시스템 도입  

---

<p align="center">
  <strong> 건강한 식단, 편리한 생활을 위한 우리동네영양사를 경험해보세요!</strong>
</p>
