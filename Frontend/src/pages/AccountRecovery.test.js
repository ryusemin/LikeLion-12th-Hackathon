import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AccountRecovery from './AccountRecovery';

const json = (status, body) => Promise.resolve({ ok: status < 400, status, json: () => Promise.resolve(body) });

const bodyOf = (path) => {
  const call = global.fetch.mock.calls.find(([url]) => url.includes(path));
  return call && JSON.parse(call[1].body);
};

const verifyEmail = async () => {
  fireEvent.change(screen.getByPlaceholderText('이메일을 입력하세요'), { target: { value: 'member@example.invalid' } });
  fireEvent.click(screen.getByRole('button', { name: '이메일 확인' }));
  fireEvent.click(await screen.findByRole('button', { name: '비밀번호 변경하기' }));
  fireEvent.click(screen.getByRole('button', { name: '인증번호 발송' }));
  fireEvent.change(await screen.findByPlaceholderText('인증번호를 입력하세요'), { target: { value: '123456' } });
  fireEvent.click(screen.getByRole('button', { name: '인증번호 확인' }));
  await screen.findByPlaceholderText('새 비밀번호를 입력하세요');
};

const submitNewPassword = () => {
  fireEvent.change(screen.getByPlaceholderText('새 비밀번호를 입력하세요'), { target: { value: 'new-password' } });
  fireEvent.change(screen.getByPlaceholderText('새 비밀번호를 다시 입력하세요'), { target: { value: 'new-password' } });
  fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }));
};

let resetResponse;

beforeEach(() => {
  resetResponse = () => json(200, { email: 'member@example.invalid', password: null });
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/users/isExist')) return json(200, true);
    if (url.includes('/api/email/send')) return json(200, { success: true });
    if (url.includes('/api/email/check')) return json(200, { success: true, verificationToken: 'proof-token' });
    if (url.includes('/api/users/reset-password')) return resetResponse();
    return json(404, {});
  });
});

afterEach(() => {
  delete global.fetch;
});

test('비밀번호 재설정은 인증 목적과 서버가 발급한 증명 토큰을 함께 보낸다', async () => {
  render(<MemoryRouter><AccountRecovery /></MemoryRouter>);

  await verifyEmail();
  submitNewPassword();

  await screen.findByText('비밀번호가 성공적으로 변경되었습니다.');
  expect(bodyOf('/api/email/send')).toEqual({ email: 'member@example.invalid', purpose: 'PASSWORD_RESET' });
  expect(bodyOf('/api/email/check')).toMatchObject({ purpose: 'PASSWORD_RESET', code: 123456 });
  expect(bodyOf('/api/users/reset-password')).toEqual({
    email: 'member@example.invalid',
    newPassword: 'new-password',
    emailVerificationToken: 'proof-token',
  });
});

test('재설정이 실패하면 소비된 증명 대신 이메일 인증을 다시 받게 한다', async () => {
  resetResponse = () => json(403, { code: 'EMAIL_VERIFICATION_REQUIRED', message: '이메일 인증이 필요합니다.' });
  render(<MemoryRouter><AccountRecovery /></MemoryRouter>);

  await verifyEmail();
  submitNewPassword();

  expect(await screen.findByText('이메일 인증이 필요합니다. 이메일 인증을 다시 진행해주세요.')).toBeInTheDocument();
  await waitFor(() => expect(screen.getByRole('button', { name: '인증번호 발송' })).toBeEnabled());
  expect(screen.queryByPlaceholderText('새 비밀번호를 입력하세요')).not.toBeInTheDocument();
});
