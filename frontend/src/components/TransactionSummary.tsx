import { useState, useEffect } from 'react';
import { CheckCircle } from 'lucide-react';
import axiosInstance from '@/api/axiosInstance';
import { getStagesForSide, enumToLabel, resolveStageIndex, isTerminatedStage } from '@/utils/stages';

interface TransactionSummaryProps {
  language: 'en' | 'fr';
  transactionId: string;
}

const translations = {
  en: {
    fileNumber: 'File',
    currentStage: 'Current Stage',
    stageDescription: 'Your transaction is in the inspection phase. The property inspection is scheduled and the report will be reviewed shortly.',
    timeline: 'Transaction Timeline',
    recentActivity: 'Recent Activity',
    requiredDocuments: 'Required Documents',
    viewAllDocuments: 'View All Documents',
    appointments: 'Appointments for This Transaction',
    requestAppointment: 'Request Appointment',
    noAppointments: 'No appointments scheduled yet',
    scheduleFirst: 'Request your first appointment',
    pending: 'Pending',
    approved: 'Approved',
    revisionRequested: 'Revision Requested',
    needsUpload: 'Needs Upload',
    confirmed: 'Confirmed',
    completed: 'Completed',
    loading: 'Loading your transaction details...',
    stage: 'Stage',
    of: 'of',
    noTimeline: 'No recent activity',
    // buy/sell stage arrays removed; use enums in src/utils/stages.ts
    stageDescriptions: [
      'Your offer has been submitted to the seller. Awaiting response.',
      'Congratulations! Your offer has been accepted. Moving to the next steps.',
      'Property inspection is scheduled. We will review the inspection report together.',
      'Your financing application is in progress. Stay in touch with your lender.',
      'Legal documents are being reviewed by your lawyer.',
      'Schedule your final property walkthrough before closing.',
      'Preparing for the final closing. Almost there!',
      'Transaction complete! Congratulations on your new property!',
    ],
    openedDate: 'Opened Date',
  },
  fr: {
    fileNumber: 'Dossier',
    currentStage: 'Étape actuelle',
    stageDescription: 'Votre transaction est en phase d\'inspection. L\'inspection de la propriété est prévue et le rapport sera examiné sous peu.',
    timeline: 'Chronologie de la transaction',
    recentActivity: 'Activité récente',
    requiredDocuments: 'Documents requis',
    viewAllDocuments: 'Voir tous les documents',
    appointments: 'Rendez-vous pour cette transaction',
    requestAppointment: 'Demander un rendez-vous',
    noAppointments: 'Aucun rendez-vous prévu',
    scheduleFirst: 'Demander votre premier rendez-vous',
    pending: 'En attente',
    approved: 'Approuvé',
    revisionRequested: 'Révision demandée',
    needsUpload: 'À télécharger',
    confirmed: 'Confirmé',
    completed: 'Terminé',
    loading: 'Chargement des détails de votre transaction...',
    stage: 'Étape',
    of: 'sur',
    noTimeline: 'Aucune activité récente',
    // buy/sell stage arrays removed; use enums in src/utils/stages.ts
    stageDescriptions: [
      'Votre offre a été soumise au vendeur. En attente de réponse.',
      'Félicitations! Votre offre a été acceptée. Passons aux prochaines étapes.',
      'L\'inspection de la propriété est prévue. Nous examinerons le rapport ensemble.',
      'Votre demande de financement est en cours. Restez en contact avec votre prêteur.',
      'Les documents juridiques sont examinés par votre avocat.',
      'Planifiez votre visite finale de la propriété avant la clôture.',
      'Préparation de la clôture finale. Presque terminé!',
      'Transaction terminée! Félicitations pour votre nouvelle propriété!',
    ],
    openedDate: 'Date d\'ouverture',
  },
};

export function TransactionSummary({ language, transactionId }: TransactionSummaryProps) {
  const [isLoading, setIsLoading] = useState(true);
  const [transaction, setTransaction] = useState<any | null>(null);

  const t = translations[language];
  
  const stageEnums = getStagesForSide(transaction?.side);
  const stages = stageEnums.map(enumToLabel);
  const stageIndex = resolveStageIndex(transaction?.currentStage, stageEnums);
  const isTerminated = isTerminatedStage(transaction?.currentStage, stageEnums) || transaction?.status === 'terminated';
  const displayStage = stageIndex >= 0 ? stageIndex + 1 : 1;
  const totalStages = ((transaction?.totalStages ?? stages.length) || 1);

  useEffect(() => {
    if (!transactionId) {
      console.error('Missing transactionId for TransactionSummary; skipping fetch');
      setIsLoading(false);
      return;
    }

    const loadTransaction = async () => {
      try {
        const res = await axiosInstance.get(`/transactions/${transactionId}`, {
          headers: { 'x-broker-id': 'BROKER1' }, // temporary
        });
        setTransaction(res.data);
      } catch (err) {
        console.error('Failed to fetch transaction', err);
      } finally {
        setIsLoading(false);
      }
    };

    loadTransaction();
  }, [transactionId]);

  // We intentionally show only BE-provided transaction fields here.

  if (isLoading) {
    return (
      <div className="space-y-6">
        {/* Header Skeleton */}
        <div className="space-y-2">
          <div className="h-8 bg-gray-200 rounded-lg w-3/4 animate-pulse" />
          <div className="h-5 bg-gray-200 rounded-lg w-1/4 animate-pulse" />
        </div>

        {/* Stage Tracker Skeleton */}
        <div className="p-6 rounded-xl shadow-md" style={{ backgroundColor: '#FFFFFF' }}>
          <div className="h-6 bg-gray-200 rounded-lg w-1/3 mb-4 animate-pulse" />
          <div className="h-3 bg-gray-200 rounded-full mb-4 animate-pulse" />
          <div className="flex justify-between">
            {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
              <div key={i} className="w-8 h-8 bg-gray-200 rounded-full animate-pulse" />
            ))}
          </div>
        </div>

        {/* Content Skeleton */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 p-6 rounded-xl shadow-md" style={{ backgroundColor: '#FFFFFF' }}>
            <div className="h-6 bg-gray-200 rounded-lg w-1/3 mb-4 animate-pulse" />
            <div className="space-y-4">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-20 bg-gray-100 rounded-lg animate-pulse" />
              ))}
            </div>
          </div>
          <div className="space-y-6">
            <div className="p-6 rounded-xl shadow-md" style={{ backgroundColor: '#FFFFFF' }}>
              <div className="h-6 bg-gray-200 rounded-lg w-1/2 mb-4 animate-pulse" />
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-16 bg-gray-100 rounded-lg animate-pulse" />
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-center py-8">
          <div
            className="w-12 h-12 border-4 border-t-transparent rounded-full animate-spin"
            style={{ borderColor: '#FF6B01', borderTopColor: 'transparent' }}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 style={{ color: '#353535' }}>
          {transaction?.propertyAddress?.street ?? ''}
        </h1>
        <p style={{ color: '#353535', opacity: 0.7 }}>
          {t.fileNumber} #{transaction?.transactionId ?? ''}
        </p>
      </div>

      {/* Stage Progress Tracker */}
      <div
        className="p-6 rounded-xl shadow-md"
        style={{ backgroundColor: '#FFFFFF' }}
        role="region"
        aria-label="Transaction progress tracker"
      >
        <div className="mb-6">
          <h2 style={{ color: '#353535' }} className="mb-2">
            {t.currentStage}
          </h2>
          <p style={{ color: '#353535', opacity: 0.7 }}>
            {t.stage} {displayStage} {t.of} {transaction?.totalStages ?? stages.length}:{' '}
            {stages[stageIndex] ?? enumToLabel(stageEnums[stageIndex])}
          </p>
        </div>

        {/* Progress Bar */}
        <div className="mb-6">
          <div className="w-full h-3 bg-gray-200 rounded-full overflow-hidden">
              <div
                className="h-full transition-all duration-500 rounded-full"
                style={{
                  backgroundColor: isTerminated ? '#9ca3af' : '#FF6B01',
                  width: `${(displayStage / totalStages) * 100}%`,
                }}
                role="progressbar"
                aria-valuenow={displayStage}
                aria-valuemin={0}
                aria-valuemax={totalStages}
                aria-label={`Progress: Stage ${displayStage} of ${totalStages}`}
              />
          </div>
        </div>

        {/* Stage Dots */}
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 md:gap-2 mb-6">
          {stageEnums.map((stageEnum, index) => {
            const stageNumber = index + 1;
            const isCompleted = index < stageIndex;
            const isCurrent = index === stageIndex;
            const label = enumToLabel(stageEnum);

            return (
              <div key={stageEnum ?? `stage-${index}`} className="flex md:flex-col items-center md:items-center flex-1 gap-3 md:gap-2">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center transition-all flex-shrink-0 ${
                    isCurrent ? 'ring-4 ring-[#FF6B01] ring-opacity-30' : ''
                  }`}
                  style={{
                    backgroundColor: isTerminated ? '#e5e7eb' : isCompleted || isCurrent ? '#FF6B01' : '#e5e7eb',
                    color: isTerminated ? '#9ca3af' : isCompleted || isCurrent ? '#FFFFFF' : '#9ca3af',
                  }}
                  role="button"
                  tabIndex={0}
                  aria-label={`${label} - ${isCompleted ? 'completed' : isCurrent ? 'current stage' : 'upcoming'}`}
                >
                  {isCompleted ? (
                    <CheckCircle className="w-5 h-5" />
                  ) : (
                    <span style={{ fontSize: '0.75rem' }}>{stageNumber}</span>
                  )}
                </div>
                <span
                  style={{
                    color: isCurrent ? (isTerminated ? '#9ca3af' : '#FF6B01') : '#353535',
                    fontSize: '0.75rem',
                    opacity: !isCompleted && !isCurrent ? 0.5 : 1,
                    textAlign: 'left',
                  }}
                  className="flex-1 md:flex-none md:text-center"
                >
                  {label}
                </span>
              </div>
            );
          })}
        </div>

        {/* Stage Description */}
        <div
          className="p-4 rounded-lg border-l-4"
          style={{
            backgroundColor: '#FFF5F0',
            borderLeftColor: '#FF6B01',
          }}
        >
          <p style={{ color: '#353535' }}>
            {transaction ? t.stageDescriptions[stageIndex] : ''}
          </p>
        </div>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      {/* Main Content - Show essential BE data only */}
      <div className="lg:col-span-2 p-6 rounded-xl shadow-md" style={{ backgroundColor: '#FFFFFF' }}>
        <h2 style={{ color: '#353535' }} className="mb-2">
          {transaction?.propertyAddress?.street ?? ''}
        </h2>
        <p style={{ color: '#353535', opacity: 0.7 }} className="mb-4">
          {t.fileNumber} #{transaction?.transactionId ?? ''}
        </p>

        <div className="mb-4">
          <p style={{ color: '#353535', opacity: 0.7 }} className="mb-1">
            {t.stage} {transaction?.currentStage ?? 1} {t.of} {transaction?.totalStages ?? stages.length}
          </p>
          <div className="w-full h-3 bg-gray-200 rounded-full overflow-hidden mt-2">
            <div
              className="h-full rounded-full"
              style={{
                backgroundColor: '#FF6B01',
                width: `${((transaction?.currentStage ?? 0) / ((transaction?.totalStages ?? stages.length) || 1)) * 100}%`,
              }}
              role="progressbar"
              aria-valuenow={transaction?.currentStage ?? 0}
              aria-valuemin={0}
              aria-valuemax={((transaction?.totalStages ?? stages.length) || 1)}
            />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
          <div>
            <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }} className="mb-1">{t.stage}</p>
            <p style={{ color: '#353535' }}>{stages[(transaction?.currentStage ?? 1) - 1] ?? ''}</p>
          </div>
          <div>
            <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }} className="mb-1">{t.currentStage}</p>
            <p style={{ color: '#353535' }}>{transaction?.currentStage ?? ''}</p>
          </div>
          <div>
            <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }} className="mb-1">{t.openedDate}</p>
            <p style={{ color: '#353535' }}>{transaction?.openedAt ?? ''}</p>
          </div>
        </div>
      </div>
      </div>
    </div>
  );
}