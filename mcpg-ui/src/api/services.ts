import http from './index';

export type Environment = 'DEV' | 'STAGING' | 'PROD' | 'UNKNOWN';

export interface ServiceDto {
  id: number;
  name: string;
  displayName?: string;
  environment: Environment;
  sourceType: string;
  sourceRef?: string;
  baseUrl: string;
  specUrl?: string;
  status: 'ACTIVE' | 'ERROR' | 'DISABLED';
  lastError?: string;
  toolCount: number;
  createdAt: string;
  updatedAt: string;
  lastSyncedAt?: string;
}

export interface ImportSwaggerRequest {
  name: string;
  displayName?: string;
  url: string;
  baseUrl?: string;
  environment: Environment;
}

export interface UpdateServiceRequest {
  displayName?: string;
  baseUrl?: string;
  environment?: Environment;
}

export interface ImportResultDto {
  service: ServiceDto;
  toolCount: number;
  added: number;
  updated: number;
  removed: number;
  specVersion: string;
}

export const servicesApi = {
  async list(params: { keyword?: string; environment?: Environment } = {}): Promise<ServiceDto[]> {
    const { data } = await http.get<ServiceDto[]>('/services', { params });
    return data;
  },
  async get(id: number): Promise<ServiceDto> {
    const { data } = await http.get<ServiceDto>(`/services/${id}`);
    return data;
  },
  async importSwagger(req: ImportSwaggerRequest): Promise<ImportResultDto> {
    const { data } = await http.post<ImportResultDto>('/services/import-swagger', req, {
      timeout: 60000
    });
    return data;
  },
  async update(id: number, req: UpdateServiceRequest): Promise<ServiceDto> {
    const { data } = await http.put<ServiceDto>(`/services/${id}`, req);
    return data;
  },
  async refresh(id: number): Promise<ImportResultDto> {
    const { data } = await http.post<ImportResultDto>(`/services/${id}/refresh`, {}, {
      timeout: 60000
    });
    return data;
  },
  async remove(id: number): Promise<void> {
    await http.delete(`/services/${id}`);
  }
};
