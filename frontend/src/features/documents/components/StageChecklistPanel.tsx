import { useMemo, useState } from 'react';
import { CheckSquare, Loader2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';

import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/components/ui/select';
import { Checkbox } from '@/shared/components/ui/checkbox';
import { Badge } from '@/shared/components/ui/badge';
import { Label } from '@/shared/components/ui/label';
import { Button } from '@/shared/components/ui/button';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/shared/components/ui/alert-dialog';

import { useStageChecklist } from '@/features/documents/api/queries';
import { useUpdateChecklistManualState } from '@/features/documents/api/mutations';
import type { StageChecklistItemDTO } from '@/features/documents/api/documentsApi';
import { getLocalizedDocumentTitle } from '@/features/documents/utils/formatDocumentTitle';

interface StageOption {
  value: string;
  label: string;
}

interface StageChecklistPanelProps {
  transactionId: string;
  stageOptions: StageOption[];
  currentStage?: string;
  canToggle: boolean;
}

export function StageChecklistPanel({
  transactionId,
  stageOptions,
  currentStage,
  canToggle,
}: StageChecklistPanelProps) {
  const { t } = useTranslation('documents');
  const resolveDefaultStage = (): string => {
    if (stageOptions.length === 0) return '';
    if (currentStage && stageOptions.some((stage) => stage.value === currentStage)) {
      return currentStage;
    }
    return stageOptions[0]?.value ?? '';
  };

  const [selectedStage, setSelectedStage] = useState<string>(() => resolveDefaultStage());
  const selectedStageIsValid = stageOptions.some((stage) => stage.value === selectedStage);
  const effectiveSelectedStage = selectedStageIsValid ? selectedStage : resolveDefaultStage();
  const [pendingAutoOverride, setPendingAutoOverride] = useState<{
    itemKey: string;
    checked: boolean;
  } | null>(null);
  const updateManualState = useUpdateChecklistManualState();

  const { data, isLoading, error } = useStageChecklist(transactionId, effectiveSelectedStage);

  const sourceLabelMap = useMemo(
    () => ({
      AUTO: t('checklistSourceAuto', 'Auto'),
      MANUAL: t('checklistSourceManual', 'Manual'),
    }),
    [t]
  );

  const applyToggle = (itemKey: string, checked: boolean) => {
    if (!effectiveSelectedStage) return;

    updateManualState.mutate(
      {
        transactionId,
        itemKey,
        stage: effectiveSelectedStage,
        checked,
      },
      {
        onError: () => {
          toast.error(t('errors.updateChecklistFailed', 'Failed to update checklist item'));
        },
      }
    );
  };

  const handleToggle = (item: StageChecklistItemDTO, checked: boolean) => {
    if (item.source !== 'MANUAL') {
      setPendingAutoOverride({ itemKey: item.itemKey, checked });
      return;
    }

    applyToggle(item.itemKey, checked);
  };

  const handleConfirmAutoOverride = () => {
    if (!pendingAutoOverride) return;
    applyToggle(pendingAutoOverride.itemKey, pendingAutoOverride.checked);
    setPendingAutoOverride(null);
  };

  return (
    <Card>
      <CardHeader className="space-y-3">
        <div className="flex items-center gap-2">
          <CheckSquare className="w-5 h-5 text-primary" />
          <CardTitle>{t('stageChecklistTitle', 'Document Checklist')}</CardTitle>
        </div>
        <p className="text-sm text-muted-foreground">
          {t('stageChecklistDescription', 'Track required documents for each stage.')}
        </p>
        <div>
          <Label className="text-xs text-muted-foreground">{t('associatedStage', 'Associated Stage')}</Label>
          <Select value={effectiveSelectedStage} onValueChange={setSelectedStage}>
            <SelectTrigger className="mt-1">
              <SelectValue placeholder={t('selectStage', 'Select a stage')} />
            </SelectTrigger>
            <SelectContent>
              {stageOptions.map((stage) => (
                <SelectItem key={stage.value} value={stage.value}>
                  {stage.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="w-4 h-4 animate-spin" />
            {t('loading', 'Loading...')}
          </div>
        )}

        {!isLoading && error && (
          <p className="text-sm text-destructive">
            {t('errors.loadChecklistFailed', 'Failed to load checklist')}
          </p>
        )}

        {!isLoading && !error && (!data?.items || data.items.length === 0) && (
          <p className="text-sm text-muted-foreground">
            {t('noChecklistItems', 'No checklist items for this stage.')}
          </p>
        )}

        {!isLoading && !error && data?.items && data.items.length > 0 && (
          <div className="space-y-3">
            {data.items.map((item) => (
              <div
                key={item.itemKey}
                className="rounded-md border border-border p-3 space-y-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                role={canToggle ? 'button' : undefined}
                tabIndex={canToggle ? 0 : -1}
                onClick={() => {
                  if (!canToggle || updateManualState.isPending) return;
                  handleToggle(item, !item.checked);
                }}
                onKeyDown={(event) => {
                  if (!canToggle || updateManualState.isPending) return;
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    handleToggle(item, !item.checked);
                  }
                }}
              >
                <div className="flex items-start gap-2">
                  <Checkbox
                    id={`checklist-${item.itemKey}`}
                    checked={item.checked}
                    onCheckedChange={(value) => handleToggle(item, Boolean(value))}
                    onClick={(event) => event.stopPropagation()}
                    disabled={!canToggle || updateManualState.isPending}
                  />
                  <div className="flex-1 min-w-0">
                    <button
                      type="button"
                      className="text-sm font-medium cursor-pointer text-left"
                      onClick={(event) => {
                        event.stopPropagation();
                        handleToggle(item, !item.checked);
                      }}
                      disabled={!canToggle || updateManualState.isPending}
                    >
                      {getLocalizedDocumentTitle(t, { docType: item.docType, fallbackLabel: item.label })}
                    </button>
                    <div className="mt-1 flex flex-wrap items-center gap-2">
                      <Badge variant="outline" className="text-[10px]">
                        {item.flow === 'UPLOAD' ? t('flow.UPLOAD', 'Shared') : t('flow.REQUEST', 'Request')}
                      </Badge>
                      <Badge variant="secondary" className="text-[10px]">
                        {sourceLabelMap[item.source]}
                      </Badge>
                      {item.requiresSignature && (
                        <Badge variant="outline" className="text-[10px]">
                          {t('signatureRequired', 'Signature Required')}
                        </Badge>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>

      <AlertDialog
        open={!!pendingAutoOverride}
        onOpenChange={(open) => {
          if (!open) {
            setPendingAutoOverride(null);
          }
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t('checklistAutoOverrideTitle', 'Switch this checklist item to manual?')}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t(
                'checklistAutoOverrideDescription',
                'This checklist item is currently tracked automatically. Continuing will switch it to manual and disable automatic tracking for this item.'
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={updateManualState.isPending}>
              {t('actions.cancel', 'Cancel')}
            </AlertDialogCancel>
            <Button
              type="button"
              onClick={handleConfirmAutoOverride}
              disabled={updateManualState.isPending}
            >
              {t('checklistAutoOverrideConfirm', 'Switch to Manual')}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
