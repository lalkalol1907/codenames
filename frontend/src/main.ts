import { createPinia } from 'pinia';
import { ViteSSG } from 'vite-ssg';
import { createHead } from '@unhead/vue/client';
import { initUmami, trackPageView } from '@/analytics/umami';
import App from './App.vue';
import { routes } from './router';
import { useLocaleStore } from './stores/locale';
import { useDiscordStore } from './stores/discord';
import { useRoomStore } from './stores/room';
import { detectDiscord, initDiscord } from './composables/useDiscord';
import './assets/styles.css';

export const createApp = ViteSSG(App, { routes }, ({ app, router, isClient }) => {
  const pinia = createPinia();
  app.use(pinia);
  app.use(createHead());

  const localeStore = useLocaleStore(pinia);
  void localeStore.loadMessages();

  if (isClient) {
    initUmami();
    router.afterEach((to) => {
      trackPageView(to.fullPath);
    });

    const discordStore = useDiscordStore(pinia);
    const roomStore = useRoomStore(pinia);

    if (detectDiscord()) {
      discordStore.isDiscord = true;

      // Run Discord SDK init after the router is ready, then redirect to the room
      router.isReady().then(async () => {
        try {
          const { roomCode, view } = await initDiscord();
          roomStore.setView(view, roomCode);
          roomStore.language =
            view.status !== 'LOBBY' ? roomStore.language || 'en' : roomStore.language || 'en';

          const target =
            view.status === 'PLAYING' || view.status === 'FINISHED'
              ? `/rooms/${roomCode}/game`
              : `/rooms/${roomCode}`;

          await router.replace(target);
        } catch (err) {
          discordStore.error = err instanceof Error ? err.message : 'Discord init failed';
        }
      });
    }
  }
});
