import { DiscordSDK } from '@discord/embedded-app-sdk';
import type { DiscordBootstrapResponse, RoomViewDto } from '@/types/models';
import { useDiscordStore } from '@/stores/discord';

const CLIENT_ID = import.meta.env.VITE_DISCORD_CLIENT_ID as string;

let _sdk: DiscordSDK | null = null;

export function getDiscordSdk(): DiscordSDK | null {
  return _sdk;
}

export function detectDiscord(): boolean {
  if (typeof window === 'undefined') return false;
  const params = new URLSearchParams(window.location.search);
  return params.has('frame_id') || window.location.hostname.endsWith('.discordsays.com');
}

export interface DiscordInitResult {
  roomCode: string;
  view: RoomViewDto;
}

export async function initDiscord(): Promise<DiscordInitResult> {
  const store = useDiscordStore();

  if (!CLIENT_ID) {
    throw new Error('VITE_DISCORD_CLIENT_ID is not set');
  }

  const sdk = new DiscordSDK(CLIENT_ID);
  _sdk = sdk;
  await sdk.ready();

  // OAuth2 authorization – Discord intercepts and returns a code
  const { code } = await sdk.commands.authorize({
    client_id: CLIENT_ID,
    response_type: 'code',
    state: '',
    prompt: 'none',
    scope: ['identify'],
  });

  // Exchange code for our app token + Discord access token via backend
  const res = await fetch('/api/discord/bootstrap', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      code,
      instanceId: sdk.instanceId,
      channelId: sdk.channelId ?? '',
    }),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error((body as { error?: string }).error ?? 'Discord bootstrap failed');
  }

  const data = (await res.json()) as DiscordBootstrapResponse;

  store.setBootstrapResult({
    appToken: data.appToken,
    discordAccessToken: data.discordAccessToken,
    instanceId: sdk.instanceId,
    channelId: sdk.channelId ?? null,
  });

  try {
    const auth = await sdk.commands.authenticate({ access_token: data.discordAccessToken });
    if (auth?.user) {
      store.setUser({
        id: auth.user.id,
        username: auth.user.username,
        globalName: (auth.user as unknown as { global_name?: string }).global_name ?? null,
        avatarUrl: auth.user.avatar
          ? `https://cdn.discordapp.com/avatars/${auth.user.id}/${auth.user.avatar}.webp?size=64`
          : null,
      });
    }
  } catch {
    // authenticate is best-effort; the game still works without it
  }

  return { roomCode: data.roomCode, view: data.view };
}
