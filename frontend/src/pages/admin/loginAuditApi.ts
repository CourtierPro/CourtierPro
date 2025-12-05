import axiosInstance from '@/api/axiosInstance';

export interface LoginAuditEvent {
  id: string;
  userId: string;
  email: string;
  role: string;
  timestamp: string;
  ipAddress?: string;
  userAgent?: string;
}

export async function getLoginAuditEvents(): Promise<LoginAuditEvent[]> {
  const res = await axiosInstance.get<LoginAuditEvent[]>('/api/admin/login-audit');
  return res.data;
}
