import { useForm, Controller } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/shared/components/ui/dialog";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/shared/components/ui/select";
import { Checkbox } from "@/shared/components/ui/checkbox";
import { toast } from 'sonner';
import { useUpdateParticipant } from '@/features/transactions/api/mutations';
import type { ParticipantRole, ParticipantPermission, UpdateParticipantRequestDTO, TransactionParticipant } from '@/shared/api/types';
import { useEffect } from 'react';

interface EditParticipantModalProps {
    isOpen: boolean;
    onClose: () => void;
    transactionId: string;
    participant: TransactionParticipant | null;
}

interface ParticipantFormValues {
    name: string;
    role: ParticipantRole;
    email: string;
    phoneNumber: string;
    permissions: ParticipantPermission[];
}

const ROLES: ParticipantRole[] = ['BROKER', 'CO_BROKER', 'NOTARY', 'LAWYER', 'BUYER', 'SELLER', 'OTHER'];

const PERMISSIONS: { id: ParticipantPermission; label: string }[] = [
    { id: 'EDIT', label: 'Edit Transaction' },
    { id: 'MANAGE_DOCUMENTS', label: 'Manage Documents' },
    { id: 'MANAGE_OFFERS', label: 'Manage Offers' },
    { id: 'MANAGE_CONDITIONS', label: 'Manage Conditions' }
];

export function EditParticipantModal({ isOpen, onClose, transactionId, participant }: EditParticipantModalProps) {
    const { t } = useTranslation('transactions');
    const updateParticipant = useUpdateParticipant();

    const { register, handleSubmit, reset, control, watch, setValue, formState: { errors } } = useForm<ParticipantFormValues>({
        defaultValues: {
            name: '',
            role: 'CO_BROKER',
            email: '',
            phoneNumber: '',
            permissions: []
        }
    });

    useEffect(() => {
        if (participant) {
            setValue('name', participant.name);
            setValue('role', participant.role);
            setValue('email', participant.email || '');
            setValue('phoneNumber', participant.phoneNumber || '');
            setValue('permissions', participant.permissions || []);
        }
    }, [participant, setValue]);

    const selectedRole = watch('role');

    const onSubmit = async (data: ParticipantFormValues) => {
        if (!participant) return;

        try {
            const updateData: UpdateParticipantRequestDTO = {
                name: data.name,
                role: data.role,
                email: data.email || undefined,
                phoneNumber: data.phoneNumber || undefined,
                permissions: data.role === 'CO_BROKER' ? data.permissions : undefined
            };

            await updateParticipant.mutateAsync({
                transactionId,
                participantId: participant.id,
                data: updateData
            });
            toast.success(t('participantUpdated'));
            onClose();
        } catch {
            toast.error(t('errorUpdatingParticipant'));
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-[500px]">
                <DialogHeader>
                    <DialogTitle>{t('editParticipant')}</DialogTitle>
                    <DialogDescription>
                        {t('editParticipantDescription')}
                    </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                    <div className="grid gap-2">
                        <Label htmlFor="name">{t('name')} *</Label>
                        <Input
                            id="name"
                            {...register('name', { required: t('nameRequired') })}
                            placeholder={t('enterName')}
                        />
                        {errors.name && <span className="text-sm text-destructive">{errors.name.message}</span>}
                    </div>

                    <div className="grid gap-2">
                        <Label htmlFor="role">{t('role')} *</Label>
                        <Controller
                            control={control}
                            name="role"
                            rules={{ required: t('roleRequired') }}
                            render={({ field }) => (
                                <Select onValueChange={field.onChange} value={field.value}>
                                    <SelectTrigger>
                                        <SelectValue placeholder={t('selectRole')} />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {ROLES.map((role) => (
                                            <SelectItem key={role} value={role}>
                                                {t(`roles.${role}`)}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            )}
                        />
                    </div>

                    {(selectedRole === 'BROKER' || selectedRole === 'CO_BROKER') && (
                        <div className="grid gap-2">
                            <Label htmlFor="email">{t('email')} *</Label>
                            <Input
                                id="email"
                                type="email"
                                {...register('email', {
                                    required: t('emailRequiredForBroker')
                                })}
                                placeholder={t('enterEmail')}
                            />
                            {errors.email && <span className="text-sm text-destructive">{errors.email.message}</span>}
                            <p className="text-xs text-muted-foreground">{t('brokerEmailNote')}</p>
                        </div>
                    )}

                    {selectedRole !== 'BROKER' && selectedRole !== 'CO_BROKER' && (
                        <div className="grid gap-2">
                            <Label htmlFor="email">{t('email')}</Label>
                            <Input
                                id="email"
                                type="email"
                                {...register('email')}
                                placeholder={t('enterEmail')}
                            />
                        </div>
                    )}

                    <div className="grid gap-2">
                        <Label htmlFor="phoneNumber">{t('phoneNumber')}</Label>
                        <Input
                            id="phoneNumber"
                            {...register('phoneNumber')}
                            placeholder={t('enterPhoneNumber')}
                        />
                    </div>

                    {selectedRole === 'CO_BROKER' && (
                        <div className="border rounded-md p-4 space-y-3 bg-muted/20">
                            <h4 className="text-sm font-medium">{t('permissions.label')}</h4>
                            <div className="grid grid-cols-2 gap-3">
                                {PERMISSIONS.map((permission) => (
                                    <Controller
                                        key={permission.id}
                                        control={control}
                                        name="permissions"
                                        render={({ field }) => (
                                            <div className="flex items-center space-x-2">
                                                <Checkbox
                                                    id={`perm-${permission.id}`}
                                                    checked={(field.value || []).includes(permission.id)}
                                                    onCheckedChange={(checked) => {
                                                        const current = field.value || [];
                                                        const updated = checked
                                                            ? [...current, permission.id]
                                                            : current.filter((value) => value !== permission.id);
                                                        field.onChange(updated);
                                                    }}
                                                />
                                                <Label
                                                    htmlFor={`perm-${permission.id}`}
                                                    className="text-sm font-normal cursor-pointer text-muted-foreground"
                                                >
                                                    {t(`permissions.${permission.id}`) || permission.label}
                                                </Label>
                                            </div>
                                        )}
                                    />
                                ))}
                            </div>
                        </div>
                    )}

                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={onClose}>
                            {t('cancel')}
                        </Button>
                        <Button type="submit" disabled={updateParticipant.isPending}>
                            {updateParticipant.isPending ? t('saving') : t('save')}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
