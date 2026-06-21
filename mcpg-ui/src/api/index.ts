import axios, { type AxiosInstance } from 'axios';

/**
 * Axios instance preconfigured for the MCPG backend.
 *
 * In development the dev server proxies `/api` to the Spring Boot backend on
 * port 8080 (see vite.config.ts). In production the same-origin path is used
 * because the SPA is served by the same jar.
 */
const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
});

http.interceptors.response.use(
  (res) => res,
  (err) => {
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
