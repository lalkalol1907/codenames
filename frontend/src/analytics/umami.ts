declare global {
  interface Window {
    umami?: {
      track: (
        event: string | ((props: Record<string, unknown>) => Record<string, unknown>),
        data?: Record<string, string | number | boolean>,
      ) => void;
    };
  }
}

let scriptLoaded = false;
const pending: Array<() => void> = [];

function getConfig() {
  return {
    websiteId: import.meta.env.VITE_UMAMI_WEBSITE_ID?.trim(),
    scriptUrl: import.meta.env.VITE_UMAMI_SCRIPT_URL?.trim(),
  };
}

function isEnabled(): boolean {
  const { websiteId, scriptUrl } = getConfig();
  return Boolean(websiteId && scriptUrl && !import.meta.env.SSR);
}

function flushPending(): void {
  if (!window.umami) return;
  for (const action of pending.splice(0)) {
    action();
  }
}

function runWhenReady(action: () => void): void {
  if (!isEnabled()) return;
  if (window.umami) {
    action();
    return;
  }
  pending.push(action);
}

export function initUmami(): void {
  const { websiteId, scriptUrl } = getConfig();
  if (!isEnabled() || typeof document === 'undefined') return;
  if (document.querySelector(`script[data-website-id="${websiteId}"]`)) {
    scriptLoaded = true;
    flushPending();
    return;
  }

  const script = document.createElement('script');
  script.defer = true;
  script.src = scriptUrl!;
  script.setAttribute('data-website-id', websiteId!);
  script.onload = () => {
    scriptLoaded = true;
    flushPending();
  };
  document.head.appendChild(script);
}

export function trackPageView(path: string): void {
  runWhenReady(() => {
    window.umami?.track((props) => ({ ...props, url: path }));
  });
}

export function trackEvent(name: string, data?: Record<string, string | number | boolean>): void {
  runWhenReady(() => {
    window.umami?.track(name, data);
  });
}

export function umamiConfigured(): boolean {
  return isEnabled();
}
