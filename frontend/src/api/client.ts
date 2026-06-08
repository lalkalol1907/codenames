import type { ErrorResponse } from '@/types/models';

let csrfToken: string | null = null;

function readCookie(name: string): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

export async function ensureCsrf(): Promise<string> {
  const existing = readCookie('csrf_token') ?? csrfToken;
  if (existing) {
    csrfToken = existing;
    return existing;
  }
  const res = await fetch('/api/csrf', { credentials: 'include' });
  if (!res.ok) throw new Error('Failed to fetch CSRF token');
  const data = (await res.json()) as { token: string };
  csrfToken = data.token;
  return data.token;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.method && init.method !== 'GET' && init.method !== 'HEAD') {
    const token = await ensureCsrf();
    headers.set('X-CSRF-Token', token);
  }
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const res = await fetch(path, {
    ...init,
    headers,
    credentials: 'include',
  });

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;

  if (!res.ok) {
    const err = data as ErrorResponse | null;
    throw new ApiError(err?.error ?? res.statusText, res.status);
  }

  return data as T;
}

export const api = {
  getI18n: (locale: string) =>
    apiFetch<Record<string, string>>(`/api/i18n?locale=${encodeURIComponent(locale)}`),
  setLocale: (locale: string) =>
    apiFetch<void>('/api/locale', {
      method: 'POST',
      body: JSON.stringify({ locale }),
    }),
  createRoom: (name: string, language: string) =>
    apiFetch<import('@/types/models').RoomActionResponse>('/api/rooms', {
      method: 'POST',
      body: JSON.stringify({ name, language }),
    }),
  joinRoom: (code: string, name: string) =>
    apiFetch<import('@/types/models').RoomActionResponse>('/api/rooms/join', {
      method: 'POST',
      body: JSON.stringify({ code, name }),
    }),
  getRoom: (code: string) =>
    apiFetch<import('@/types/models').RoomBootstrapDto>(`/api/rooms/${encodeURIComponent(code)}`),
  joinRoomAt: (code: string, name: string) =>
    apiFetch<import('@/types/models').RoomActionResponse>(
      `/api/rooms/${encodeURIComponent(code)}/join`,
      {
        method: 'POST',
        body: JSON.stringify({ name }),
      },
    ),
  getRoomOptions: (code: string) =>
    apiFetch<import('@/types/models').RoomOptionsDto>(
      `/api/rooms/${encodeURIComponent(code)}/options`,
    ),
  setRole: (code: string, team: string, role: string) =>
    apiFetch<void>(`/api/rooms/${encodeURIComponent(code)}/role`, {
      method: 'POST',
      body: JSON.stringify({ team, role }),
    }),
  randomizeTeams: (code: string) =>
    apiFetch<void>(`/api/rooms/${encodeURIComponent(code)}/randomize`, {
      method: 'POST',
    }),
  startGame: (code: string) =>
    apiFetch<void>(`/api/rooms/${encodeURIComponent(code)}/start`, {
      method: 'POST',
    }),
};
