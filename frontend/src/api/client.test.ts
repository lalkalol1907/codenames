import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, api } from '@/api/client';

describe('api client', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
    document.cookie = 'csrf_token=test-csrf';
  });

  it('throws ApiError with server message', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      text: async () => JSON.stringify({ error: 'Room not found' }),
    } as Response);

    await expect(api.getRoom('ABCD')).rejects.toEqual(
      expect.objectContaining({
        name: 'ApiError',
        message: 'Room not found',
        status: 404,
      }),
    );
  });

  it('sends CSRF header on POST requests', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      status: 204,
      text: async () => '',
    } as Response);

    await api.setLocale('ru');

    expect(fetch).toHaveBeenCalledWith(
      '/api/locale',
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        headers: expect.any(Headers),
      }),
    );

    const init = vi.mocked(fetch).mock.calls[0][1] as RequestInit;
    expect((init.headers as Headers).get('X-CSRF-Token')).toBe('test-csrf');
  });

  it('returns parsed JSON on success', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: async () => JSON.stringify({ code: 'ABCD', viewerId: 'player-1' }),
    } as Response);

    await expect(api.createRoom('Alice', 'en')).resolves.toEqual({
      code: 'ABCD',
      viewerId: 'player-1',
    });
  });

  it('ApiError preserves status code', () => {
    const error = new ApiError('Forbidden', 403);
    expect(error).toBeInstanceOf(Error);
    expect(error.status).toBe(403);
  });
});
