import axios, { type AxiosInstance } from 'axios';
import { ElMessageBox } from 'element-plus';

const TOKEN_KEY = 'mcpg.apiToken';

export function getApiToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? '';
}

export function setApiToken(token: string): void {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY);
  } else {
    localStorage.setItem(TOKEN_KEY, token.trim());
  }
}

export async function ensureApiToken(): Promise<void> {
  try {
    const { data } = await axios.get<{ securityEnabled: boolean }>('/api/system/auth-config', {
      timeout: 8000
    });
    if (!data.securityEnabled) return;
  } catch {
    // If probe fails, still try to collect a token so subsequent calls can proceed.
  }
  if (getApiToken()) return;
  const { value } = await ElMessageBox.prompt(
    'This gateway requires an API token (Authorization: Bearer). '
      + 'Read it from ./data/api-token.txt or the MCPG_API_TOKEN env var.',
    'API token required',
    {
      confirmButtonText: 'Save',
      cancelButtonText: 'Skip',
      inputPlaceholder: 'paste token',
      inputType: 'password',
      distinguishCancelAndClose: true
    }
  ).catch(() => ({ value: '' }));
  if (value) setApiToken(String(value));
}

/**
 * Axios instance preconfigured for the MCPG backend.
 *
 * In development the dev server proxies `/api` to the Spring Boot backend.
 * In production the same-origin path is used because the SPA is served by
 * the same jar.
 */
const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = getApiToken();
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
    config.headers['X-MCPG-Token'] = token;
  }
  return config;
});

http.interceptors.response.use(
  (res) => res,
  async (err) => {
    const status = err?.response?.status;
    if (status === 401) {
      setApiToken('');
      try {
        await ensureApiToken();
      } catch {
        /* ignore */
      }
    }
    const message = err?.response?.data?.message ?? err.message ?? 'Network error';
    return Promise.reject(new Error(message));
  }
);

export interface SystemInfo {
  name: string;
  version: string;
  timestamp: string;
  status: string;
}

export const systemApi = {
  info: async (): Promise<SystemInfo> => {
    const { data } = await http.get<SystemInfo>('/system/info');
    return data;
  }
};

export default http;
