import { apiUrl } from '../config/endpoints';

// Access Token은 이 모듈의 변수에만 둔다. 브라우저 저장소에 남기지 않으며,
// 새로고침하면 HttpOnly Refresh 쿠키로 /api/auth/refresh를 호출해 다시 받는다 (#4).
let accessToken = null;
let restorePromise = null;
let refreshPromise = null;
// login·logout마다 바뀐다. 그 전에 시작한 재발급 결과가 늦게 도착하면 버린다.
let session = 0;

function refreshAccessToken() {
  // 서버가 refresh마다 Refresh Token을 교체하므로, 동시에 여러 번 호출하면 재사용으로 판단돼 세션이 끊긴다.
  if (!refreshPromise) {
    const startedSession = session;
    const promise = fetch(apiUrl('/api/auth/refresh'), { method: 'POST', credentials: 'include' })
      .then(async (response) => (response.ok ? (await response.json()).accessToken || null : null))
      .catch(() => null)
      .then((token) => {
        if (startedSession === session) accessToken = token;
        return accessToken;
      })
      .finally(() => {
        if (refreshPromise === promise) refreshPromise = null;
      });
    refreshPromise = promise;
  }
  return refreshPromise;
}

// 페이지를 연 뒤 첫 요청 전에 한 번만 세션 복원을 시도한다.
function ensureSession() {
  if (!restorePromise) restorePromise = accessToken ? Promise.resolve() : refreshAccessToken();
  return restorePromise;
}

function send(path, options, token) {
  const headers = new Headers(options.headers);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  return fetch(apiUrl(path), { ...options, headers, credentials: 'include' });
}

export async function apiFetch(path, options = {}) {
  await ensureSession();
  const sentToken = accessToken;
  const response = await send(path, options, sentToken);
  if (response.status !== 401 || !sentToken) return response;

  // 다른 요청이 이미 재발급했으면 그 토큰을 쓰고, 아니면 재발급한다. 재시도는 한 번뿐이다.
  // 재발급에 실패하면 비로그인 요청으로 다시 보낸다. 공개 API는 손님 응답을 받는다.
  const nextToken = accessToken !== sentToken ? accessToken : await refreshAccessToken();
  return send(path, options, nextToken);
}

export async function login(email, password) {
  const response = await fetch(apiUrl('/api/auth/login'), {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok || !body.accessToken) return { ok: false, message: body.message };

  startSession(body.accessToken);
  return { ok: true, role: roleOf(accessToken) };
}

export async function logout() {
  try {
    await fetch(apiUrl('/api/auth/logout'), { method: 'POST', credentials: 'include' });
  } finally {
    startSession(null);
  }
}

function startSession(token) {
  session += 1;
  accessToken = token;
  restorePromise = Promise.resolve();
  refreshPromise = null;
}

// JWT의 payload는 서명 없이도 읽을 수 있는 공개 정보다. 화면 분기에만 쓰고, 권한 판단은 서버가 한다.
function roleOf(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(payload)).role || null;
  } catch {
    return null;
  }
}
