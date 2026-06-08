import { createPinia } from 'pinia';
import { ViteSSG } from 'vite-ssg';
import { createHead } from '@unhead/vue/client';
import { initUmami, trackPageView } from '@/analytics/umami';
import App from './App.vue';
import { routes } from './router';
import { useLocaleStore } from './stores/locale';
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
  }
});
