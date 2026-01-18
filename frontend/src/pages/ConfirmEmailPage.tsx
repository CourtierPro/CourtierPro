import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axiosInstance from '@/shared/api/axiosInstance';

export function ConfirmEmailPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    // Removed unused state variables

    useEffect(() => {
        const token = searchParams.get('token');
        if (!token) {
            // No token, redirect to dashboard
            navigate('/dashboard/client', { replace: true });
            return;
        }
        axiosInstance.get(`/api/me/confirm-email?token=${token}`)
            .then(() => {
                // On success, redirect to dashboard
                navigate('/dashboard/client', { replace: true });
            })
            .catch(() => {
                // On error, also redirect to dashboard
                navigate('/dashboard/client', { replace: true });
            });
    }, [searchParams, navigate]);

    // Optionally, show nothing or a loading spinner while redirecting
    return null;
}
