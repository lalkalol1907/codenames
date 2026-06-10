import { defineStore } from 'pinia';
import { ref } from 'vue';

export interface DiscordUser {
  id: string;
  username: string;
  globalName: string | null;
  avatarUrl: string | null;
}

export const useDiscordStore = defineStore('discord', () => {
  const isDiscord = ref(false);
  const initialized = ref(false);
  const appToken = ref<string | null>(null);
  const discordAccessToken = ref<string | null>(null);
  const user = ref<DiscordUser | null>(null);
  const instanceId = ref<string | null>(null);
  const channelId = ref<string | null>(null);
  const error = ref<string | null>(null);

  function setBootstrapResult(result: {
    appToken: string;
    discordAccessToken: string;
    instanceId: string;
    channelId: string | null;
  }) {
    appToken.value = result.appToken;
    discordAccessToken.value = result.discordAccessToken;
    instanceId.value = result.instanceId;
    channelId.value = result.channelId;
    initialized.value = true;
  }

  function setUser(u: DiscordUser) {
    user.value = u;
  }

  return {
    isDiscord,
    initialized,
    appToken,
    discordAccessToken,
    user,
    instanceId,
    channelId,
    error,
    setBootstrapResult,
    setUser,
  };
});
