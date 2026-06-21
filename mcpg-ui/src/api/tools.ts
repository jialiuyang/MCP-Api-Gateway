import http from './index';

export type RiskLevel = 'READ' | 'WRITE_LOW' | 'WRITE_HIGH' | 'FORBIDDEN';

export interface ToolDto {
  id: number;
  serviceId: number;
  toolName: string;
  operationId: string;
  httpMethod: string;
  path: string;
  summary?: string;
  description?: string;
  tags: string[];
  inputSchema?: unknown;
  outputSchema?: unknown;
  riskLevel: RiskLevel;
  promoted: boolean;
  deprecated: boolean;
  updatedAt: string;
}

export const toolsApi = {
  async list(params: { keyword?: string; serviceId?: number } = {}): Promise<ToolDto[]> {
    const { data } = await http.get<ToolDto[]>('/tools', { params });
    return data;
  },
  async get(id: number): Promise<ToolDto> {
    const { data } = await http.get<ToolDto>(`/tools/${id}`);
    return data;
  },
  async promote(id: number, promoted: boolean): Promise<ToolDto> {
    const { data } = await http.post<ToolDto>(`/tools/${id}/promote`, { promoted });
    return data;
  }
};
