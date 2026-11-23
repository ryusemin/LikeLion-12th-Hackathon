const originalEnvironment = { ...process.env };

afterEach(() => {
  process.env = { ...originalEnvironment };
  jest.resetModules();
});

test.each([
  ['local', 'http://127.0.0.1:8080', 'http://127.0.0.1:3000'],
  ['production', 'https://nimn.store', 'https://nimn.store'],
])('%s 환경의 API·이미지·WebSocket·결제 복귀 주소를 사용한다', (_, apiBase, appBase) => {
  process.env.REACT_APP_API_BASE_URL = `${apiBase}/`;
  process.env.REACT_APP_WS_URL = `${apiBase}/ws/notification`;
  process.env.REACT_APP_APP_BASE_URL = `${appBase}/`;
  const { apiUrl, appUrl, notificationSocketUrl } = require('./endpoints');

  expect(apiUrl('/api/users')).toBe(`${apiBase}/api/users`);
  expect(apiUrl('/api/image/42')).toBe(`${apiBase}/api/image/42`);
  expect(appUrl('/dietpaymentcomplete')).toBe(`${appBase}/dietpaymentcomplete`);
  expect(notificationSocketUrl()).toBe(`${apiBase}/ws/notification`);
});

test('API 주소가 없으면 다른 서버로 조용히 요청하지 않고 설정 오류를 알린다', () => {
  delete process.env.REACT_APP_API_BASE_URL;

  expect(() => require('./endpoints')).toThrow('REACT_APP_API_BASE_URL');
});
