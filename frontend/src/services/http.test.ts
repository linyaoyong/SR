import { describe, expect, it } from 'vitest';
import { formatHttpError, unwrapApiResponse } from './http';

describe('unwrapApiResponse', () => {
  it('returns data when code is zero', () => {
    expect(unwrapApiResponse({ code: 0, message: 'success', data: { id: 1 } })).toEqual({ id: 1 });
  });

  it('throws readable backend message when code is non-zero', () => {
    expect(() => unwrapApiResponse({ code: 40401, message: '钱包余额不足', data: null })).toThrow('钱包余额不足');
  });

  it('throws fallback message when backend message is empty', () => {
    expect(() => unwrapApiResponse({ code: 500, message: '', data: null })).toThrow('请求失败');
  });
});

describe('formatHttpError', () => {
  it('uses friendly Chinese message for gateway 500 without backend business message', () => {
    const error = {
      response: { status: 500, data: 'Request failed with status code 500' },
      message: 'Request failed with status code 500',
    };

    expect(formatHttpError(error)).toBe('服务暂时不可用，请稍后再试');
  });

  it('uses friendly Chinese message when backend is unreachable', () => {
    const error = { code: 'ERR_NETWORK', message: 'Network Error' };

    expect(formatHttpError(error)).toBe('网络异常，请稍后再试');
  });
});
