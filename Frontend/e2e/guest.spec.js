const { test, expect } = require('@playwright/test');

// 비로그인 화면 검사: 실제 서버·메일·결제 API에는 접속하지 않는다.
let apiRequests;
let unexpectedRequests;
let pageErrors;
test.beforeEach(async ({ page }) => {
  apiRequests = [];
  unexpectedRequests = [];
  pageErrors = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  await page.route('**/*', async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.origin === 'http://127.0.0.1:3001') return route.continue();
    if (url.origin === 'http://127.0.0.1:8080') {
      apiRequests.push({ path: url.pathname, method: request.method() });
      // 예상한 조회만 대체한다. 새로운 API는 명시적으로 fixture를 추가한다.
      if (request.method() === 'GET' && url.pathname === '/api/users') {
        return route.fulfill({ status: 401, json: { message: '비로그인 테스트' } });
      }
      // 페이지를 열면 Refresh 쿠키로 세션 복원을 시도한다. 비로그인이면 쿠키가 없어 401이다.
      if (request.method() === 'POST' && url.pathname === '/api/auth/refresh') {
        return route.fulfill({ status: 401, json: { code: 'REFRESH_TOKEN_NOT_FOUND' } });
      }
      unexpectedRequests.push(`${request.method()} ${url.pathname}`);
      return route.abort();
    }
    // 공개 결제 SDK는 로드하지 않는다. 그 외 외부 접속은 회귀로 보고한다.
    if (url.href !== 'https://cdn.iamport.kr/v1/iamport.js') {
      unexpectedRequests.push(`${url.origin}${url.pathname}`);
    }
    return route.abort();
  });
});

const isRefresh = request => request.method === 'POST' && request.path === '/api/auth/refresh';

test.afterEach(() => {
  expect(unexpectedRequests, '예상하지 않은 API 또는 외부 접속').toEqual([]);
  expect(pageErrors, '브라우저 실행 오류').toEqual([]);
  // 여러 화면이 동시에 API를 불러도 세션 복원은 페이지당 한 번이다.
  expect(apiRequests.filter(isRefresh).length, '세션 복원 요청 수').toBeLessThanOrEqual(1);
});

test('비로그인 홈에서 주문하면 로그인 화면으로 이동한다', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: '오늘의 추천 식단' })).toBeVisible();
  await page.getByRole('button', { name: '주문하기', exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByPlaceholder('아이디', { exact: true })).toBeVisible();
  await expect(page.getByPlaceholder('비밀번호', { exact: true })).toBeVisible();
});

test('빈 로그인은 경고를 표시하고 로그인 API를 호출하지 않는다', async ({ page }) => {
  await page.goto('/login');
  const dialogMessage = new Promise(resolve => page.once('dialog', async dialog => {
    resolve(dialog.message());
    await dialog.accept();
  }));
  await page.getByRole('button', { name: '로그인', exact: true }).click();
  expect(await dialogMessage).toBe('아이디와 비밀번호를 모두 입력해주세요.');
  expect(apiRequests.some(request => request.path === '/api/auth/login')).toBe(false);
});

test('가입 초기 화면은 이메일 확인과 가입 버튼을 비활성화한다', async ({ page }) => {
  await page.goto('/signup');
  await expect(page.getByLabel('이메일', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '중복확인', exact: true })).toBeDisabled();
  await expect(page.getByRole('button', { name: '회원가입', exact: true })).toBeDisabled();
  expect(apiRequests.some(request => request.method === 'POST' && !isRefresh(request))).toBe(false);
});
