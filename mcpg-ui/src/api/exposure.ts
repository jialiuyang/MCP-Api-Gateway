import http from './index';

/**
 * Gateway-wide MCP exposure strategy.
 *
 * - META: only the four meta tools are advertised.
 * - HYBRID: meta tools plus operator-promoted operations (default).
 * - DIRECT_ALL: every non-deprecated tool is advertised; meta tools are dropped.
 */
export type ExposureMode = 'META' | 'HYBRID' | 'DIRECT_ALL';

export interface ExposureSettingsDto {
  mode: ExposureMode;
  note?: string;
  updatedBy?: string;
  updatedAt: string;
  totalTools: number;
  promotedTools: number;
  metaToolCount: number;
  effectiveCount: number;
}

export interface UpdateExposureRequest {
  mode: ExposureMode;
  note?: string;
}

export const exposureApi = {
  async get(): Promise<ExposureSettingsDto> {
    const { data } = await http.get<ExposureSettingsDto>('/exposure');
    return data;
  },
  async update(req: UpdateExposureRequest): Promise<ExposureSettingsDto> {
    const { data } = await http.put<ExposureSettingsDto>('/exposure', req);
    return data;
  }
};
