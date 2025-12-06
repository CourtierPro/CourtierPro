import { useUserProfile } from '../api/queries';

export function useProfilePageLogic() {
    const { data: user, isLoading, error, refetch } = useUserProfile();

    return {
        user,
        isLoading,
        error,
        refetch,
    };
}
