import { Mail, User } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, CardContent } from '@/shared/components/ui/card';
import { Avatar, AvatarFallback } from '@/shared/components/ui/avatar';
import { Badge } from '@/shared/components/ui/badge';
import type { Client } from '../api/clientsApi';

interface ClientCardProps {
    client: Client;
    hasActiveTransaction: boolean;
    onClick: () => void;
}

export function ClientCard({ client, hasActiveTransaction, onClick }: ClientCardProps) {
    const { t } = useTranslation('clients');

    const initials = `${client.firstName?.[0] ?? ''}${client.lastName?.[0] ?? ''}`.toUpperCase();
    const fullName = `${client.firstName} ${client.lastName}`.trim();

    return (
        <Card
            className="cursor-pointer hover:shadow-md transition-all focus-within:ring-2 focus-within:ring-primary focus-within:ring-offset-2"
            role="button"
            tabIndex={0}
            onClick={onClick}
            onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    onClick();
                }
            }}
            aria-label={`${t('viewClient')}: ${fullName}`}
        >
            <CardContent className="pt-6">
                <div className="flex items-start gap-4">
                    <Avatar className="h-12 w-12">
                        <AvatarFallback className="bg-primary/10 text-primary font-medium">
                            {initials || <User className="h-5 w-5" />}
                        </AvatarFallback>
                    </Avatar>
                    <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-2">
                            <h3 className="font-medium text-foreground truncate">{fullName}</h3>
                            <Badge variant={hasActiveTransaction ? 'success' : 'secondary'}>
                                {hasActiveTransaction ? t('active') : t('inactive')}
                            </Badge>
                        </div>
                        <div className="flex items-center gap-1.5 mt-1 text-muted-foreground text-sm">
                            <Mail className="h-3.5 w-3.5 flex-shrink-0" />
                            <span className="truncate">{client.email}</span>
                        </div>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}
