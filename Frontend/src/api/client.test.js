const json = (status, body = {}) => Promise.resolve({ ok: status < 400, status, json: () => Promise.resolve(body) });
const jwt = (payload) => `h.${btoa(JSON.stringify(payload))}.s`;

let client;
let calls;

const authOf = (path) => calls.filter(([url]) => url.endsWith(path)).map(([, init]) => init.headers.get('Authorization'));
const countOf = (path) => calls.filter(([url]) => url.endsWith(path)).length;

beforeEach(() => {
  jest.resetModules();
  client = require('./client');
  calls = [];
});

afterEach(() => {
  delete global.fetch;
});

const mockServer = (routes) => {
  global.fetch = jest.fn((url, init = {}) => {
    calls.push([url, { ...init, headers: new Headers(init.headers) }]);
    const path = new URL(url).pathname;
    const handler = routes[path];
    return handler ? handler(init) : json(404);
  });
};

test('로그인 응답의 토큰을 메모리에 두고 이후 요청에 Bearer로 붙인다', async () => {
  const token = jwt({ role: 'ROLE_ADMIN' });
  mockServer({
    '/api/auth/login': () => json(200, { accessToken: token }),
    '/api/users': () => json(200, { name: 'u' }),
  });

  expect(await client.login('a@t.invalid', 'pw')).toEqual({ ok: true, role: 'ROLE_ADMIN' });
  await client.apiFetch('/api/users');

  expect(authOf('/api/users')).toEqual([`Bearer ${token}`]);
  expect(countOf('/api/auth/refresh')).toBe(0);
  expect(window.localStorage.length).toBe(0);
  expect(window.sessionStorage.length).toBe(0);
});

test('새로고침 뒤 첫 요청 전에 refresh로 세션을 한 번만 복원한다', async () => {
  mockServer({
    '/api/auth/refresh': () => json(200, { accessToken: 'restored' }),
    '/api/users': () => json(200),
    '/api/foods/plans': () => json(200),
  });

  await Promise.all([client.apiFetch('/api/users'), client.apiFetch('/api/foods/plans')]);
  await client.apiFetch('/api/users');

  expect(countOf('/api/auth/refresh')).toBe(1);
  expect(authOf('/api/users')).toEqual(['Bearer restored', 'Bearer restored']);
});

test('비로그인이면 복원 실패 후 토큰 없이 요청한다', async () => {
  mockServer({
    '/api/auth/refresh': () => json(401, { code: 'REFRESH_TOKEN_NOT_FOUND' }),
    '/api/foods/plans': () => json(200),
  });

  const response = await client.apiFetch('/api/foods/plans');

  expect(response.status).toBe(200);
  expect(authOf('/api/foods/plans')).toEqual([null]);
});

test('동시 401은 재발급을 한 번으로 합치고 각 요청을 한 번씩만 재시도한다', async () => {
  let refreshes = 0;
  mockServer({
    '/api/auth/refresh': () => json(200, { accessToken: refreshes++ === 0 ? 'old' : 'new' }),
    '/api/users': (init) => json(new Headers(init.headers).get('Authorization') === 'Bearer new' ? 200 : 401),
  });

  const responses = await Promise.all([client.apiFetch('/api/users'), client.apiFetch('/api/users'), client.apiFetch('/api/users')]);

  expect(responses.map((r) => r.status)).toEqual([200, 200, 200]);
  expect(countOf('/api/auth/refresh')).toBe(2); // 시작 시 복원 1번 + 401 재발급 1번
  expect(countOf('/api/users')).toBe(6);
});

test('재발급도 실패하면 토큰을 비우고 비로그인 요청으로 한 번 다시 보낸다', async () => {
  let refreshes = 0;
  mockServer({
    '/api/auth/refresh': () => (refreshes++ === 0 ? json(200, { accessToken: 'expired' }) : json(401)),
    '/api/foods/plans': (init) => json(new Headers(init.headers).has('Authorization') ? 401 : 200),
  });

  expect((await client.apiFetch('/api/foods/plans')).status).toBe(200);
  await client.apiFetch('/api/foods/plans');
  expect(authOf('/api/foods/plans')).toEqual(['Bearer expired', null, null]);
});

const deferred = () => {
  let resolve;
  const promise = new Promise((r) => { resolve = r; });
  return { promise, resolve };
};

test('로그인보다 늦게 끝난 세션 복원은 로그인 토큰을 덮어쓰지 않는다', async () => {
  const slowRefresh = deferred();
  mockServer({
    '/api/auth/refresh': () => slowRefresh.promise,
    '/api/auth/login': () => json(200, { accessToken: 'logged-in' }),
    '/api/users': () => json(200),
  });

  const pending = client.apiFetch('/api/users');
  await client.login('a@t.invalid', 'pw');
  slowRefresh.resolve(await json(401));
  await pending;
  await client.apiFetch('/api/users');

  expect(authOf('/api/users')).toEqual(['Bearer logged-in', 'Bearer logged-in']);
});

test('로그아웃보다 늦게 끝난 재발급은 토큰을 되살리지 않는다', async () => {
  const slowRefresh = deferred();
  mockServer({
    '/api/auth/login': () => json(200, { accessToken: 'expired' }),
    '/api/auth/refresh': () => slowRefresh.promise,
    '/api/auth/logout': () => json(200),
    '/api/users': (init) => json(new Headers(init.headers).has('Authorization') ? 401 : 500),
  });

  await client.login('a@t.invalid', 'pw');
  const pending = client.apiFetch('/api/users');
  await new Promise((r) => setTimeout(r, 0)); // 401을 받고 재발급을 시작할 때까지 진행
  await client.logout();
  slowRefresh.resolve(await json(200, { accessToken: 'revived' }));
  await pending;
  await client.apiFetch('/api/users');

  expect(authOf('/api/users')).toEqual(['Bearer expired', null, null]);
});

test('로그아웃하면 서버에 알리고 토큰을 비우며 다시 복원하지 않는다', async () => {
  mockServer({
    '/api/auth/login': () => json(200, { accessToken: jwt({ role: 'ROLE_USER' }) }),
    '/api/auth/logout': () => json(200),
    '/api/users': () => json(200),
  });

  await client.login('a@t.invalid', 'pw');
  await client.logout();
  await client.apiFetch('/api/users');

  expect(countOf('/api/auth/logout')).toBe(1);
  expect(authOf('/api/users')).toEqual([null]);
  expect(countOf('/api/auth/refresh')).toBe(0);
});

test('로그인 실패는 서버 메시지를 돌려주고 토큰을 남기지 않는다', async () => {
  mockServer({
    '/api/auth/login': () => json(401, { isSuccess: '실패', message: '자격 증명에 실패하였습니다.' }),
    '/api/auth/refresh': () => json(401),
    '/api/users': () => json(200),
  });

  expect(await client.login('a@t.invalid', 'bad')).toEqual({ ok: false, message: '자격 증명에 실패하였습니다.' });
  await client.apiFetch('/api/users');
  expect(authOf('/api/users')).toEqual([null]);
});

describe('알림 WebSocket 인증 (#10)', () => {
  const socketClient = () => {
    const stomp = { active: true, connectHeaders: {} };
    stomp.deactivate = jest.fn(() => {
      stomp.active = false;
      return Promise.resolve();
    });
    return stomp;
  };
  const inSeconds = (seconds) => Math.floor(Date.now() / 1000) + seconds;

  test('CONNECT 헤더에 현재 Access Token만 넣고 이메일은 보내지 않는다', async () => {
    const token = jwt({ email: 'a@t.invalid', role: 'ROLE_USER', exp: inSeconds(3600) });
    mockServer({ '/api/auth/login': () => json(200, { accessToken: token }) });
    await client.login('a@t.invalid', 'pw');
    const stomp = socketClient();

    await client.authorizeNotificationSocket(stomp);

    expect(stomp.connectHeaders).toEqual({ Authorization: `Bearer ${token}` });
    expect(stomp.deactivate).not.toHaveBeenCalled();
    expect(countOf('/api/auth/refresh')).toBe(0);
  });

  test('재연결 때 토큰 만료가 가까우면 먼저 재발급한 토큰으로 연결한다', async () => {
    const expiring = jwt({ role: 'ROLE_USER', exp: inSeconds(5) });
    const renewed = jwt({ role: 'ROLE_USER', exp: inSeconds(3600) });
    mockServer({
      '/api/auth/login': () => json(200, { accessToken: expiring }),
      '/api/auth/refresh': () => json(200, { accessToken: renewed }),
    });
    await client.login('a@t.invalid', 'pw');
    const stomp = socketClient();

    await client.authorizeNotificationSocket(stomp);

    expect(countOf('/api/auth/refresh')).toBe(1);
    expect(stomp.connectHeaders).toEqual({ Authorization: `Bearer ${renewed}` });
  });

  test('로그인 세션이 없으면 연결을 멈추고 거부될 CONNECT를 보내지 않는다', async () => {
    mockServer({ '/api/auth/refresh': () => json(401) });
    const stomp = socketClient();

    await client.authorizeNotificationSocket(stomp);

    expect(stomp.deactivate).toHaveBeenCalledTimes(1);
    expect(stomp.connectHeaders).toEqual({});
  });

  test('재발급이 일시적으로 실패하면(503·네트워크) 연결을 멈추지 않고 다시 시도한다', async () => {
    const token = jwt({ role: 'ROLE_USER', exp: inSeconds(3600) });
    let refreshes = 0;
    mockServer({
      '/api/auth/refresh': () => {
        refreshes += 1;
        if (refreshes === 1) return json(503);
        if (refreshes === 2) return Promise.reject(new TypeError('Failed to fetch'));
        return json(200, { accessToken: token });
      },
    });
    const stomp = socketClient();

    await client.authorizeNotificationSocket(stomp, 0);

    expect(countOf('/api/auth/refresh')).toBe(3);
    expect(stomp.deactivate).not.toHaveBeenCalled();
    expect(stomp.connectHeaders).toEqual({ Authorization: `Bearer ${token}` });
  });

  test('재시도를 기다리는 동안 화면을 떠나 연결이 멈추면 더 시도하지 않는다', async () => {
    mockServer({ '/api/auth/refresh': () => json(503) });
    const stomp = socketClient();
    stomp.active = false;

    await client.authorizeNotificationSocket(stomp, 0);

    expect(countOf('/api/auth/refresh')).toBe(1); // 페이지 첫 세션 복원뿐
    expect(stomp.connectHeaders).toEqual({});
  });
});
