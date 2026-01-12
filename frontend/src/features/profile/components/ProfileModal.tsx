import { useTranslation } from 'react-i18next';
import { Mail, Globe, User, Shield, Loader2 } from 'lucide-react';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from '@/shared/components/ui/dialog';
import { Avatar, AvatarFallback } from '@/shared/components/ui/avatar';
import { Separator } from '@/shared/components/ui/separator';
import { Badge } from '@/shared/components/ui/badge';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/shared/components/ui/select';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { useUserProfile } from '../api/queries';
import { useUpdateUserProfile } from '../api/mutations';
import { useLanguage } from '@/app/providers/LanguageContext';

interface ProfileModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export function ProfileModal({ isOpen, onClose }: ProfileModalProps) {
    const { t } = useTranslation('profile');
    const { setLanguage } = useLanguage();
    const { data: user, isLoading, isError, refetch } = useUserProfile();
    const updateProfile = useUpdateUserProfile();

    const initials = user
        ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()
        : '';
    const fullName = user ? `${user.firstName} ${user.lastName}`.trim() : '';

    const getRoleBadgeVariant = (role: string) => {
        switch (role) {
            case 'BROKER':
                return 'default';
            case 'ADMIN':
                return 'destructive';
            case 'CLIENT':
                return 'secondary';
            default:
                return 'outline';
        }
    };

    const getRoleLabel = (role: string) => {
        switch (role) {
            case 'BROKER':
                return t('roleBroker', 'Broker');
            case 'ADMIN':
                return t('roleAdmin', 'Administrator');
            case 'CLIENT':
                return t('roleClient', 'Client');
            default:
                return role;
        }
    };

    const handleLanguageChange = (newLanguage: string) => {
        updateProfile.mutate(
            { preferredLanguage: newLanguage },
            {
                onSuccess: () => {
                    // Also update the UI language immediately
                    setLanguage(newLanguage as 'en' | 'fr');
                },
            }
        );
    };

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
            <DialogContent className="max-w-md">
                {isLoading && <LoadingState message={t('loading', 'Loading profile...')} />}

                {isError && (
                    <ErrorState
                        message={t('error', 'Failed to load profile')}
                        onRetry={() => refetch()}
                    />
                )}

                {!isLoading && !isError && user && (
                    <>
                        <DialogHeader>
                            <div className="flex items-center gap-4">
                                <Avatar className="h-16 w-16">
                                    <AvatarFallback className="bg-primary text-primary-foreground text-xl font-semibold">
                                        {initials || <User className="h-6 w-6" />}
                                    </AvatarFallback>
                                </Avatar>
                                <div>
                                    <DialogTitle className="text-xl">{fullName}</DialogTitle>
                                    <DialogDescription className="flex items-center gap-2 mt-1">
                                        <Badge variant={getRoleBadgeVariant(user.role)}>
                                            {getRoleLabel(user.role)}
                                        </Badge>
                                        {user.active ? (
                                            <Badge variant="outline" className="border-green-500 text-green-600">
                                                {t('active', 'Active')}
                                            </Badge>
                                        ) : (
                                            <Badge variant="outline" className="border-red-500 text-red-600">
                                                {t('inactive', 'Inactive')}
                                            </Badge>
                                        )}
                                    </DialogDescription>
                                </div>
                            </div>
                        </DialogHeader>

                        <Separator className="my-4" />

                        <div className="space-y-4">
                            {/* Contact Information */}
                            <section>
                                <h4 className="text-sm font-medium text-foreground mb-3 flex items-center gap-2">
                                    <User className="h-4 w-4" />
                                    {t('personalInfo', 'Personal Information')}
                                </h4>
                                <div className="space-y-3 pl-6">
                                    <div className="flex items-center gap-3 text-sm">
                                        <Mail className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                                        <span className="text-muted-foreground">{t('email', 'Email')}:</span>
                                        <span className="text-foreground">{user.email}</span>
                                    </div>
                                    <div className="flex items-center gap-3 text-sm">
                                        <Globe className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                                        <span className="text-muted-foreground">{t('preferredLanguage', 'Language')}:</span>
                                        <div className="flex items-center gap-2">
                                            <Select
                                                value={user.preferredLanguage}
                                                onValueChange={handleLanguageChange}
                                                disabled={updateProfile.isPending}
                                            >
                                                <SelectTrigger className="w-32 h-8">
                                                    <SelectValue />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="en">{t('languageEnglish', 'English')}</SelectItem>
                                                    <SelectItem value="fr">{t('languageFrench', 'Français')}</SelectItem>
                                                </SelectContent>
                                            </Select>
                                            {updateProfile.isPending && (
                                                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                                            )}
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-3 text-sm">
                                        <Shield className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                                        <span className="text-muted-foreground">{t('role', 'Role')}:</span>
                                        <span className="text-foreground">{getRoleLabel(user.role)}</span>
                                    </div>
                                </div>
                            </section>
                        </div>
                    </>
                )}
            </DialogContent>
        </Dialog>
    );
}
