
import { useTranslation } from 'react-i18next';
import { useForm, Controller } from 'react-hook-form';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/shared/components/ui/dialog";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/shared/components/ui/select";
import { Checkbox } from "@/shared/components/ui/checkbox";
import { toast } from 'sonner';
import { useAddParticipant } from '@/features/transactions/api/mutations';
import { useTransactionParticipants, useTransaction, useBrokers } from '@/features/transactions/api/queries';
import type { ParticipantRole, ParticipantPermission } from '@/shared/api/types';

interface AddParticipantModalProps {
    isOpen: boolean;
    onClose: () => void;
    transactionId: string;
}

interface ParticipantFormValues {
    name: string;
    role: ParticipantRole;
    email: string;
    phoneNumber: string;
    permissions: ParticipantPermission[];
}

const PERMISSIONS: { id: ParticipantPermission; label: string }[] = [
    { id: 'VIEW_DOCUMENTS', label: 'View Documents' },
    { id: 'EDIT_DOCUMENTS', label: 'Edit Documents' },
    { id: 'VIEW_PROPERTIES', label: 'View Properties' },
    { id: 'EDIT_PROPERTIES', label: 'Edit Properties' },
    { id: 'VIEW_STAGE', label: 'View Stage' },
    { id: 'EDIT_STAGE', label: 'Edit Stage' },
    { id: 'VIEW_OFFERS', label: 'View Offers' },
    { id: 'EDIT_OFFERS', label: 'Edit Offers' },
    { id: 'VIEW_CONDITIONS', label: 'View Conditions' },
    { id: 'EDIT_CONDITIONS', label: 'Edit Conditions' },
    { id: 'VIEW_NOTES', label: 'View Notes' },
    { id: 'EDIT_NOTES', label: 'Edit Notes' },
];

export default function AddParticipantModal({ isOpen, onClose, transactionId }: AddParticipantModalProps) {
    const { t } = useTranslation('transactions');
    const addParticipant = useAddParticipant();
    const { data: participants = [] } = useTransactionParticipants(transactionId);
    const { data: transaction } = useTransaction(transactionId);

    const { register, handleSubmit, reset, control, watch, formState: { errors }, setError, setValue, getValues } = useForm<ParticipantFormValues>({
        defaultValues: {
            name: '',
            role: 'CO_BROKER',
            email: '',
            phoneNumber: '',
            permissions: []
        }
    });

    const selectedRole = watch('role');

    const existingEmails = participants.map(p => p.email?.toLowerCase()).filter(Boolean);

    const primaryBrokerEmail = transaction?.brokerId
        ? participants.find(p => p.role === 'BROKER' && p.isSystem)?.email?.toLowerCase()
        : undefined;

    const onSubmit = async (data: ParticipantFormValues) => {
        const emailLower = data.email?.toLowerCase();

        if (emailLower && existingEmails.includes(emailLower)) {
            setError('email', {
                type: 'manual',
                message: t('emailAlreadyExists') || 'Cet email est déjà utilisé dans la transaction.'
            });
            return;
        }

        if (primaryBrokerEmail && emailLower && emailLower === primaryBrokerEmail) {
            setError('email', {
                type: 'manual',
                message: t('cannotAddPrimaryBroker') || 'Impossible d’ajouter le broker principal comme participant.'
            });
            return;
        }

        try {
            await addParticipant.mutateAsync({
                transactionId,
                data: {
                    ...data,
                    email: data.email || undefined,
                    phoneNumber: data.phoneNumber || undefined,
                    permissions: data.role === 'CO_BROKER' ? data.permissions : undefined
                }
            });
            toast.success(t('participantAdded'));
            reset();
            onClose();
        } catch {
            toast.error(t('errorAddingParticipant'));
        }
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            reset();
            onClose();
        }
    };

    const { data: brokers = [] } = useBrokers();

    // Filter brokers to exclude primary broker
    const availableBrokers = brokers.filter(b =>
        (!primaryBrokerEmail || b.email.toLowerCase() !== primaryBrokerEmail) &&
        !existingEmails.includes(b.email.toLowerCase())
    );

    return (
        <Dialog open={isOpen} onOpenChange={handleOpenChange}>
            <DialogContent className="sm:max-w-[425px] max-h-[85vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle>{t('addParticipant')}</DialogTitle>
                </DialogHeader>
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                    <div className="space-y-2">
                        <Label htmlFor="name">{t('name')}</Label>
                        <Input
                            id="name"
                            {...register('name', { required: t('nameRequired') || 'Name is required' })}
                            placeholder={t('participantNamePlaceholder')}
                        />
                        {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="role">{t('role')}</Label>
                        <Controller
                            control={control}
                            name="role"
                            rules={{ required: t('selectRole') }}
                            render={({ field }) => (
                                <Select onValueChange={field.onChange} defaultValue={field.value}>
                                    <SelectTrigger>
                                        <SelectValue placeholder={t('selectRole')} />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="BROKER">{t('roles.BROKER')}</SelectItem>
                                        <SelectItem value="CO_BROKER">{t('roles.CO_BROKER')}</SelectItem>
                                        <SelectItem value="NOTARY">{t('roles.NOTARY')}</SelectItem>
                                        <SelectItem value="LAWYER">{t('roles.LAWYER')}</SelectItem>
                                        <SelectItem value="BUYER">{t('roles.BUYER')}</SelectItem>
                                        <SelectItem value="SELLER">{t('roles.SELLER')}</SelectItem>
                                        <SelectItem value="OTHER">{t('roles.OTHER')}</SelectItem>
                                    </SelectContent>
                                </Select>
                            )}
                        />
                        {errors.role && <p className="text-sm text-destructive">{errors.role.message}</p>}
                    </div>

                    {selectedRole === 'CO_BROKER' && (
                        <div className="space-y-3 border p-4 rounded-md bg-muted/20">
                            <Label className="text-base font-semibold">{t('permissionsLabel')}</Label>
                            <p className="text-sm text-muted-foreground mb-2">
                                {t('permissionsDescription')}
                            </p>
                            <div className="mb-3">
                                <Button
                                    type="button"
                                    variant="outline"
                                    size="sm"
                                    className="text-xs h-7"
                                    onClick={() => {
                                        const allPermissionIds = PERMISSIONS.map(p => p.id);
                                        const currentPermissions = getValues('permissions') || [];
                                        if (currentPermissions.length === allPermissionIds.length) {
                                            setValue('permissions', []);
                                        } else {
                                            setValue('permissions', allPermissionIds);
                                        }
                                    }}
                                >
                                    {t('selectAll')}
                                </Button>
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                                <Controller
                                    control={control}
                                    name="permissions"
                                    render={({ field }) => (
                                        <>
                                            {PERMISSIONS.map((permission) => (
                                                <div key={permission.id} className="flex items-center space-x-2">
                                                    <Checkbox
                                                        id={permission.id}
                                                        checked={field.value?.includes(permission.id)}
                                                        onCheckedChange={(checked) => {
                                                            const currentValues = field.value || [];
                                                            if (checked) {
                                                                field.onChange([...currentValues, permission.id]);
                                                            } else {
                                                                field.onChange(currentValues.filter((v) => v !== permission.id));
                                                            }
                                                        }}
                                                    />
                                                    <Label
                                                        htmlFor={permission.id}
                                                        className="font-normal cursor-pointer"
                                                    >
                                                        {t(`permissions.${permission.id}`) || permission.label}
                                                    </Label>
                                                </div>
                                            ))}
                                        </>
                                    )}
                                />
                            </div>
                        </div>
                    )}

                    <div className="space-y-2">
                        <Label htmlFor="email">{t('email')}</Label>
                        <Input
                            id="email"
                            list="broker-emails"
                            {...register('email', {
                                pattern: {
                                    value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                                    message: t('invalidEmail') || 'Invalid email address'
                                },
                                onChange: (e) => {
                                    // Auto-fill name if email matches a broker
                                    const email = e.target.value;
                                    const broker = availableBrokers.find(b => b.email === email);
                                    if (broker && !getValues('name')) {
                                        setValue('name', `${broker.firstName} ${broker.lastName}`);
                                    }
                                }
                            })}
                            placeholder="example@email.com"
                        />
                        <datalist id="broker-emails">
                            {availableBrokers.map(broker => (
                                <option key={broker.id} value={broker.email}>
                                    {broker.firstName} {broker.lastName}
                                </option>
                            ))}
                        </datalist>
                        {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="phoneNumber">{t('phoneNumber')}</Label>
                        <Input id="phoneNumber" {...register('phoneNumber')} placeholder="(555) 555-5555" />
                    </div>

                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={onClose}>{t('cancel')}</Button>
                        <Button type="submit" disabled={addParticipant.isPending}>{t('add')}</Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog >
    );
}
