import React, { useState } from 'react';
import { Toast } from '@/shared/components/ui/Toast';
import { useTranslation } from 'react-i18next';
import { Mail, Globe, User, Shield, Loader2, Bell, BellOff, KeyRound } from 'lucide-react';
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
import { useMfaStatus } from '../api/useMfaStatus';
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

    // Correctly place the MFA status hook inside the component
    const { data: mfaEnabled, isLoading: mfaLoading, isError: mfaError } = useMfaStatus();

    const [showToast, setShowToast] = useState(false);

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


    const [emailInput, setEmailInput] = useState(user?.email || '');
    const [emailDirty, setEmailDirty] = useState(false);
    const [emailChangeMsg, setEmailChangeMsg] = useState<string | null>(null);
    const [emailNotifications, setEmailNotifications] = useState(user?.emailNotificationsEnabled ?? true);
    const [inAppNotifications, setInAppNotifications] = useState(user?.inAppNotificationsEnabled ?? true);

    // Sync state when user changes
    React.useEffect(() => {
        setEmailInput(user?.email || '');
        setEmailNotifications(user?.emailNotificationsEnabled ?? true);
        setInAppNotifications(user?.inAppNotificationsEnabled ?? true);
    }, [user]);

    const handleLanguageChange = (newLanguage: string) => {
        updateProfile.mutate(
            { preferredLanguage: newLanguage },
            {
                onSuccess: () => {
                    setLanguage(newLanguage as 'en' | 'fr');
                },
            }
        );
    };

    const handleEmailChange = () => {
        if (emailInput && emailInput !== user?.email) {
            updateProfile.mutate(
                { 
                    email: emailInput, 
                    preferredLanguage: user?.preferredLanguage || 'en' // Always include preferredLanguage
                },
                {
                    onSuccess: () => {
                        setEmailChangeMsg(t('emailChangeSent', 'Confirmation sent to new email. Please check your inbox.'));
                        setShowToast(true);
                        setEmailDirty(false);
                        refetch(); // Refetch user profile to update UI
                        setTimeout(() => {
                            setShowToast(false);
                            // Clear auth tokens (adjust if you use a different storage or context)
                            localStorage.removeItem('accessToken');
                            localStorage.removeItem('refreshToken');
                            window.location.replace('/login');
                        }, 4000); // Increased delay for better visibility
                    },
                }
            );
        }
    };

    const handleNotificationChange = (type: 'email' | 'inApp', value: boolean) => {
        if (!user) return;
        if (type === 'email') {
            setEmailNotifications(value);
            updateProfile.mutate({ 
                emailNotificationsEnabled: value,
                preferredLanguage: user.preferredLanguage || 'en'
            });
        } else {
            setInAppNotifications(value);
            updateProfile.mutate({ 
                inAppNotificationsEnabled: value,
                preferredLanguage: user.preferredLanguage || 'en'
            });
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
            <DialogContent className="max-w-md">
                {showToast && (
                    <Toast
                        message={t('emailChangeSent', 'Confirmation sent to new email. Please check your inbox.')}
                        onClose={() => setShowToast(false)}
                        duration={3500}
                    />
                )}
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
                                        <KeyRound className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                                        <span className="text-muted-foreground">Multi-Factor Auth (MFA):</span>
                                        {mfaLoading ? (
                                            <span className="text-muted-foreground">Checking...</span>
                                        ) : mfaError ? (
                                            <span className="text-red-600">Error</span>
                                        ) : mfaEnabled ? (
                                            <span className="text-green-600 font-semibold">Enabled</span>
                                        ) : (
                                            <span className="text-yellow-600 font-semibold">Not Enabled</span>
                                        )}
                                    </div>
                                    <div className="flex items-center gap-3 text-sm">
                                        <Mail className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                                        <span className="text-muted-foreground">{t('email', 'Email')}:</span>
                                        <input
                                            className="border rounded px-2 py-1 text-foreground bg-background"
                                            type="email"
                                            value={emailInput}
                                            onChange={e => {
                                                setEmailInput(e.target.value);
                                                setEmailDirty(e.target.value !== user?.email);
                                                setEmailChangeMsg(null);
                                            }}
                                            disabled={updateProfile.isPending}
                                            style={{ width: 220 }}
                                        />
                                        {emailDirty && (
                                            <button
                                                className="ml-2 px-2 py-1 bg-primary text-primary-foreground rounded"
                                                onClick={handleEmailChange}
                                                disabled={updateProfile.isPending}
                                            >
                                                {t('update', 'Update')}
                                            </button>
                                        )}
                                    </div>
                                    {emailChangeMsg && (
                                        <div className="text-xs text-green-600 pl-8">{emailChangeMsg}</div>
                                    )}
                                    <div className="flex items-center gap-3 text-sm">
                                        <Bell className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                                        <span className="text-muted-foreground">{t('emailNotifications', 'Email Notifications')}:</span>
                                        <input
                                            type="checkbox"
                                            checked={emailNotifications}
                                            onChange={e => handleNotificationChange('email', e.target.checked)}
                                            disabled={updateProfile.isPending}
                                            className="ml-2"
                                        />
                                        <span className="text-muted-foreground">{emailNotifications ? t('on', 'On') : t('off', 'Off')}</span>
                                    </div>
                                    <div className="flex items-center gap-3 text-sm">
                                        <BellOff className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                                        <span className="text-muted-foreground">{t('inAppNotifications', 'In-App Notifications')}:</span>
                                        <input
                                            type="checkbox"
                                            checked={inAppNotifications}
                                            onChange={e => handleNotificationChange('inApp', e.target.checked)}
                                            disabled={updateProfile.isPending}
                                            className="ml-2"
                                        />
                                        <span className="text-muted-foreground">{inAppNotifications ? t('on', 'On') : t('off', 'Off')}</span>
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
