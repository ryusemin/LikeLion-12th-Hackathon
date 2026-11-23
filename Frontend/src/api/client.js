import { apiUrl } from '../config/endpoints';

// Access Token은 이 모듈의 변수에만 둔다. 브라우저 저장소에 남기지 않으며,
// 새로고침하면 HttpOnly Refresh 쿠키로 /api/auth/refresh를 호출해 다시 받는다 (#4).
let accessToken = null;
let restorePromise = null;
let refreshPromise = null;
// login·logout마다 바뀐다. 그 전에 시작한 재발급 결과가 늦게 도착하면 버린다.
let session = 0;
// 마지막 재발급이 일시 장애(5xx·네트워크)로 실패했는지. 로그인 세션이 없는 것(401 등)과 구분한다.
let refreshFailedTransiently = false;

function refreshAccessToken() {
  // 서버가 refresh마다 Refresh Token을 교체하므로, 동시에 여러 번 호출하면 재사용으로 판단돼 세션이 끊긴다.
  if (!refreshPromise) {
    const startedSession = session;
    let transient = false;
    const promise = fetch(apiUrl('/api/auth/refresh'), { method: 'POST', credentials: 'include' })
      .then(async (response) => {
        transient = response.status >= 500;
        return response.ok ? (await response.json()).accessToken || null : null;
      })
      .catch(() => {
        transient = true;
        return null;
      })
      .then((token) => {
        if (startedSession === session) {
          accessToken = token;
          refreshFailedTransiently = !token && transient;
        }
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
  refreshFailedTransiently = false;
}

// 알림 WebSocket은 SockJS라 연결 요청에 Authorization 헤더를 붙일 수 없다. STOMP CONNECT 헤더로 Access Token을 보낸다 (#10).
// stompjs의 beforeConnect에서 연결·재연결마다 호출한다. WebSocket은 401로 재발급할 수 없으므로 만료가 가까우면 미리 재발급한다.
// 재발급이 일시 장애로 실패하면 기다렸다가 다시 시도하고, 로그인 세션이 없을 때만 연결을 멈춘다.
export async function authorizeNotificationSocket(client, retryDelayMs = 5000) {
  await ensureSession();
  while (client.active) {
    const needsRefresh = accessToken ? expiresWithin(accessToken, 30_000) : refreshFailedTransiently;
    const token = needsRefresh ? await refreshAccessToken() : accessToken;
    if (token) {
      client.connectHeaders = { Authorization: `Bearer ${token}` };
      return;
    }
    if (!refreshFailedTransiently) {
      // 로그인 세션이 없으면 거부될 연결을 반복하지 않는다.
      await client.deactivate();
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, retryDelayMs));
  }
}

// JWT의 payload는 서명 없이도 읽을 수 있는 공개 정보다. 화면 분기에만 쓰고, 권한 판단은 서버가 한다.
function payloadOf(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(payload));
  } catch {
    return {};
  }
}

function roleOf(token) {
  return payloadOf(token).role || null;
}

// 재발급 시점을 정하는 데만 쓴다. 만료 여부의 판단은 서버가 한다.
function expiresWithin(token, milliseconds) {
  const { exp } = payloadOf(token);
  return typeof exp === 'number' && exp * 1000 - Date.now() < milliseconds;
}
