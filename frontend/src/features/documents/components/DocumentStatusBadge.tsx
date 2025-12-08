import { Badge } from '@/shared/components/ui/badge';
import { DocumentStatusEnum } from '../types';
import { useTranslation } from 'react-i18next';

interface DocumentStatusBadgeProps {
    status: DocumentStatusEnum;
}

export function DocumentStatusBadge({ status }: DocumentStatusBadgeProps) {
    const { t } = useTranslation('documents');

    const getVariant = (status: DocumentStatusEnum) => {
        switch (status) {
            case DocumentStatusEnum.APPROVED:
                return 'success';
            case DocumentStatusEnum.SUBMITTED:
                return 'secondary';
            case DocumentStatusEnum.NEEDS_REVISION:
                return 'destructive';
            case DocumentStatusEnum.REQUESTED:
            default:
                return 'secondary';
        }
    };

    return (
        <Badge variant={getVariant(status)}>
            {t(`status.${status}`)}
        </Badge>
    );
}
