import http from './index';

export type EnvironmentName = 'DEV' | 'STAGING' | 'PROD' | 'UNKNOWN';

export interface SiteSettingsDto {
  siteName: string;
  defaultEnvironment: EnvironmentName;
  refreshCron: string;
  maxToolsPerService: number;
  ssoEnabled: boolean;
  auditRetentionDays: number;
  demoMode: boolean;
  updatedBy?: string;
  updatedAt: string;
}

export interface UpdateSiteSettingsRequest {
  siteName?: string;
  defaultEnvironment?: EnvironmentName;
  refreshCron?: string;
  maxToolsPerService?: number;
  ssoEnabled?: boolean;
  auditRetentionDays?: number;
  demoMode?: boolean;
}

export const settingsApi = {
  async get(): Promise<SiteSettingsDto> {
    const { data } = await http.get<SiteSettingsDto>('/settings');
    return data;
  },
  async update(req: UpdateSiteSettingsRequest): Promise<SiteSettingsDto> {
    const { data } = await http.put<SiteSettingsDto>('/settings', req);
    return data;
  }
};
