import axiosInstance from "@/shared/api/axiosInstance";
import type { AdminUserResponse } from "@/features/admin/components/InviteUserModal";

export async function getAdminUsers(): Promise<AdminUserResponse[]> {
    const res = await axiosInstance.get<AdminUserResponse[]>("/api/admin/users");
    return res.data;
}

export async function setUserActiveStatus(
    userId: string,
    active: boolean
): Promise<AdminUserResponse> {
    const res = await axiosInstance.patch<AdminUserResponse>(
        `/api/admin/users/${userId}/status`,
        { active }
    );
    return res.data;
}
