import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

describe('umami analytics', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_UMAMI_WEBSITE_ID', '');
    vi.stubEnv('VITE_UMAMI_SCRIPT_URL', '');
    delete window.umami;
    document.head.querySelectorAll('script[data-website-id]').forEach((node) => node.remove());
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  async function loadUmami() {
    return import('./umami');
  }

  it('no-ops when env is not configured', async () => {
    const { trackEvent, trackPageView } = await loadUmami();
    const track = vi.fn();
    window.umami = { track };

    trackEvent('room_created');
    trackPageView('/rooms/ABCD');

    expect(track).not.toHaveBeenCalled();
  });

  it('tracks events when configured and umami is available', async () => {
    vi.stubEnv('VITE_UMAMI_WEBSITE_ID', 'test-website-id');
    vi.stubEnv('VITE_UMAMI_SCRIPT_URL', 'https://analytics.example.com/script.js');

    const { trackEvent, trackPageView } = await loadUmami();
    const track = vi.fn();
    window.umami = { track };

    trackEvent('game_started', { players: 4 });
    expect(track).toHaveBeenCalledWith('game_started', { players: 4 });

    trackPageView('/rooms/ABCD/game');
    expect(track).toHaveBeenCalledWith(expect.any(Function));
  });
});
