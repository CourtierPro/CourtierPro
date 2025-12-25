
import { useTranslation } from 'react-i18next';
import { useForm, Controller } from 'react-hook-form';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/shared/components/ui/dialog";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/shared/components/ui/select";
import { toast } from 'sonner';
import { useAddParticipant } from '@/features/transactions/api/mutations';

interface AddParticipantModalProps {
    isOpen: boolean;
    onClose: () => void;
    transactionId: string;
}

type Role = 'BROKER' | 'CO_BROKER' | 'NOTARY' | 'LAWYER' | 'BUYER' | 'SELLER' | 'OTHER';

interface ParticipantFormValues {
    name: string;
    role: Role;
    email: string;
    phoneNumber: string;
}

export function AddParticipantModal({ isOpen, onClose, transactionId }: AddParticipantModalProps) {
    const { t } = useTranslation('transactions');
    const addParticipant = useAddParticipant();

    const { register, handleSubmit, reset, control, formState: { errors } } = useForm<ParticipantFormValues>({
        defaultValues: {
            name: '',
            role: 'CO_BROKER',
            email: '',
            phoneNumber: ''
        }
    });

    const onSubmit = async (data: ParticipantFormValues) => {
        try {
            await addParticipant.mutateAsync({
                transactionId,
                data: {
                    ...data,
                    email: data.email || undefined,
                    phoneNumber: data.phoneNumber || undefined,
                }
            });
            toast.success(t('participantAdded'));
            reset();
            onClose();
        } catch {
            toast.error(t('errorAddingParticipant'));
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-[425px]">
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

                    <div className="space-y-2">
                        <Label htmlFor="email">{t('email')}</Label>
                        <Input
                            id="email"
                            {...register('email', {
                                pattern: {
                                    value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                                    message: t('invalidEmail') || 'Invalid email address'
                                }
                            })}
                            placeholder="example@email.com"
                        />
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
        </Dialog>
    );
}
