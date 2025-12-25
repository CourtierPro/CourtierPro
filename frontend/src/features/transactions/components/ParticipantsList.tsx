
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2, Mail, Phone } from 'lucide-react';
import { Button } from "@/shared/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { Badge } from "@/shared/components/ui/badge";
import { useTransactionParticipants } from '@/features/transactions/api/queries';
import { useRemoveParticipant } from '@/features/transactions/api/mutations';
import { AddParticipantModal } from './AddParticipantModal';
import { toast } from 'sonner';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/shared/components/ui/alert-dialog";

interface ParticipantsListProps {
    transactionId: string;
    isReadOnly?: boolean;
}

export function ParticipantsList({ transactionId, isReadOnly = false }: ParticipantsListProps) {
    const { t } = useTranslation('transactions');
    const { data: participants, isLoading } = useTransactionParticipants(transactionId);
    const removeParticipant = useRemoveParticipant();

    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [participantToDelete, setParticipantToDelete] = useState<string | null>(null);

    const handleRemove = async () => {
        if (!participantToDelete) return;
        try {
            await removeParticipant.mutateAsync({ transactionId, participantId: participantToDelete });
            toast.success(t('participantRemoved'));
        } catch {
            toast.error(t('errorRemovingParticipant'));
        } finally {
            setParticipantToDelete(null);
        }
    };

    if (isLoading) {
        return <div className="animate-pulse h-24 bg-muted rounded-md" />;
    }

    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between py-4">
                <CardTitle className="text-lg font-medium">{t('participants')}</CardTitle>
                {!isReadOnly && (
                    <Button size="sm" onClick={() => setIsAddModalOpen(true)} className="gap-2">
                        <Plus className="h-4 w-4" />
                        {t('addParticipant')}
                    </Button>
                )}
            </CardHeader>
            <CardContent>
                {participants?.length === 0 ? (
                    <div className="text-center py-6 text-muted-foreground">
                        {t('noParticipants')}
                    </div>
                ) : (
                    <div className="space-y-4">
                        {participants?.map((participant) => (
                            <div key={participant.id} className="flex items-start justify-between p-3 border rounded-lg bg-card">
                                <div className="space-y-1">
                                    <div className="flex items-center gap-2">
                                        <span className="font-medium">{participant.name}</span>
                                        <Badge variant="outline">{t(`roles.${participant.role}`)}</Badge>
                                    </div>
                                    <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
                                        {participant.email && (
                                            <div className="flex items-center gap-1">
                                                <Mail className="h-3 w-3" />
                                                <span>{participant.email}</span>
                                            </div>
                                        )}
                                        {participant.phoneNumber && (
                                            <div className="flex items-center gap-1">
                                                <Phone className="h-3 w-3" />
                                                <span>{participant.phoneNumber}</span>
                                            </div>
                                        )}
                                    </div>
                                </div>
                                {!isReadOnly && (
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        className="text-muted-foreground hover:text-destructive"
                                        onClick={() => setParticipantToDelete(participant.id)}
                                    >
                                        <Trash2 className="h-4 w-4" />
                                        <span className="sr-only">{t('remove')}</span>
                                    </Button>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </CardContent>

            <AddParticipantModal
                isOpen={isAddModalOpen}
                onClose={() => setIsAddModalOpen(false)}
                transactionId={transactionId}
            />

            <AlertDialog open={!!participantToDelete} onOpenChange={(open) => !open && setParticipantToDelete(null)}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t('confirmRemoveParticipant')}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t('confirmRemoveParticipantDescription')}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t('cancel')}</AlertDialogCancel>
                        <AlertDialogAction onClick={handleRemove} className="bg-destructive text-destructive-foreground hover:bg-destructive/90">
                            {t('remove')}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </Card>
    );
}
