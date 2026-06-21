import http from './index';
import type { Environment } from './services';

export type RegistryStatus = 'UNKNOWN' | 'OK' | 'ERROR';

export interface RegistryDto {
  id: number;
  name: string;
  displayName?: string;
  type: string;
  endpoint: string;
  username?: string;
  environment: Environment;
  namespace?: string;
  groupName?: string;
  extra?: string;
  enabled: boolean;
  status: RegistryStatus;
  lastError?: string;
  lastServiceCount?: number;
  lastSyncedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RegistryTypeDto {
  type: string;
  label: string;
  implemented: boolean;
}

export interface CreateRegistryRequest {
  name: string;
  displayName?: string;
  type: string;
  endpoint: string;
  username?: string;
  password?: string;
  environment: Environment;
  namespace?: string;
  groupName?: string;
  extra?: string;
  enabled?: boolean;
}

export interface UpdateRegistryRequest {
  displayName?: string;
  endpoint?: string;
  username?: string;
  /** `null` keeps current; empty string clears the stored password. */
  password?: string;
  environment?: Environment;
  namespace?: string;
  groupName?: string;
  extra?: string;
  enabled?: boolean;
}

export interface TestConnectionResult {
  ok: boolean;
  message?: string;
  elapsedMs: number;
}

export interface DiscoveredServiceOutcome {
  name: string;
  baseUrl?: string;
  status: 'IMPORTED' | 'UPDATED' | 'SKIPPED';
  message?: string;
}

export interface DiscoveryResultDto {
  registryId: number;
  registryName: string;
  discovered: number;
  imported: number;
  skipped: number;
  items: DiscoveredServiceOutcome[];
}

export const registriesApi = {
  async list(): Promise<RegistryDto[]> {
    const { data } = await http.get<RegistryDto[]>('/registries');
    return data;
  },
  async types(): Promise<RegistryTypeDto[]> {
    const { data } = await http.get<RegistryTypeDto[]>('/registries/types');
    return data;
  },
  async get(id: number): Promise<RegistryDto> {
    const { data } = await http.get<RegistryDto>(`/registries/${id}`);
    return data;
  },
  async create(req: CreateRegistryRequest): Promise<RegistryDto> {
    const { data } = await http.post<RegistryDto>('/registries', req);
    return data;
  },
  async update(id: number, req: UpdateRegistryRequest): Promise<RegistryDto> {
    const { data } = await http.put<RegistryDto>(`/registries/${id}`, req);
    return data;
  },
  async remove(id: number): Promise<void> {
    await http.delete(`/registries/${id}`);
  },
  async test(id: number): Promise<TestConnectionResult> {
    const { data } = await http.post<TestConnectionResult>(`/registries/${id}/test`, {}, {
      timeout: 30000
    });
    return data;
  },
  async discover(id: number): Promise<DiscoveryResultDto> {
    const { data } = await http.post<DiscoveryResultDto>(`/registries/${id}/discover`, {}, {
      timeout: 120000
    });
    return data;
  }
};
