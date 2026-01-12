import { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/shared/components/ui/dialog';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Textarea } from '@/shared/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/components/ui/select';
import { Label } from '@/shared/components/ui/label';
import { useAddCondition, useUpdateCondition, useRemoveCondition, useUpdateConditionStatus } from '../api/mutations';
import type { Condition, ConditionType, ConditionStatus, ConditionRequestDTO } from '@/shared/api/types';

interface AddConditionModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    transactionId: string;
    existingCondition?: Condition;
}

const CONDITION_TYPES: ConditionType[] = ['FINANCING', 'INSPECTION', 'SALE_OF_PROPERTY', 'OTHER'];
const CONDITION_STATUSES: ConditionStatus[] = ['PENDING', 'SATISFIED', 'FAILED'];

export function AddConditionModal({ open, onOpenChange, transactionId, existingCondition }: AddConditionModalProps) {
    const { t } = useTranslation('transactions');
    const isEditMode = !!existingCondition;

    // Compute initial values based on existingCondition
    const getInitialValues = () => ({
        type: existingCondition?.type ?? 'FINANCING' as ConditionType,
        customTitle: existingCondition?.customTitle ?? '',
        description: existingCondition?.description ?? '',
        deadlineDate: existingCondition?.deadlineDate ?? '',
        status: existingCondition?.status ?? 'PENDING' as ConditionStatus,
        notes: existingCondition?.notes ?? '',
    });

    // Use single state object to allow atomic reset
    const [formState, setFormState] = useState(getInitialValues);

    // Derived values for easier access
    const { type, customTitle, description, deadlineDate, status, notes } = formState;

    // Setters that update single fields
    const setType = (v: ConditionType) => setFormState(s => ({ ...s, type: v }));
    const setCustomTitle = (v: string) => setFormState(s => ({ ...s, customTitle: v }));
    const setDescription = (v: string) => setFormState(s => ({ ...s, description: v }));
    const setDeadlineDate = (v: string) => setFormState(s => ({ ...s, deadlineDate: v }));
    const setStatus = (v: ConditionStatus) => setFormState(s => ({ ...s, status: v }));
    const setNotes = (v: string) => setFormState(s => ({ ...s, notes: v }));

    // Reset form when modal opens - single setState call avoids cascading renders
    const prevOpenRef = useRef(open);
    useEffect(() => {
        if (open && !prevOpenRef.current) {
            // Single atomic reset - only one setState call
            setFormState(getInitialValues());
        }
        prevOpenRef.current = open;
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, existingCondition]);

    const addCondition = useAddCondition();
    const updateCondition = useUpdateCondition();
    const removeCondition = useRemoveCondition();
    const updateStatus = useUpdateConditionStatus();

    const resetForm = () => {
        setFormState({
            type: 'FINANCING',
            customTitle: '',
            description: '',
            deadlineDate: '',
            status: 'PENDING',
            notes: '',
        });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        const data: ConditionRequestDTO = {
            type,
            customTitle: type === 'OTHER' ? customTitle : undefined,
            description,
            deadlineDate,
            status,
            notes: notes || undefined,
        };

        try {
            if (isEditMode && existingCondition) {
                await updateCondition.mutateAsync({
                    transactionId,
                    conditionId: existingCondition.conditionId,
                    data,
                });
            } else {
                await addCondition.mutateAsync({ transactionId, data });
            }
            resetForm();
            onOpenChange(false);
        } catch {
            // Error handled by global error system
        }
    };

    const handleDelete = async () => {
        if (!existingCondition) return;

        try {
            await removeCondition.mutateAsync({
                transactionId,
                conditionId: existingCondition.conditionId,
            });
            resetForm();
            onOpenChange(false);
        } catch {
            // Error handled by global error system
        }
    };

    const handleStatusChange = async (newStatus: ConditionStatus) => {
        const previousStatus = status;
        setStatus(newStatus); // Optimistic update
        if (isEditMode && existingCondition) {
            try {
                await updateStatus.mutateAsync({
                    transactionId,
                    conditionId: existingCondition.conditionId,
                    status: newStatus,
                });
            } catch {
                // Rollback on error
                setStatus(previousStatus);
            }
        }
    };

    const isSubmitting = addCondition.isPending || updateCondition.isPending;
    const isDeleting = removeCondition.isPending;
    const isValid = description.trim() && deadlineDate && (type !== 'OTHER' || customTitle.trim());

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>
                        {isEditMode ? t('conditions.editCondition') : t('conditions.addCondition')}
                    </DialogTitle>
                </DialogHeader>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="space-y-2">
                        <Label>{t('conditions.type')}</Label>
                        <Select value={type} onValueChange={(v) => setType(v as ConditionType)}>
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {CONDITION_TYPES.map((condType) => (
                                    <SelectItem key={condType} value={condType}>
                                        {t(`conditionTypes.${condType}`)}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {type === 'OTHER' && (
                        <div className="space-y-2">
                            <Label>{t('conditions.customTitle')}</Label>
                            <Input
                                value={customTitle}
                                onChange={(e) => setCustomTitle(e.target.value)}
                                placeholder={t('conditions.customTitlePlaceholder')}
                                required
                            />
                        </div>
                    )}

                    <div className="space-y-2">
                        <Label>{t('conditions.description')}</Label>
                        <Textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder={t('conditions.descriptionPlaceholder')}
                            rows={3}
                            required
                        />
                    </div>

                    <div className="space-y-2">
                        <Label>{t('conditions.deadline')}</Label>
                        <Input
                            type="date"
                            value={deadlineDate}
                            onChange={(e) => setDeadlineDate(e.target.value)}
                            required
                        />
                    </div>

                    {isEditMode && (
                        <div className="space-y-2">
                            <Label>{t('conditions.status')}</Label>
                            <Select value={status} onValueChange={(v) => handleStatusChange(v as ConditionStatus)}>
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {CONDITION_STATUSES.map((s) => (
                                        <SelectItem key={s} value={s}>
                                            {t(`conditionStatus.${s}`)}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    )}

                    <div className="space-y-2">
                        <Label>{t('conditions.notes')}</Label>
                        <Textarea
                            value={notes}
                            onChange={(e) => setNotes(e.target.value)}
                            placeholder={t('conditions.notesPlaceholder')}
                            rows={2}
                        />
                    </div>

                    <DialogFooter className="gap-2">
                        {isEditMode && (
                            <Button
                                type="button"
                                variant="destructive"
                                onClick={handleDelete}
                                disabled={isDeleting || isSubmitting}
                                className="mr-auto"
                            >
                                {isDeleting ? t('common.deleting') : t('common.delete')}
                            </Button>
                        )}
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                            {t('common.cancel')}
                        </Button>
                        <Button type="submit" disabled={!isValid || isSubmitting}>
                            {isSubmitting ? t('common.saving') : isEditMode ? t('common.save') : t('common.add')}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
