import { useState, useEffect, useRef } from 'react';
import {
  ChevronLeft,
  CheckCircle,
  Clock,
  // AlertCircle,
  FileText,
  Calendar,
  MessageSquare,
  Edit,
  X,
  Check,
  XCircle,
  // Download,
  // Eye,
  Plus,
} from 'lucide-react';
// import { StageUpdateModal } from './StageUpdateModal';
// import { RequestDocumentModal } from './RequestDocumentModal';
// import { CreateAppointmentModal, AppointmentFormData } from './CreateAppointmentModal';
import { Toast } from './Toast';
import axiosInstance from "@/api/axiosInstace";

interface TransactionDetailProps {
  transactionId: string;
  language: 'en' | 'fr';
  onNavigate: (route: string) => void;
}

type TabType = 'timeline' | 'documents' | 'appointments' | 'notes';

const translations = {
  en: {
    backToTransactions: 'Back to Transactions',
    transactionDetails: 'Transaction Details',
    status: 'Status',
    client: 'Client',
    broker: 'Broker',
    openedDate: 'Opened Date',
    active: 'Active',
    closed: 'Closed',
    terminated: 'Terminated',
    stage: 'Stage',
    of: 'of',
    changeStage: 'Change Stage',
    timeline: 'Timeline',
    documents: 'Documents',
    appointments: 'Appointments',
    internalNotes: 'Internal Notes',
    selectNewStage: 'Select New Stage',
    addNote: 'Add Note',
    noteOptional: 'Add note (optional)',
    cancel: 'Cancel',
    save: 'Save',
    saveStage: 'Update Stage',
    addNewNote: 'Add New Note',
    noteContent: 'Note content',
    noTimeline: 'No timeline entries yet',
    noDocuments: 'No documents uploaded',
    noAppointments: 'No appointments scheduled',
    noNotes: 'No internal notes yet',
    document: 'Document',
    type: 'Type',
    statusLabel: 'Status',
    lastUpdated: 'Last Updated',
    actions: 'Actions',
    approve: 'Approve',
    requestRevision: 'Request Revision',
    pending: 'Pending',
    approved: 'Approved',
    needsRevision: 'Needs Revision',
    dateTime: 'Date & Time',
    appointmentType: 'Type',
    appointmentStatus: 'Status',
    notes: 'Notes',
    confirm: 'Confirm',
    proposeNewTime: 'Propose New Time',
    markCompleted: 'Mark Completed',
    confirmed: 'Confirmed',
    completed: 'Completed',
    timestamp: 'Timestamp',
    description: 'Description',
    author: 'Author',
    buyStages: [
      'Offer Submitted',
      'Offer Accepted',
      'Inspection',
      'Financing',
      'Legal Review',
      'Final Walkthrough',
      'Closing',
      'Complete',
    ],
    sellStages: [
      'Listed',
      'Marketing',
      'Offer Received',
      'Negotiations',
      'Accepted',
      'Legal Review',
      'Closing',
      'Complete',
    ],
  },
  fr: {
    backToTransactions: 'Retour aux transactions',
    transactionDetails: 'Détails de la transaction',
    status: 'Statut',
    client: 'Client',
    broker: 'Courtier',
    openedDate: 'Date d\'ouverture',
    active: 'Actif',
    closed: 'Fermé',
    terminated: 'Résilié',
    stage: 'Étape',
    of: 'sur',
    changeStage: 'Changer l\'étape',
    timeline: 'Chronologie',
    documents: 'Documents',
    appointments: 'Rendez-vous',
    internalNotes: 'Notes internes',
    selectNewStage: 'Sélectionner une nouvelle étape',
    addNote: 'Ajouter une note',
    noteOptional: 'Ajouter une note (optionnel)',
    cancel: 'Annuler',
    save: 'Sauvegarder',
    saveStage: 'Mettre à jour l\'étape',
    addNewNote: 'Ajouter une nouvelle note',
    noteContent: 'Contenu de la note',
    noTimeline: 'Aucune entrée de chronologie',
    noDocuments: 'Aucun document téléchargé',
    noAppointments: 'Aucun rendez-vous prévu',
    noNotes: 'Aucune note interne',
    document: 'Document',
    type: 'Type',
    statusLabel: 'Statut',
    lastUpdated: 'Dernière mise à jour',
    actions: 'Actions',
    approve: 'Approuver',
    requestRevision: 'Demander révision',
    pending: 'En attente',
    approved: 'Approuvé',
    needsRevision: 'Révision requise',
    dateTime: 'Date et heure',
    appointmentType: 'Type',
    appointmentStatus: 'Statut',
    notes: 'Notes',
    confirm: 'Confirmer',
    proposeNewTime: 'Proposer nouveau créneau',
    markCompleted: 'Marquer comme terminé',
    confirmed: 'Confirmé',
    completed: 'Terminé',
    timestamp: 'Horodatage',
    description: 'Description',
    author: 'Auteur',
    buyStages: [
      'Offre soumise',
      'Offre acceptée',
      'Inspection',
      'Financement',
      'Révision légale',
      'Visite finale',
      'Clôture',
      'Complet',
    ],
    sellStages: [
      'Inscrit',
      'Marketing',
      'Offre reçue',
      'Négociations',
      'Accepté',
      'Révision légale',
      'Clôture',
      'Complet',
    ],
  },
};



export function TransactionDetail({ transactionId, language, onNavigate }: TransactionDetailProps) {
  const [activeTab, setActiveTab] = useState<TabType>('timeline');
  const [showStageModal, setShowStageModal] = useState(false);
  const [showNoteModal, setShowNoteModal] = useState(false);
  const [showRequestDocModal, setShowRequestDocModal] = useState(false);
  const [showCreateAppointmentModal, setShowCreateAppointmentModal] = useState(false);
  const [transaction, setTransaction] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);

  const [selectedStage, setSelectedStage] = useState<number>(1);
  const [stageNote, setStageNote] = useState('');
  const [noteContent, setNoteContent] = useState('');
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  
  const modalRef = useRef<HTMLDivElement>(null);

  const t = translations[language];
  // TODO: replace when timeline API exists
  const timeline: any[] = [];
  // Backend not yet wired for these lists; keep empty until API exists
  const documents: any[] = [];
  const appointments: any[] = [];
  const notes: any[] = [];

  // Close modal on ESC key
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setShowStageModal(false);
        setShowNoteModal(false);
        setShowRequestDocModal(false);
        setShowCreateAppointmentModal(false);
      }
    };

    if (showStageModal || showNoteModal || showRequestDocModal || showCreateAppointmentModal) {
      document.addEventListener('keydown', handleEscape);
      // Trap focus in modal
      modalRef.current?.focus();
    }

    return () => {
      document.removeEventListener('keydown', handleEscape);
    };
  }, [showStageModal, showNoteModal, showRequestDocModal, showCreateAppointmentModal]);

  useEffect(() => {
    if (!transactionId) {
      console.error('Missing transactionId for TransactionDetail; skipping fetch');
      setLoading(false);
      return;
    }

    const loadTransaction = async () => {
      try {
        const res = await axiosInstance.get(`/transactions/${transactionId}`, {
          headers: { "x-broker-id": "BROKER1" } // remove after Auth0
        });

        setTransaction(res.data);
        setSelectedStage(res.data.currentStage || 1);
      } catch (err) {
        console.error("Failed to load transaction:", err);
      } finally {
        setLoading(false);
      }
    };

    loadTransaction();
  }, [transactionId]);

  const handleSaveStage = () => {
    // In real app, would make API call
    console.log('Saving stage:', selectedStage, 'Note:', stageNote);
    setShowStageModal(false);
    setStageNote('');
    setToastMessage('Stage updated successfully');
    setShowToast(true);
  };

  const handleSaveNote = () => {
    // In real app, would make API call
    console.log('Saving note:', noteContent);
    setShowNoteModal(false);
    setNoteContent('');
    setToastMessage('Note added successfully');
    setShowToast(true);
  };

  const handleRequestDocument = (documentTitle: string, instructions: string, stage: string) => {
    // In real app, would make API call to add document request to client's checklist
    console.log('Requesting document:', { documentTitle, instructions, stage });
    setToastMessage(language === 'en' ? 'Document request sent successfully' : 'Demande de document envoyée avec succès');
    setShowToast(true);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active':
        return '#10b981';
      case 'closed':
        return '#6b7280';
      case 'terminated':
        return '#ef4444';
      default:
        return '#353535';
    }
  };

  const getDocumentStatusColor = (status: string) => {
    switch (status) {
      case 'approved':
        return '#10b981';
      case 'pending':
        return '#f59e0b';
      case 'needs_revision':
        return '#ef4444';
      default:
        return '#353535';
    }
  };

  const getTimelineIcon = (type: string) => {
    switch (type) {
      case 'stage_change':
        return <CheckCircle className="w-5 h-5" style={{ color: '#10b981' }} />;
      case 'document_upload':
        return <FileText className="w-5 h-5" style={{ color: '#3b82f6' }} />;
      case 'appointment':
        return <Calendar className="w-5 h-5" style={{ color: '#f59e0b' }} />;
      case 'note':
        return <MessageSquare className="w-5 h-5" style={{ color: '#8b5cf6' }} />;
      default:
        return <Clock className="w-5 h-5" style={{ color: '#353535' }} />;
    }
  };

  if (loading) return <p>Loading…</p>;
  if (!transaction) return <p>Transaction not found.</p>;

  const stages = transaction.side === 'BUY_SIDE' ? t.buyStages : t.sellStages;

  return (
    <div className="space-y-6">
      {/* Back Button */}
      <button
        onClick={() => onNavigate('/transactions')}
        className="flex items-center gap-2 hover:opacity-70 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 rounded px-2 py-1 transition-opacity"
        style={{ color: '#FF6B01' }}
      >
        <ChevronLeft className="w-5 h-5" />
        {t.backToTransactions}
      </button>

      {/* Transaction Header */}
      <div
        className="p-6 rounded-xl shadow-md"
        style={{ backgroundColor: '#FFFFFF' }}
      >
        <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4 mb-6">
          <div>
            <h1 style={{ color: '#353535' }} className="mb-2">
              {t.transactionDetails} #{transaction.transactionId}
            </h1>
            <p style={{ color: '#353535', opacity: 0.7 }}>
              {transaction.propertyAddress?.street}
            </p>
          </div>
          <span
            className="px-4 py-2 rounded-full self-start"
            style={{
              backgroundColor: `${getStatusColor(transaction.status)}20`,
              color: getStatusColor(transaction.status),
            }}
          >
            {t[transaction.status as 'active' | 'closed' | 'terminated']}
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }} className="mb-1">
              {t.client}
            </p>
            <p style={{ color: '#353535' }}>{transaction.clientId}</p>
          </div>
          <div>
            <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }} className="mb-1">
              {t.broker}
            </p>
            <p style={{ color: '#353535' }}>{transaction.brokerId}</p>
          </div>
          <div>
            <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }} className="mb-1">
              {t.openedDate}
            </p>
            <p style={{ color: '#353535' }}>{transaction.openedAt}</p>
          </div>
        </div>
      </div>

      {/* Stage Tracker */}
      <div
        className="p-6 rounded-xl shadow-md"
        style={{ backgroundColor: '#FFFFFF' }}
      >
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 style={{ color: '#353535' }}>
              {t.stage} {selectedStage} {t.of} {transaction.totalStages}
            </h2>
            <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}>
              {stages[selectedStage - 1]}
            </p>
          </div>
          <button
            disabled
            title="Stage updates are disabled in DEV"
            className="px-4 py-2 rounded-lg disabled:opacity-60 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all flex items-center gap-2"
            style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
          >
            <Edit className="w-4 h-4" />
            {t.changeStage}
          </button>
        </div>

        {/* Progress Bar */}
        <div className="mb-4">
          <div className="w-full h-3 bg-gray-200 rounded-full overflow-hidden">
                <div
                  className="h-full transition-all duration-500 rounded-full"
                  style={{
                    backgroundColor: '#FF6B01',
                    width: `${(selectedStage / transaction.totalStages) * 100}%`,
                  }}
                  role="progressbar"
                  aria-valuenow={selectedStage}
                  aria-valuemin={0}
                  aria-valuemax={transaction.totalStages}
                />
          </div>
        </div>

        {/* Stage Dots */}
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 md:gap-0">
          {stages.map((stage, index) => {
            const stageNumber = index + 1;
            const isCompleted = stageNumber < selectedStage;
            const isCurrent = stageNumber === selectedStage;

            return (
              <div key={index} className="flex md:flex-col items-center md:items-center flex-1 gap-3 md:gap-0">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center md:mb-2 transition-all flex-shrink-0 ${
                    isCurrent ? 'ring-4 ring-[#FF6B01] ring-opacity-30' : ''
                  }`}
                  style={{
                    backgroundColor: isCompleted || isCurrent ? '#FF6B01' : '#e5e7eb',
                    color: isCompleted || isCurrent ? '#FFFFFF' : '#9ca3af',
                  }}
                >
                  {isCompleted ? (
                    <CheckCircle className="w-5 h-5" />
                  ) : (
                    <span style={{ fontSize: '0.75rem' }}>{stageNumber}</span>
                  )}
                </div>
                <span
                  style={{
                    color: isCurrent ? '#FF6B01' : '#353535',
                    fontSize: '0.75rem',
                    opacity: !isCompleted && !isCurrent ? 0.5 : 1,
                    textAlign: 'center',
                  }}
                  className="flex-1 md:flex-none text-left md:text-center"
                >
                  {stage}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Tabs Section */}
      <div
        className="rounded-xl shadow-md overflow-hidden"
        style={{ backgroundColor: '#FFFFFF' }}
      >
        {/* Tab Headers */}
        <div
          className="flex border-b border-gray-200 overflow-x-auto"
          role="tablist"
          aria-label="Transaction information tabs"
        >
          {(['timeline', 'documents', 'appointments', 'notes'] as TabType[]).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              role="tab"
              aria-selected={activeTab === tab}
              aria-controls={`${tab}-panel`}
              className="px-6 py-4 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-[#FF6B01] transition-colors whitespace-nowrap"
              style={{
                borderBottom: activeTab === tab ? `3px solid #FF6B01` : 'none',
                color: activeTab === tab ? '#FF6B01' : '#353535',
              }}
            >
              {t[tab as keyof typeof t] as string}
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div className="p-6">
          {/* Timeline Tab */}
          {activeTab === 'timeline' && (
            <div id="timeline-panel" role="tabpanel" aria-labelledby="timeline-tab">
              {timeline.length === 0 ? (
                <div className="text-center py-12">
                  <Clock className="w-12 h-12 mx-auto mb-4" style={{ color: '#353535', opacity: 0.3 }} />
                  <p style={{ color: '#353535', opacity: 0.7 }}>{t.noTimeline}</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {timeline.map((entry) => (
                    <div
                      key={entry.id}
                      className="flex items-start gap-4 p-4 rounded-lg border border-gray-100"
                    >
                      <div className="flex-shrink-0 mt-1">{getTimelineIcon(entry.type)}</div>
                      <div className="flex-1">
                        <p style={{ color: '#353535' }} className="mb-1">
                          {entry.description}
                        </p>
                        <div className="flex items-center gap-2">
                          <Clock className="w-4 h-4" style={{ color: '#353535', opacity: 0.5 }} />
                          <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}>
                            {entry.timestamp}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Documents Tab */}
          {activeTab === 'documents' && (
            <div id="documents-panel" role="tabpanel" aria-labelledby="documents-tab">
              <div className="mb-4">
                <button
                  disabled
                  title="Document requests disabled in DEV"
                  className="px-4 py-2 rounded-lg disabled:opacity-60 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all flex items-center gap-2"
                  style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
                >
                  <Plus className="w-4 h-4" />
                  {language === 'en' ? 'Request Document' : 'Demander un document'}
                </button>
              </div>

              {documents.length === 0 ? (
                <div className="text-center py-12">
                  <FileText className="w-12 h-12 mx-auto mb-4" style={{ color: '#353535', opacity: 0.3 }} />
                  <p style={{ color: '#353535', opacity: 0.7 }}>{t.noDocuments}</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {documents.map((doc) => (
                    <div
                      key={doc.id}
                      className="p-4 rounded-lg border border-gray-100 flex flex-col md:flex-row md:items-center justify-between gap-4"
                    >
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <FileText className="w-5 h-5" style={{ color: '#FF6B01' }} />
                          <p style={{ color: '#353535' }}>{doc.name}</p>
                        </div>
                        <div className="flex flex-wrap items-center gap-4">
                          <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}>
                            {t.type}: {doc.type}
                          </p>
                          <span
                            className="px-3 py-1 rounded-full"
                            style={{
                              backgroundColor: `${getDocumentStatusColor(doc.status)}20`,
                              color: getDocumentStatusColor(doc.status),
                              fontSize: '0.875rem',
                            }}
                          >
                            {t[doc.status as 'pending' | 'approved' | 'needs_revision']}
                          </span>
                          <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}>
                            {t.lastUpdated}: {doc.lastUpdated}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {doc.status === 'pending' && (
                          <>
                            <button
                              className="px-4 py-2 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#10b981] focus:ring-offset-2 transition-all flex items-center gap-2"
                              style={{ backgroundColor: '#10b981', color: '#FFFFFF' }}
                              aria-label={`Approve ${doc.name}`}
                            >
                              <Check className="w-4 h-4" />
                              {t.approve}
                            </button>
                            <button
                              className="px-4 py-2 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#ef4444] focus:ring-offset-2 transition-all flex items-center gap-2"
                              style={{ backgroundColor: '#ef4444', color: '#FFFFFF' }}
                              aria-label={`Request revision for ${doc.name}`}
                            >
                              <XCircle className="w-4 h-4" />
                              {t.requestRevision}
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Appointments Tab */}
          {activeTab === 'appointments' && (
            <div id="appointments-panel" role="tabpanel" aria-labelledby="appointments-tab">
              <div className="mb-4">
                <button
                  disabled
                  title="Creating appointments is disabled in DEV"
                  className="px-4 py-2 rounded-lg disabled:opacity-60 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all flex items-center gap-2"
                  style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
                >
                  <Plus className="w-4 h-4" />
                  {language === 'en' ? 'Create Appointment' : 'Créer un rendez-vous'}
                </button>
              </div>

              {appointments.length === 0 ? (
                <div className="text-center py-12">
                  <Calendar className="w-12 h-12 mx-auto mb-4" style={{ color: '#353535', opacity: 0.3 }} />
                  <p style={{ color: '#353535', opacity: 0.7 }}>{t.noAppointments}</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {appointments.map((apt) => (
                    <div
                      key={apt.id}
                      className="p-4 rounded-lg border border-gray-100 flex flex-col md:flex-row md:items-center justify-between gap-4"
                    >
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <Calendar className="w-5 h-5" style={{ color: '#FF6B01' }} />
                          <p style={{ color: '#353535' }}>{apt.type}</p>
                        </div>
                        <div className="space-y-1">
                          <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}>
                            {t.dateTime}: {apt.dateTime}
                          </p>
                          <div className="flex items-center gap-2">
                            <span
                              className="px-3 py-1 rounded-full"
                              style={{
                                backgroundColor:
                                  apt.status === 'completed'
                                    ? '#10b98120'
                                    : apt.status === 'confirmed'
                                    ? '#3b82f620'
                                    : '#f59e0b20',
                                color:
                                  apt.status === 'completed'
                                    ? '#10b981'
                                    : apt.status === 'confirmed'
                                    ? '#3b82f6'
                                    : '#f59e0b',
                                fontSize: '0.875rem',
                              }}
                            >
                              {t[apt.status as 'pending' | 'confirmed' | 'completed']}
                            </span>
                          </div>
                          {apt.notes && (
                            <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}>
                              {t.notes}: {apt.notes}
                            </p>
                          )}
                        </div>
                      </div>
                      <div className="flex flex-wrap items-center gap-2">
                        {apt.status === 'pending' && (
                          <button
                            className="px-4 py-2 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#10b981] focus:ring-offset-2 transition-all"
                            style={{ backgroundColor: '#10b981', color: '#FFFFFF' }}
                            aria-label={`Confirm ${apt.type} appointment`}
                          >
                            {t.confirm}
                          </button>
                        )}
                        {apt.status === 'confirmed' && (
                          <button
                            className="px-4 py-2 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all"
                            style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
                            aria-label={`Mark ${apt.type} appointment as completed`}
                          >
                            {t.markCompleted}
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Internal Notes Tab */}
          {activeTab === 'notes' && (
            <div id="notes-panel" role="tabpanel" aria-labelledby="notes-tab">
              <div className="mb-4">
                <button
                  onClick={() => setShowNoteModal(true)}
                  className="px-4 py-2 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all flex items-center gap-2"
                  style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
                >
                  <MessageSquare className="w-4 h-4" />
                  {t.addNewNote}
                </button>
              </div>

              {notes.length === 0 ? (
                <div className="text-center py-12">
                  <MessageSquare className="w-12 h-12 mx-auto mb-4" style={{ color: '#353535', opacity: 0.3 }} />
                  <p style={{ color: '#353535', opacity: 0.7 }}>{t.noNotes}</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {notes.map((note) => (
                    <div key={note.id} className="p-4 rounded-lg border border-gray-100">
                      <div className="flex items-start justify-between gap-4 mb-2">
                        <div className="flex items-center gap-2">
                          <MessageSquare className="w-5 h-5" style={{ color: '#8b5cf6' }} />
                          <p style={{ color: '#353535' }}>{note.author}</p>
                        </div>
                        <div className="flex items-center gap-2">
                          <Clock className="w-4 h-4" style={{ color: '#353535', opacity: 0.5 }} />
                          <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}>
                            {note.timestamp}
                          </p>
                        </div>
                      </div>
                      <p style={{ color: '#353535', opacity: 0.9 }}>{note.content}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Change Stage Modal */}
      {showStageModal && (
        <div
          className="fixed inset-0 bg-gray-900 bg-opacity-20 flex items-center justify-center z-50 p-4"
          onClick={() => setShowStageModal(false)}
        >
          <div
            ref={modalRef}
            className="rounded-xl shadow-xl max-w-md w-full p-6"
            style={{ backgroundColor: '#FFFFFF' }}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="stage-modal-title"
            tabIndex={-1}
          >
            <div className="flex items-center justify-between mb-6">
              <h2 id="stage-modal-title" style={{ color: '#353535' }}>
                {t.changeStage}
              </h2>
              <button
                onClick={() => setShowStageModal(false)}
                className="p-1 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-[#FF6B01]"
                aria-label="Close modal"
              >
                <X className="w-5 h-5" style={{ color: '#353535' }} />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label
                  htmlFor="stage-select"
                  style={{ color: '#353535', fontSize: '0.875rem' }}
                  className="block mb-2"
                >
                  {t.selectNewStage}
                </label>
                <select
                  id="stage-select"
                  value={selectedStage}
                  onChange={(e) => setSelectedStage(Number(e.target.value))}
                  className="w-full p-3 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent"
                  style={{ color: '#353535' }}
                >
                  {stages.map((stage, index) => (
                    <option key={index} value={index + 1}>
                      {stage}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label
                  htmlFor="stage-note"
                  style={{ color: '#353535', fontSize: '0.875rem' }}
                  className="block mb-2"
                >
                  {t.noteOptional}
                </label>
                <textarea
                  id="stage-note"
                  value={stageNote}
                  onChange={(e) => setStageNote(e.target.value)}
                  rows={3}
                  className="w-full p-3 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent resize-none"
                  style={{ color: '#353535' }}
                  placeholder={t.noteOptional}
                />
              </div>

              <div className="flex items-center gap-3 pt-4">
                <button
                  onClick={() => setShowStageModal(false)}
                  className="flex-1 px-4 py-2 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-300 focus:ring-offset-2 transition-colors border-2 border-gray-200"
                  style={{ color: '#353535' }}
                >
                  {t.cancel}
                </button>
                <button
                  onClick={() => {
                    const title = (document.getElementById("document-title") as HTMLInputElement).value;
                    const instructions = (document.getElementById("document-instructions") as HTMLTextAreaElement).value;
                    const stage = (document.getElementById("document-stage") as HTMLSelectElement).value;
                    handleRequestDocument(title, instructions, stage);
                    setShowRequestDocModal(false);
                  }}
                  className="flex-1 px-4 py-2 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all"
                  style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
                >
                  {language === 'en' ? 'Request Document' : 'Demander un document'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Add Note Modal */}
      {showNoteModal && (
        <div
          className="fixed inset-0 bg-gray-900 bg-opacity-20 flex items-center justify-center z-50 p-4"
          onClick={() => setShowNoteModal(false)}
        >
          <div
            ref={modalRef}
            className="rounded-xl shadow-xl max-w-md w-full p-6"
            style={{ backgroundColor: '#FFFFFF' }}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="note-modal-title"
            tabIndex={-1}
          >
            <div className="flex items-center justify-between mb-6">
              <h2 id="note-modal-title" style={{ color: '#353535' }}>
                {t.addNewNote}
              </h2>
              <button
                onClick={() => setShowNoteModal(false)}
                className="p-1 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-[#FF6B01]"
                aria-label="Close modal"
              >
                <X className="w-5 h-5" style={{ color: '#353535' }} />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label
                  htmlFor="note-content"
                  style={{ color: '#353535', fontSize: '0.875rem' }}
                  className="block mb-2"
                >
                  {t.noteContent}
                </label>
                <textarea
                  id="note-content"
                  value={noteContent}
                  onChange={(e) => setNoteContent(e.target.value)}
                  rows={5}
                  className="w-full p-3 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent resize-none"
                  style={{ color: '#353535' }}
                  placeholder={t.noteContent}
                />
              </div>

              <div className="flex items-center gap-3 pt-4">
                <button
                  onClick={() => setShowNoteModal(false)}
                  className="flex-1 px-4 py-2 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-300 focus:ring-offset-2 transition-colors border-2 border-gray-200"
                  style={{ color: '#353535' }}
                >
                  {t.cancel}
                </button>
                <button
                  onClick={handleSaveNote}
                  className="flex-1 px-4 py-2 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all"
                  style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
                >
                  {t.save}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Request Document Modal */}
      {showRequestDocModal && (
        <div
          className="fixed inset-0 bg-gray-900 bg-opacity-20 flex items-center justify-center z-50 p-4"
          onClick={() => setShowRequestDocModal(false)}
        >
          <div
            ref={modalRef}
            className="rounded-xl shadow-xl max-w-md w-full p-6"
            style={{ backgroundColor: '#FFFFFF' }}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="request-doc-modal-title"
            tabIndex={-1}
          >
            <div className="flex items-center justify-between mb-6">
              <h2 id="request-doc-modal-title" style={{ color: '#353535' }}>
                {language === 'en' ? 'Request Document' : 'Demander un document'}
              </h2>
              <button
                onClick={() => setShowRequestDocModal(false)}
                className="p-1 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-[#FF6B01]"
                aria-label="Close modal"
              >
                <X className="w-5 h-5" style={{ color: '#353535' }} />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label
                  htmlFor="document-title"
                  style={{ color: '#353535', fontSize: '0.875rem' }}
                  className="block mb-2"
                >
                  {language === 'en' ? 'Document Title' : 'Titre du document'}
                </label>
                <input
                  id="document-title"
                  type="text"
                  className="w-full p-3 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent"
                  style={{ color: '#353535' }}
                  placeholder={language === 'en' ? 'Enter document title' : 'Entrez le titre du document'}
                />
              </div>

              <div>
                <label
                  htmlFor="document-instructions"
                  style={{ color: '#353535', fontSize: '0.875rem' }}
                  className="block mb-2"
                >
                  {language === 'en' ? 'Instructions' : 'Instructions'}
                </label>
                <textarea
                  id="document-instructions"
                  rows={3}
                  className="w-full p-3 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent resize-none"
                  style={{ color: '#353535' }}
                  placeholder={language === 'en' ? 'Enter instructions' : 'Entrez les instructions'}
                />
              </div>

              <div>
                <label
                  htmlFor="document-stage"
                  style={{ color: '#353535', fontSize: '0.875rem' }}
                  className="block mb-2"
                >
                  {language === 'en' ? 'Stage' : 'Étape'}
                </label>
                <select
                  id="document-stage"
                  className="w-full p-3 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent"
                  style={{ color: '#353535' }}
                >
                  {stages.map((stage, index) => (
                    <option key={index} value={index + 1}>
                      {stage}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex items-center gap-3 pt-4">
                <button
                  onClick={() => setShowRequestDocModal(false)}
                  className="flex-1 px-4 py-2 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-300 focus:ring-offset-2 transition-colors border-2 border-gray-200"
                  style={{ color: '#353535' }}
                >
                  {t.cancel}
                </button>
                <button
                  onClick={handleSaveNote}
                  className="flex-1 px-4 py-2 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all"
                  style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
                >
                  {language === 'en' ? 'Request Document' : 'Demander un document'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Create Appointment Modal */}
      {showCreateAppointmentModal && (
        <CreateAppointmentModal
          isOpen={showCreateAppointmentModal}
          onClose={() => setShowCreateAppointmentModal(false)}
          onSubmit={(formData: AppointmentFormData) => {
            console.log('Creating appointment:', formData);
            setToastMessage(language === 'en' ? 'Appointment request sent to client' : 'Demande de rendez-vous envoyée au client');
            setShowToast(true);
          }}
          language={language}
          fromTransaction={true}
          prefilledClientId={transaction.clientId}
          prefilledClientName={null}
          prefilledTransactionId={transaction.transactionId}
          prefilledTransactionAddress={transaction.propertyAddress?.street}
        />
      )}

      {/* Toast */}
      {showToast && (
        <Toast
          message={toastMessage}
          onClose={() => setShowToast(false)}
        />
      )}
    </div>
  );
}