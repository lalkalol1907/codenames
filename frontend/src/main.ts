import { createPinia } from 'pinia';
import { ViteSSG } from 'vite-ssg';
import { createHead } from '@unhead/vue/client';
import App from './App.vue';
import { routes } from './router';
import { useLocaleStore } from './stores/locale';
import './assets/styles.css';

export const createApp = ViteSSG(App, { routes }, ({ app }) => {
  const pinia = createPinia();
  app.use(pinia);
  app.use(createHead());

  const localeStore = useLocaleStore(pinia);
  void localeStore.loadMessages();
});
