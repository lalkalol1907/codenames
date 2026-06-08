import type { RouteRecordRaw } from 'vue-router';
import HomeView from '@/views/HomeView.vue';
import RoomView from '@/views/RoomView.vue';
import GameView from '@/views/GameView.vue';

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
  },
  {
    path: '/rooms/:code',
    name: 'room',
    component: RoomView,
    props: true,
  },
  {
    path: '/rooms/:code/game',
    name: 'game',
    component: GameView,
    props: true,
  },
];
