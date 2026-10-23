import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';

import App from './App.vue';
import router from './router';
import { i18n } from './i18n';
import { ensureApiToken } from './api';
import './styles/global.css';

async function bootstrap() {
  await ensureApiToken().catch(() => undefined);
  const app = createApp(App);
  app.use(createPinia());
  app.use(router);
  app.use(i18n);
  app.use(ElementPlus);
  app.mount('#app');
}

void bootstrap();
