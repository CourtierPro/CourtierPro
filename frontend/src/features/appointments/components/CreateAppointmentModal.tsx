import React, { useState, useEffect, useRef, useMemo } from 'react';
import {
  X,
  Calendar,
  Clock,
  FileText,
  MessageSquare,
  Send,
  User,
  MapPin,
  ChevronDown,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Textarea } from '@/shared/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select';
import { useClientsForDisplay, type ClientDisplay } from '@/features/clients';
import { useTransactions, type Transaction } from '@/features/transactions/api/queries';

type AppointmentType = 'inspection' | 'notary' | 'showing' | 'consultation' | 'walkthrough' | 'meeting';

interface CreateAppointmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (appointmentData: AppointmentFormData) => void;
  // Context props
  fromTransaction?: boolean;
  prefilledClientId?: string;
  prefilledClientName?: string;
  prefilledTransactionId?: string;
  prefilledTransactionAddress?: string;
}

export interface AppointmentFormData {
  type: AppointmentType;
  date: string;
  time: string;
  message: string;
  clientId: string;
  clientName: string;
  transactionId: string;
  transactionAddress: string;
}

export function CreateAppointmentModal({
  isOpen,
  onClose,
  onSubmit,
  fromTransaction = false,
  prefilledClientId = '',
  prefilledTransactionId = '',
  prefilledTransactionAddress = '',
}: CreateAppointmentModalProps) {
  const { t, i18n } = useTranslation('appointments');
  const [appointmentType, setAppointmentType] = useState<AppointmentType | ''>('');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [message, setMessage] = useState('');
  const [selectedClientId, setSelectedClientId] = useState('');
  const [selectedTransactionId, setSelectedTransactionId] = useState('');
  const [clientSearchTerm, setClientSearchTerm] = useState('');
  const [showClientDropdown, setShowClientDropdown] = useState(false);

  const modalRef = useRef<HTMLDivElement>(null);
  const clientDropdownRef = useRef<HTMLDivElement>(null);

  // Fetch real client and transaction data
  const { data: clients = [] } = useClientsForDisplay();
  const { data: transactions = [] } = useTransactions();

  const getMinDate = () => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  };

  const getTimeSlots = () => {
    const slots: string[] = [];
    for (let hour = 9; hour <= 17; hour++) {
      for (const minute of [0, 30]) {
        if (hour === 17 && minute === 30) break;
        const timeStr = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
        slots.push(timeStr);
      }
    }
    return slots;
  };

  const timeSlots = getTimeSlots();

  const filteredClients = useMemo(() =>
    clients.filter(client =>
      client.name.toLowerCase().includes(clientSearchTerm.toLowerCase()) ||
      client.email.toLowerCase().includes(clientSearchTerm.toLowerCase())
    ), [clients, clientSearchTerm]);

  const getClientDetails = (clientId: string): ClientDisplay | undefined => {
    return clients.find(c => c.id === clientId);
  };

  const getTransactionDetails = (transactionId: string): Transaction | undefined => {
    return transactions.find(t => t.transactionId === transactionId);
  };

  const getClientTransactions = (clientId: string): Transaction[] => {
    return transactions.filter(t => t.clientId === clientId);
  };

  useEffect(() => {
    if (isOpen) {
      setAppointmentType('');
      setDate('');
      setTime('');
      setMessage('');
      setClientSearchTerm('');
      setShowClientDropdown(false);

      if (fromTransaction) {
        setSelectedClientId(prefilledClientId);
        setSelectedTransactionId(prefilledTransactionId);
      } else {
        setSelectedClientId('');
        setSelectedTransactionId('');
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (showClientDropdown) {
          setShowClientDropdown(false);
        } else {
          onClose();
        }
      }
    };

    const handleTabKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;

      const focusableElements = modalRef.current?.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );

      if (!focusableElements || focusableElements.length === 0) return;

      const firstElement = focusableElements[0] as HTMLElement;
      const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          e.preventDefault();
          lastElement.focus();
        }
      } else {
        if (document.activeElement === lastElement) {
          e.preventDefault();
          firstElement.focus();
        }
      }
    };

    document.addEventListener('keydown', handleEscape);
    document.addEventListener('keydown', handleTabKey);

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.removeEventListener('keydown', handleTabKey);
    };
  }, [isOpen, onClose, showClientDropdown]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        clientDropdownRef.current &&
        !clientDropdownRef.current.contains(event.target as Node)
      ) {
        setShowClientDropdown(false);
      }
    };

    if (showClientDropdown) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => {
        document.removeEventListener('mousedown', handleClickOutside);
      };
    }
  }, [showClientDropdown]);

  const handleClientSelect = (client: ClientDisplay) => {
    setSelectedClientId(client.id);
    setClientSearchTerm(client.name);
    setShowClientDropdown(false);
    // Transaction will be selected separately
    setSelectedTransactionId('');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!appointmentType || !date || !time || !selectedClientId || !selectedTransactionId) {
      alert(t('allFieldsRequired'));
      return;
    }

    const clientDetails = getClientDetails(selectedClientId);
    const transactionDetails = getTransactionDetails(selectedTransactionId);

    if (!clientDetails || !transactionDetails) {
      return;
    }

    const appointmentData: AppointmentFormData = {
      type: appointmentType as AppointmentType,
      date,
      time: formatTimeDisplay(time),
      message,
      clientId: selectedClientId,
      clientName: clientDetails.name,
      transactionId: selectedTransactionId,
      transactionAddress: transactionDetails.propertyAddress?.street || '',
    };

    onSubmit(appointmentData);
    onClose();
  };

  const formatTimeDisplay = (time: string) => {
    if (!time) return '';
    const displayTime = new Date(`2000-01-01T${time}`).toLocaleTimeString(
      i18n.language === 'en' ? 'en-US' : 'fr-FR',
      { hour: 'numeric', minute: '2-digit', hour12: i18n.language === 'en' }
    );
    return displayTime;
  };

  const selectedClient = getClientDetails(selectedClientId);
  const clientTransactions = selectedClientId ? getClientTransactions(selectedClientId) : [];

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 bg-gray-900 bg-opacity-20 flex items-center justify-center z-50 p-4"
      onClick={onClose}
      aria-hidden={!isOpen}
    >
      <div
        ref={modalRef}
        className="rounded-xl shadow-xl w-full max-w-md mx-auto max-h-[90vh] overflow-y-auto"
        style={{ backgroundColor: '#FFFFFF' }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-appointment-modal-title"
      >
        <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-white z-10">
          <div className="flex items-center gap-3">
            <div
              className="p-2 rounded-lg"
              style={{ backgroundColor: '#FFF5F0' }}
            >
              <Calendar className="w-6 h-6" style={{ color: '#FF6B01' }} />
            </div>
            <h2 id="create-appointment-modal-title" style={{ color: '#353535' }}>
              {t('title')}
            </h2>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={onClose}
            aria-label="Close modal"
          >
            <X className="w-5 h-5" />
          </Button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Client Selector - Only visible from calendar */}
          {!fromTransaction && (
            <div ref={clientDropdownRef}>
              <label
                htmlFor="client-select"
                style={{ color: '#353535' }}
                className="block mb-2 flex items-center justify-between"
              >
                <span className="flex items-center gap-2">
                  <User className="w-4 h-4" />
                  {t('client')}
                </span>
                <span
                  style={{ color: '#ef4444', fontSize: '0.875rem' }}
                  aria-label="required"
                >
                  {t('required')}
                </span>
              </label>
              <div className="relative">
                <Input
                  type="text"
                  id="client-select"
                  value={clientSearchTerm}
                  onChange={(e) => {
                    setClientSearchTerm(e.target.value);
                    setShowClientDropdown(true);
                  }}
                  onFocus={() => setShowClientDropdown(true)}
                  placeholder={t('searchClient')}
                  className="pr-10"
                  aria-required="true"
                  autoComplete="off"
                />
                <ChevronDown
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5 pointer-events-none"
                  style={{ color: '#353535', opacity: 0.5 }}
                />

                {showClientDropdown && (
                  <div
                    className="absolute top-full left-0 right-0 mt-2 rounded-lg border-2 border-gray-200 shadow-lg max-h-60 overflow-y-auto z-20"
                    style={{ backgroundColor: '#FFFFFF' }}
                  >
                    {filteredClients.length > 0 ? (
                      filteredClients.map((client) => (
                        <Button
                          key={client.id}
                          type="button"
                          variant="ghost"
                          onClick={() => handleClientSelect(client)}
                          className="w-full h-auto text-left justify-start p-3 hover:bg-orange-50 focus:bg-orange-50 border-b border-gray-100 last:border-b-0 rounded-none"
                        >
                          <div className="flex flex-col items-start">
                            <p className="text-foreground mb-1">
                              {client.name}
                            </p>
                            <p className="text-muted-foreground text-sm">
                              {client.email}
                            </p>
                          </div>
                        </Button>
                      ))
                    ) : (
                      <div className="p-4 text-center">
                        <p style={{ color: '#353535', opacity: 0.6, fontSize: '0.875rem' }}>
                          {t('noClientsAvailable')}
                        </p>
                      </div>
                    )}
                  </div>
                )}
              </div>
              {selectedClient && (
                <p style={{ color: '#10b981', fontSize: '0.75rem' }} className="mt-2">
                  ✓ {selectedClient.name} ({selectedClient.email})
                </p>
              )}
            </div>
          )}

          {/* Transaction Reference - Locked from transaction screen, selectable from calendar */}
          {fromTransaction ? (
            <div>
              <label
                style={{ color: '#353535' }}
                className="block mb-2 flex items-center gap-2"
              >
                <MapPin className="w-4 h-4" />
                {t('transaction')}
              </label>
              <div
                className="p-3 rounded-lg"
                style={{ backgroundColor: '#f9fafb', border: '2px solid #e5e7eb' }}
              >
                <p style={{ color: '#353535' }} className="mb-1">
                  {prefilledTransactionAddress}
                </p>
                <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}>
                  {prefilledTransactionId}
                </p>
              </div>
            </div>
          ) : (
            selectedClientId && (
              <div>
                <label
                  htmlFor="transaction-select"
                  style={{ color: '#353535' }}
                  className="block mb-2 flex items-center justify-between"
                >
                  <span className="flex items-center gap-2">
                    <MapPin className="w-4 h-4" />
                    {t('transaction')}
                  </span>
                  <span
                    style={{ color: '#ef4444', fontSize: '0.875rem' }}
                    aria-label="required"
                  >
                    {t('required')}
                  </span>
                </label>
                <Select
                  value={selectedTransactionId}
                  onValueChange={setSelectedTransactionId}
                >
                  <SelectTrigger id="transaction-select" className="w-full" aria-required="true">
                    <SelectValue placeholder={t('selectTransaction')} />
                  </SelectTrigger>
                  <SelectContent>
                    {clientTransactions.map((transaction) => (
                      <SelectItem key={transaction.transactionId} value={transaction.transactionId}>
                        {transaction.propertyAddress?.street || 'Unknown'} ({transaction.transactionId})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {clientTransactions.length === 0 && (
                  <p style={{ color: '#ef4444', fontSize: '0.75rem' }} className="mt-2">
                    {t('noTransactionsAvailable')}
                  </p>
                )}
              </div>
            )
          )}

          <div>
            <label
              htmlFor="appointment-type"
              style={{ color: '#353535' }}
              className="block mb-2 flex items-center justify-between"
            >
              <span className="flex items-center gap-2">
                <FileText className="w-4 h-4" />
                {t('appointmentType')}
              </span>
              <span
                style={{ color: '#ef4444', fontSize: '0.875rem' }}
                aria-label="required"
              >
                {t('required')}
              </span>
            </label>
            <Select
              value={appointmentType}
              onValueChange={(value) => setAppointmentType(value as AppointmentType)}
            >
              <SelectTrigger id="appointment-type" className="w-full" aria-required="true">
                <SelectValue placeholder={t('selectType')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="inspection">{t('inspection')}</SelectItem>
                <SelectItem value="notary">{t('notary')}</SelectItem>
                <SelectItem value="showing">{t('showing')}</SelectItem>
                <SelectItem value="consultation">{t('consultation')}</SelectItem>
                <SelectItem value="walkthrough">{t('walkthrough')}</SelectItem>
                <SelectItem value="meeting">{t('meeting')}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div>
            <label
              htmlFor="appointment-date"
              style={{ color: '#353535' }}
              className="block mb-2 flex items-center justify-between"
            >
              <span className="flex items-center gap-2">
                <Calendar className="w-4 h-4" />
                {t('date')}
              </span>
              <span
                style={{ color: '#ef4444', fontSize: '0.875rem' }}
                aria-label="required"
              >
                {t('required')}
              </span>
            </label>
            <Input
              type="date"
              id="appointment-date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              min={getMinDate()}
              aria-required="true"
            />
          </div>

          <div>
            <label
              htmlFor="appointment-time"
              style={{ color: '#353535' }}
              className="block mb-2 flex items-center justify-between"
            >
              <span className="flex items-center gap-2">
                <Clock className="w-4 h-4" />
                {t('time')}
              </span>
              <span
                style={{ color: '#ef4444', fontSize: '0.875rem' }}
                aria-label="required"
              >
                {t('required')}
              </span>
            </label>
            <Select
              value={time}
              onValueChange={setTime}
            >
              <SelectTrigger id="appointment-time" className="w-full" aria-required="true">
                <SelectValue placeholder={t('selectTime')} />
              </SelectTrigger>
              <SelectContent>
                {timeSlots.map((timeSlot) => (
                  <SelectItem key={timeSlot} value={timeSlot}>
                    {formatTimeDisplay(timeSlot)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div>
            <label
              htmlFor="appointment-message"
              style={{ color: '#353535' }}
              className="block mb-2 flex items-center justify-between"
            >
              <span className="flex items-center gap-2">
                <MessageSquare className="w-4 h-4" />
                {t('messageToClient')}
              </span>
              <span
                style={{ color: '#6b7280', fontSize: '0.875rem' }}
                aria-label="optional"
              >
                {t('optional')}
              </span>
            </label>
            <Textarea
              id="appointment-message"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              rows={4}
              placeholder={t('messagePlaceholder')}
              aria-required="false"
            />
          </div>
        </form>

        <div className="p-6 border-t border-gray-200 bg-gray-50">
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              className="flex-1"
            >
              {t('cancel')}
            </Button>
            <Button
              type="button"
              onClick={handleSubmit}
              disabled={!appointmentType || !date || !time || !selectedClientId || !selectedTransactionId}
              className="flex-1 gap-2"
            >
              <Send className="w-5 h-5" />
              {t('sendRequest')}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}