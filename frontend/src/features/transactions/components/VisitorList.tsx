import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Trash2, Mail, Phone, Pencil, Plus, Users } from 'lucide-react';
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { Badge } from "@/shared/components/ui/badge";
import { useTransactionVisitors } from '@/features/transactions/api/queries';
import { useAddVisitor, useUpdateVisitor, useDeleteVisitor } from '@/features/transactions/api/mutations';
import { toast } from 'sonner';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";
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
import type { Visitor, VisitorRequestDTO } from '@/shared/api/types';

interface VisitorListProps {
    transactionId: string;
    isBroker: boolean;
}

export function VisitorList({ transactionId, isBroker }: VisitorListProps) {
    const { t } = useTranslation('transactions');
    const { data: visitors = [], isLoading } = useTransactionVisitors(transactionId);
    const addVisitor = useAddVisitor();
    const updateVisitor = useUpdateVisitor();
    const deleteVisitor = useDeleteVisitor();

    const [isAddOpen, setIsAddOpen] = useState(false);
    const [editingVisitor, setEditingVisitor] = useState<Visitor | null>(null);
    const [visitorToDelete, setVisitorToDelete] = useState<string | null>(null);

    const [formName, setFormName] = useState('');
    const [formEmail, setFormEmail] = useState('');
    const [formPhone, setFormPhone] = useState('');

    const resetForm = () => {
        setFormName('');
        setFormEmail('');
        setFormPhone('');
    };

    const openAdd = () => {
        resetForm();
        setIsAddOpen(true);
    };

    const openEdit = (visitor: Visitor) => {
        setFormName(visitor.name);
        setFormEmail(visitor.email || '');
        setFormPhone(visitor.phoneNumber || '');
        setEditingVisitor(visitor);
    };

    const handleAdd = () => {
        if (!formName.trim()) return;
        const data: VisitorRequestDTO = {
            name: formName.trim(),
            email: formEmail.trim() || undefined,
            phoneNumber: formPhone.trim() || undefined,
        };
        addVisitor.mutate({ transactionId, data }, {
            onSuccess: () => {
                toast.success(t('visitorAdded'));
                setIsAddOpen(false);
                resetForm();
            },
        });
    };

    const handleUpdate = () => {
        if (!editingVisitor || !formName.trim()) return;
        const data: VisitorRequestDTO = {
            name: formName.trim(),
            email: formEmail.trim() || undefined,
            phoneNumber: formPhone.trim() || undefined,
        };
        updateVisitor.mutate({ transactionId, visitorId: editingVisitor.visitorId, data }, {
            onSuccess: () => {
                toast.success(t('visitorUpdated'));
                setEditingVisitor(null);
                resetForm();
            },
        });
    };

    const handleDelete = () => {
        if (!visitorToDelete) return;
        deleteVisitor.mutate({ transactionId, visitorId: visitorToDelete }, {
            onSuccess: () => {
                toast.success(t('visitorDeleted'));
                setVisitorToDelete(null);
            },
        });
    };

    if (isLoading) {
        return <div className="animate-pulse h-24 bg-muted rounded-md" />;
    }

    const visitorForm = (
        <div className="space-y-3">
            <div>
                <label className="text-sm text-muted-foreground block mb-1">{t('visitorName')}</label>
                <Input
                    value={formName}
                    onChange={(e) => setFormName(e.target.value)}
                    placeholder={t('visitorName')}
                    autoFocus
                />
            </div>
            <div>
                <label className="text-sm text-muted-foreground block mb-1">{t('visitorEmail')}</label>
                <Input
                    type="email"
                    value={formEmail}
                    onChange={(e) => setFormEmail(e.target.value)}
                    placeholder={t('visitorEmail')}
                />
            </div>
            <div>
                <label className="text-sm text-muted-foreground block mb-1">{t('visitorPhone')}</label>
                <Input
                    type="tel"
                    value={formPhone}
                    onChange={(e) => setFormPhone(e.target.value)}
                    placeholder={t('visitorPhone')}
                />
            </div>
        </div>
    );

    return (
        <>
            <Card className="mt-4">
                <CardHeader className="flex flex-row items-center justify-between py-4">
                    <CardTitle className="text-lg font-medium flex items-center gap-2">
                        <Users className="h-5 w-5" />
                        {t('visitors')}
                    </CardTitle>
                    {isBroker && (
                        <Button size="sm" onClick={openAdd} className="gap-2">
                            <Plus className="h-4 w-4" />
                            {t('addVisitor')}
                        </Button>
                    )}
                </CardHeader>
                <CardContent>
                    {visitors.length === 0 ? (
                        <div className="text-center py-6 text-muted-foreground">
                            {t('noVisitors')}
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {visitors.map((visitor) => (
                                <div key={visitor.visitorId} className="flex items-start justify-between p-3 border rounded-lg bg-card">
                                    <div className="space-y-1">
                                        <div className="flex items-center gap-2">
                                            <span className="font-medium">{visitor.name}</span>
                                            <Badge variant="secondary" className="text-xs">
                                                {t('timesVisited', { count: visitor.timesVisited })}
                                            </Badge>
                                        </div>
                                        <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
                                            {visitor.email && (
                                                <div className="flex items-center gap-1">
                                                    <Mail className="h-3 w-3" />
                                                    <span>{visitor.email}</span>
                                                </div>
                                            )}
                                            {visitor.phoneNumber && (
                                                <div className="flex items-center gap-1">
                                                    <Phone className="h-3 w-3" />
                                                    <span>{visitor.phoneNumber}</span>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                    {isBroker && (
                                        <div className="flex gap-2">
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                className="text-muted-foreground hover:text-primary"
                                                onClick={() => openEdit(visitor)}
                                            >
                                                <Pencil className="h-4 w-4" />
                                            </Button>
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                className="text-muted-foreground hover:text-destructive"
                                                onClick={() => setVisitorToDelete(visitor.visitorId)}
                                            >
                                                <Trash2 className="h-4 w-4" />
                                            </Button>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* Add Visitor Modal */}
            <Dialog open={isAddOpen} onOpenChange={(val) => !val && setIsAddOpen(false)}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle>{t('addVisitor')}</DialogTitle>
                    </DialogHeader>
                    {visitorForm}
                    <div className="flex gap-2 justify-end pt-2">
                        <Button variant="ghost" onClick={() => setIsAddOpen(false)}>{t('cancel')}</Button>
                        <Button onClick={handleAdd} disabled={!formName.trim() || addVisitor.isPending}>
                            {t('addVisitor')}
                        </Button>
                    </div>
                </DialogContent>
            </Dialog>

            {/* Edit Visitor Modal */}
            <Dialog open={!!editingVisitor} onOpenChange={(val) => !val && setEditingVisitor(null)}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle>{t('editVisitor')}</DialogTitle>
                    </DialogHeader>
                    {visitorForm}
                    <div className="flex gap-2 justify-end pt-2">
                        <Button variant="ghost" onClick={() => setEditingVisitor(null)}>{t('cancel')}</Button>
                        <Button onClick={handleUpdate} disabled={!formName.trim() || updateVisitor.isPending}>
                            {t('editVisitor')}
                        </Button>
                    </div>
                </DialogContent>
            </Dialog>

            {/* Delete Confirmation */}
            <AlertDialog open={!!visitorToDelete} onOpenChange={(open) => !open && setVisitorToDelete(null)}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t('deleteVisitor')}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t('confirmDeleteVisitor')}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t('cancel')}</AlertDialogCancel>
                        <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground hover:bg-destructive/90">
                            {t('deleteVisitor')}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </>
    );
}
