import { TransactionCreateForm } from './TransactionCreateForm';
import { useTranslation } from 'react-i18next';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";

interface CreateTransactionModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

export function CreateTransactionModal({ isOpen, onClose, onSuccess }: CreateTransactionModalProps) {
    const { t } = useTranslation('transactions');

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle>{t('createTransactionTitle')}</DialogTitle>
                </DialogHeader>
                <TransactionCreateForm
                    onNavigate={(route) => {
                        if (route.startsWith("/transactions/") && route !== "/transactions") {
                            // This implies success (navigation to details)
                            onSuccess();
                            // In a real modal flow, we might want to redirect OR just close and refresh. 
                            // If onNavigate is used to Go Back (/transactions), we just close.
                        } else {
                            onClose();
                        }
                    }}
                    isModal={true}
                />
            </DialogContent>
        </Dialog>
    );
}
