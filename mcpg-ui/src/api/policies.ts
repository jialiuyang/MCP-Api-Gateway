import http from './index';

export type PolicySeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface PolicyDto {
  id: number;
  policyKey: string;
  name: string;
  description?: string;
  category: string;
  severity: PolicySeverity;
  enabled: boolean;
  configJson?: string;
  updatedBy?: string;
  updatedAt: string;
}

export interface UpdatePolicyRequest {
  enabled?: boolean;
  severity?: PolicySeverity;
  configJson?: string;
  note?: string;
}

export const policiesApi = {
  async list(): Promise<PolicyDto[]> {
    const { data } = await http.get<PolicyDto[]>('/policies');
    return data;
  },
  async update(id: number, req: UpdatePolicyRequest): Promise<PolicyDto> {
    const { data } = await http.put<PolicyDto>(`/policies/${id}`, req);
    return data;
  }
};
