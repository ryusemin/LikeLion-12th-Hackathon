function requiredUrl(name, value) {
  if (!value) throw new Error(`${name} 환경변수를 설정해주세요.`);
  return value.replace(/\/+$/, '');
}

const apiBaseUrl = requiredUrl('REACT_APP_API_BASE_URL', process.env.REACT_APP_API_BASE_URL);
const appBaseUrl = requiredUrl('REACT_APP_APP_BASE_URL', process.env.REACT_APP_APP_BASE_URL);
const wsUrl = requiredUrl('REACT_APP_WS_URL', process.env.REACT_APP_WS_URL);

export const apiUrl = (path) => `${apiBaseUrl}${path.startsWith('/') ? '' : '/'}${path}`;
export const appUrl = (path) => `${appBaseUrl}${path.startsWith('/') ? '' : '/'}${path}`;

export function notificationSocketUrl(email) {
  const url = new URL(wsUrl);
  url.searchParams.set('userEmail', email);
  return url.toString();
}
