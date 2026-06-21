import http from './index';

export interface ToolHealthDto {
  toolId: number;
  toolName: string;
  serviceName: string;
  callsLast24h: number;
  successRate: number;
  p50LatencyMs: number;
  p95LatencyMs: number;
  p99LatencyMs: number;
  lastInvokedAt: string;
  lastError?: string;
}

export interface LatencyBucket {
  range: string;
  count: number;
}

export interface TimeSeriesPoint {
  hour: string;
  success: number;
  failure: number;
}

export interface HealthOverviewDto {
  totalTools: number;
  activeTools: number;
  callsLast24h: number;
  globalSuccessRate: number;
  avgLatencyMs: number;
  latencyHistogram: LatencyBucket[];
  callVolume24h: TimeSeriesPoint[];
  topTools: ToolHealthDto[];
}

export const healthApi = {
  async overview(): Promise<HealthOverviewDto> {
    const { data } = await http.get<HealthOverviewDto>('/tool-health/overview');
    return data;
  }
};
