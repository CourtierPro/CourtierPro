import React, { useState, useEffect, useRef, useMemo } from 'react';
import {
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
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";
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
    <Dialog open={isOpen} onOpenChange={(val) => !val && onClose()}>
      <DialogContent className="sm:max-w-md max-h-[90vh] overflow-y-auto p-0 gap-0">
        <DialogHeader className="p-6 border-b border-border sticky top-0 bg-card z-10 flex-row items-center justify-between space-y-0">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/10">
              <Calendar className="w-6 h-6 text-primary" />
            </div>
            <DialogTitle>{t('title')}</DialogTitle>
          </div>
          {/* Default X close button is provided by DialogContent, but we need to ensure it's visible. 
              Actually, DialogContent adds an absolute X button. 
              The original design had a flex header. 
              Let's hide the default X by passing a prop or just let it overlay? 
              The default X is absolute right-4 top-4. 
              Our header is p-6. Top-4 is inside the header. 
              So standard Close button should work fine. 
              I will NOT add a custom button. */}
        </DialogHeader>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Client Selector - Only visible from calendar */}
          {!fromTransaction && (
            <div ref={clientDropdownRef}>
              <label
                htmlFor="client-select"
                className="block mb-2 flex items-center justify-between text-foreground"
              >
                <span className="flex items-center gap-2">
                  <User className="w-4 h-4" />
                  {t('client')}
                </span>
                <span
                  className="text-destructive text-sm"
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
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5 pointer-events-none opacity-50 text-foreground"
                />

                {showClientDropdown && (
                  <div
                    className="absolute top-full left-0 right-0 mt-2 rounded-lg border-2 border-border shadow-lg max-h-60 overflow-y-auto z-20 bg-background"
                  >
                    {filteredClients.length > 0 ? (
                      filteredClients.map((client) => (
                        <Button
                          key={client.id}
                          type="button"
                          variant="ghost"
                          onClick={() => handleClientSelect(client)}
                          className="w-full h-auto text-left justify-start p-3 hover:bg-muted focus:bg-muted border-b border-border last:border-b-0 rounded-none"
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
                        <p className="text-foreground/60 text-sm">
                          {t('noClientsAvailable')}
                        </p>
                      </div>
                    )}
                  </div>
                )}
              </div>
              {selectedClient && (
                <p className="mt-2 text-xs text-emerald-500">
                  ✓ {selectedClient.name} ({selectedClient.email})
                </p>
              )}
            </div>
          )}

          {/* Transaction Reference - Locked from transaction screen, selectable from calendar */}
          {fromTransaction ? (
            <div>
              <label
                className="block mb-2 flex items-center gap-2 text-foreground"
              >
                <MapPin className="w-4 h-4" />
                {t('transaction')}
              </label>
              <div
                className="p-3 rounded-lg bg-muted border-2 border-border"
              >
                <p className="mb-1 text-foreground">
                  {prefilledTransactionAddress}
                </p>
                <p className="text-foreground/70 text-sm">
                  {prefilledTransactionId}
                </p>
              </div>
            </div>
          ) : (
            selectedClientId && (
              <div>
                <label
                  htmlFor="transaction-select"
                  className="block mb-2 flex items-center justify-between text-foreground"
                >
                  <span className="flex items-center gap-2">
                    <MapPin className="w-4 h-4" />
                    {t('transaction')}
                  </span>
                  <span
                    className="text-destructive text-sm"
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
                  <p className="mt-2 text-xs text-destructive">
                    {t('noTransactionsAvailable')}
                  </p>
                )}
              </div>
            )
          )}

          <div>
            <label
              htmlFor="appointment-type"
              className="block mb-2 flex items-center justify-between text-foreground"
            >
              <span className="flex items-center gap-2">
                <FileText className="w-4 h-4" />
                {t('appointmentType')}
              </span>
              <span
                className="text-destructive text-sm"
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
              className="block mb-2 flex items-center justify-between text-foreground"
            >
              <span className="flex items-center gap-2">
                <Calendar className="w-4 h-4" />
                {t('date')}
              </span>
              <span
                className="text-destructive text-sm"
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
              className="block mb-2 flex items-center justify-between text-foreground"
            >
              <span className="flex items-center gap-2">
                <Clock className="w-4 h-4" />
                {t('time')}
              </span>
              <span
                className="text-destructive text-sm"
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
              className="block mb-2 flex items-center justify-between text-foreground"
            >
              <span className="flex items-center gap-2">
                <MessageSquare className="w-4 h-4" />
                {t('messageToClient')}
              </span>
              <span
                className="text-muted-foreground text-sm"
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

        <div className="p-6 border-t border-border bg-muted/50">
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
      </DialogContent>
    </Dialog>
  );
}