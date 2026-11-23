function requiredUrl(name, value) {
  if (!value) throw new Error(`${name} 환경변수를 설정해주세요.`);
  return value.replace(/\/+$/, '');
}

const apiBaseUrl = requiredUrl('REACT_APP_API_BASE_URL', process.env.REACT_APP_API_BASE_URL);
const appBaseUrl = requiredUrl('REACT_APP_APP_BASE_URL', process.env.REACT_APP_APP_BASE_URL);
const wsUrl = requiredUrl('REACT_APP_WS_URL', process.env.REACT_APP_WS_URL);

export const apiUrl = (path) => `${apiBaseUrl}${path.startsWith('/') ? '' : '/'}${path}`;
export const appUrl = (path) => `${appBaseUrl}${path.startsWith('/') ? '' : '/'}${path}`;

// 사용자는 STOMP CONNECT의 Access Token으로 서버가 정한다. 연결 주소에 이메일을 넣지 않는다 (#10).
export const notificationSocketUrl = () => wsUrl;
