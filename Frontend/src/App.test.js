import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from './App';
import Login from './pages/Login';

beforeEach(() => {
  window.history.replaceState({}, '', '/');
  global.fetch = jest.fn().mockResolvedValue({ ok: false, status: 401 });
});

afterEach(() => {
  delete global.fetch;
  jest.restoreAllMocks();
});

test('비로그인 사용자에게 추천 식단과 공통 탐색을 표시한다', async () => {
  render(<App />);

  expect(await screen.findByRole('heading', { name: '오늘의 추천 식단' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: '주문하기' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: '내 식단' })).toBeInTheDocument();
});

test('비로그인 사용자가 주문하기를 누르면 로그인 화면으로 이동한다', async () => {
  render(<App />);
  await screen.findByRole('heading', { name: '오늘의 추천 식단' });

  fireEvent.click(screen.getByRole('button', { name: '주문하기' }));

  expect(await screen.findByPlaceholderText('아이디')).toBeInTheDocument();
  expect(screen.getByPlaceholderText('비밀번호')).toBeInTheDocument();
  expect(window.location.pathname).toBe('/login');
});

test('로그인 입력값이 비어 있으면 로그인 API를 호출하지 않는다', async () => {
  window.history.replaceState({}, '', '/login');
  const alert = jest.spyOn(window, 'alert').mockImplementation(() => {});
  render(<MemoryRouter><Login /></MemoryRouter>);

  fireEvent.click(screen.getByRole('button', { name: '로그인' }));

  expect(alert).toHaveBeenCalledWith('아이디와 비밀번호를 모두 입력해주세요.');
  expect(global.fetch).not.toHaveBeenCalledWith(
    expect.stringContaining('/login'),
    expect.anything()
  );
});
