import http from './index';

export type AuditOutcome = 'SUCCESS' | 'FAILURE';

export interface AuditEventDto {
  id: number;
  timestamp: string;
  actor: string;
  action: string;
  resourceType: string;
  resourceId: string;
  outcome: AuditOutcome;
  httpStatus?: number;
  durationMs?: number;
  clientIp?: string;
  userAgent?: string;
  detail?: string;
}

export interface AuditPageDto {
  items: AuditEventDto[];
  total: number;
  page: number;
  size: number;
}

export interface AuditQuery {
  outcome?: AuditOutcome | '';
  keyword?: string;
  page?: number;
  size?: number;
}

export const auditApi = {
  async list(query: AuditQuery = {}): Promise<AuditPageDto> {
    const { data } = await http.get<AuditPageDto>('/audit/events', { params: query });
    return data;
  }
};
