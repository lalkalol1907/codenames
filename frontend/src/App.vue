<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import LocaleSwitcher from '@/components/LocaleSwitcher.vue';
import ThemeSwitcher from '@/components/ThemeSwitcher.vue';
import { useLocaleStore } from '@/stores/locale';

const localeStore = useLocaleStore();
const route = useRoute();
const isWide = computed(() => route.name === 'game');
const settingsOpen = ref(false);
const settingsFabRef = ref<HTMLElement | null>(null);

function toggleSettings() {
  settingsOpen.value = !settingsOpen.value;
}

function closeSettings() {
  settingsOpen.value = false;
}

function onDocumentPointerDown(event: PointerEvent) {
  if (!settingsOpen.value || !settingsFabRef.value) return;
  if (!settingsFabRef.value.contains(event.target as Node)) {
    closeSettings();
  }
}

function onDocumentKeyDown(event: KeyboardEvent) {
  if (event.key === 'Escape' && settingsOpen.value) closeSettings();
}

watch(isWide, (wide) => {
  if (!wide) closeSettings();
});

onMounted(() => {
  document.addEventListener('pointerdown', onDocumentPointerDown);
  document.addEventListener('keydown', onDocumentKeyDown);
});

onUnmounted(() => {
  document.removeEventListener('pointerdown', onDocumentPointerDown);
  document.removeEventListener('keydown', onDocumentKeyDown);
});
</script>

<template>
  <div class="bg-glow" aria-hidden="true" />
  <div class="app-shell">
    <header v-if="!isWide" class="site-header">
      <RouterLink to="/" class="logo">{{ localeStore.t('app.title') }}</RouterLink>
      <div class="header-controls">
        <ThemeSwitcher />
        <form class="locale-form" @submit.prevent>
          <LocaleSwitcher />
        </form>
      </div>
    </header>
    <main class="container" :class="{ 'container--wide': isWide }">
      <RouterView />
    </main>
  </div>

  <Teleport to="body">
    <div v-if="isWide" ref="settingsFabRef" class="app-settings-fab">
      <Transition name="app-settings-panel">
        <div
          v-if="settingsOpen"
          class="app-settings-panel"
          role="dialog"
          :aria-label="localeStore.t('settings.menu')"
        >
          <RouterLink to="/" class="logo app-settings-panel__logo" @click="closeSettings">
            {{ localeStore.t('app.title') }}
          </RouterLink>
          <div class="app-settings-panel__controls">
            <ThemeSwitcher />
            <form class="locale-form" @submit.prevent>
              <LocaleSwitcher />
            </form>
          </div>
        </div>
      </Transition>
      <button
        type="button"
        class="app-settings-fab__btn"
        :aria-expanded="settingsOpen"
        :aria-label="
          settingsOpen ? localeStore.t('settings.close_menu') : localeStore.t('settings.open_menu')
        "
        @click="toggleSettings"
      >
        <svg viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
          <path
            fill-rule="evenodd"
            d="M7.84 1.804A1 1 0 018.82 1h2.36a1 1 0 01.98.804l.17 1.003c.06.36.297.698.645.867.39.19.754.41 1.08.658.31.236.72.296 1.08.164l.925-.382a1 1 0 011.17.363l1.18 2.042a1 1 0 01-.232 1.27l-.75.687a.97.97 0 00-.28.778c.008.13.008.26 0 .39a.97.97 0 00.28.778l.75.687a1 1 0 01.232 1.27l-1.18 2.042a1 1 0 01-1.17.363l-.925-.382a1.01 1.01 0 00-1.08.164 6.9 6.9 0 01-1.08.658.97.97 0 00-.645.867l-.17 1.003A1 1 0 018.82 19H6.46a1 1 0 01-.98-.804l-.17-1.003a.97.97 0 00-.645-.867 6.9 6.9 0 01-1.08-.658 1.01 1.01 0 00-1.08-.164l-.925.382a1 1 0 01-1.17-.363l-1.18-2.042a1 1 0 01.232-1.27l.75-.687a.97.97 0 00.28-.778 5.5 5.5 0 000-.39.97.97 0 00-.28-.778l-.75-.687a1 1 0 01-.232-1.27l1.18-2.042a1 1 0 011.17-.363l.925.382c.36.132.77.072 1.08-.164.326-.248.69-.468 1.08-.658a.97.97 0 00.645-.867l.17-1.003zM10 13a3 3 0 100-6 3 3 0 000 6z"
            clip-rule="evenodd"
          />
        </svg>
      </button>
    </div>
  </Teleport>
</template>
