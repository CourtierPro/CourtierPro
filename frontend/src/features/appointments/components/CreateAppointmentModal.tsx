import React, { useState, useEffect, useRef } from 'react';
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

type AppointmentType = 'inspection' | 'notary' | 'showing' | 'consultation' | 'walkthrough' | 'meeting';

interface Client {
  id: string;
  name: string;
  email: string;
  transactionId?: string;
  transactionAddress?: string;
}

interface Transaction {
  id: string;
  address: string;
  clientId: string;
  clientName: string;
}

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

// Mock client data
const mockClients: Client[] = [
  {
    id: 'C-001',
    name: 'John Smith',
    email: 'john.smith@email.com',
    transactionId: 'TX-1001',
    transactionAddress: '123 Maple Street, Montreal, QC',
  },
  {
    id: 'C-002',
    name: 'Emma Johnson',
    email: 'emma.j@email.com',
    transactionId: 'TX-1002',
    transactionAddress: '456 Oak Avenue, Montreal, QC',
  },
  {
    id: 'C-003',
    name: 'Michael Brown',
    email: 'mbrown@email.com',
    transactionId: 'TX-1003',
    transactionAddress: '789 Pine Road, Montreal, QC',
  },
  {
    id: 'C-004',
    name: 'Sarah Davis',
    email: 'sarah.davis@email.com',
  },
  {
    id: 'C-005',
    name: 'David Wilson',
    email: 'dwilson@email.com',
  },
];

// Mock transaction data
const mockTransactions: Transaction[] = [
  {
    id: 'TX-1001',
    address: '123 Maple Street, Montreal, QC',
    clientId: 'C-001',
    clientName: 'John Smith',
  },
  {
    id: 'TX-1002',
    address: '456 Oak Avenue, Montreal, QC',
    clientId: 'C-002',
    clientName: 'Emma Johnson',
  },
  {
    id: 'TX-1003',
    address: '789 Pine Road, Montreal, QC',
    clientId: 'C-003',
    clientName: 'Michael Brown',
  },
];

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

  // Get minimum date (today)
  const getMinDate = () => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  };

  // Get time slots (every 30 minutes from 9 AM to 5 PM)
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

  // Filter clients based on search term
  const filteredClients = mockClients.filter(client =>
    client.name.toLowerCase().includes(clientSearchTerm.toLowerCase()) ||
    client.email.toLowerCase().includes(clientSearchTerm.toLowerCase())
  );

  // Get client details
  const getClientDetails = (clientId: string) => {
    return mockClients.find(c => c.id === clientId);
  };

  // Get transaction details
  const getTransactionDetails = (transactionId: string) => {
    return mockTransactions.find(t => t.id === transactionId);
  };

  // Get transactions for selected client
  const getClientTransactions = (clientId: string) => {
    return mockTransactions.filter(t => t.clientId === clientId);
  };

  // Reset form when modal opens
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

  // Handle ESC key and focus trap
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

  // Close dropdown when clicking outside
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

  const handleClientSelect = (client: Client) => {
    setSelectedClientId(client.id);
    setClientSearchTerm(client.name);
    setShowClientDropdown(false);

    // Auto-select transaction if client has one
    if (client.transactionId) {
      setSelectedTransactionId(client.transactionId);
    } else {
      setSelectedTransactionId('');
    }
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
      transactionAddress: transactionDetails.address,
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
        {/* Modal Header */}
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
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] transition-colors"
            aria-label="Close modal"
          >
            <X className="w-5 h-5" style={{ color: '#353535' }} />
          </button>
        </div>

        {/* Modal Body */}
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
                <input
                  type="text"
                  id="client-select"
                  value={clientSearchTerm}
                  onChange={(e) => {
                    setClientSearchTerm(e.target.value);
                    setShowClientDropdown(true);
                  }}
                  onFocus={() => setShowClientDropdown(true)}
                  placeholder={t('searchClient')}
                  className="w-full p-3 pr-10 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent transition-all"
                  style={{ color: '#353535' }}
                  aria-required="true"
                  autoComplete="off"
                />
                <ChevronDown
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5 pointer-events-none"
                  style={{ color: '#353535', opacity: 0.5 }}
                />

                {/* Client Dropdown */}
                {showClientDropdown && (
                  <div
                    className="absolute top-full left-0 right-0 mt-2 rounded-lg border-2 border-gray-200 shadow-lg max-h-60 overflow-y-auto z-20"
                    style={{ backgroundColor: '#FFFFFF' }}
                  >
                    {filteredClients.length > 0 ? (
                      filteredClients.map((client) => (
                        <button
                          key={client.id}
                          type="button"
                          onClick={() => handleClientSelect(client)}
                          className="w-full text-left p-3 hover:bg-orange-50 focus:outline-none focus:bg-orange-50 transition-colors border-b border-gray-100 last:border-b-0"
                        >
                          <p style={{ color: '#353535' }} className="mb-1">
                            {client.name}
                          </p>
                          <p style={{ color: '#353535', opacity: 0.6, fontSize: '0.875rem' }}>
                            {client.email}
                          </p>
                          {client.transactionAddress && (
                            <p style={{ color: '#FF6B01', fontSize: '0.75rem' }} className="mt-1">
                              {client.transactionAddress}
                            </p>
                          )}
                        </button>
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
                <select
                  id="transaction-select"
                  value={selectedTransactionId}
                  onChange={(e) => setSelectedTransactionId(e.target.value)}
                  className="w-full p-3 pr-10 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent appearance-none transition-all"
                  style={{ color: '#353535' }}
                  aria-required="true"
                >
                  <option value="">{t('selectTransaction')}</option>
                  {clientTransactions.map((transaction) => (
                    <option key={transaction.id} value={transaction.id}>
                      {transaction.address} ({transaction.id})
                    </option>
                  ))}
                </select>
                {clientTransactions.length === 0 && (
                  <p style={{ color: '#ef4444', fontSize: '0.75rem' }} className="mt-2">
                    {t('noTransactionsAvailable')}
                  </p>
                )}
              </div>
            )
          )}

          {/* Appointment Type */}
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
            <div className="relative">
              <select
                id="appointment-type"
                value={appointmentType}
                onChange={(e) => setAppointmentType(e.target.value as AppointmentType)}
                className="w-full p-3 pr-10 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent appearance-none transition-all"
                style={{ color: '#353535' }}
                aria-required="true"
              >
                <option value="">{t('selectType')}</option>
                <option value="inspection">{t('inspection')}</option>
                <option value="notary">{t('notary')}</option>
                <option value="showing">{t('showing')}</option>
                <option value="consultation">{t('consultation')}</option>
                <option value="walkthrough">{t('walkthrough')}</option>
                <option value="meeting">{t('meeting')}</option>
              </select>
              <ChevronDown
                className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5 pointer-events-none"
                style={{ color: '#353535', opacity: 0.5 }}
              />
            </div>
          </div>

          {/* Date */}
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
            <div className="relative">
              <input
                type="date"
                id="appointment-date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                min={getMinDate()}
                className="w-full p-3 pr-10 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent transition-all"
                style={{ color: '#353535' }}
                aria-required="true"
              />
              <Calendar
                className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5 pointer-events-none"
                style={{ color: '#353535', opacity: 0.5 }}
              />
            </div>
          </div>

          {/* Time */}
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
            <div className="relative">
              <select
                id="appointment-time"
                value={time}
                onChange={(e) => setTime(e.target.value)}
                className="w-full p-3 pr-10 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent appearance-none transition-all"
                style={{ color: '#353535' }}
                aria-required="true"
              >
                <option value="">{t('selectTime')}</option>
                {timeSlots.map((timeSlot) => (
                  <option key={timeSlot} value={timeSlot}>
                    {formatTimeDisplay(timeSlot)}
                  </option>
                ))}
              </select>
              <Clock
                className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5 pointer-events-none"
                style={{ color: '#353535', opacity: 0.5 }}
              />
            </div>
          </div>

          {/* Message to Client */}
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
            <div className="relative">
              <MessageSquare
                className="absolute left-3 top-3 w-5 h-5"
                style={{ color: '#353535', opacity: 0.5 }}
              />
              <textarea
                id="appointment-message"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                rows={4}
                className="w-full p-3 pl-11 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent resize-none transition-all"
                style={{ color: '#353535' }}
                placeholder={t('messagePlaceholder')}
                aria-required="false"
              />
            </div>
          </div>
        </form>

        {/* Modal Footer */}
        <div className="p-6 border-t border-gray-200 bg-gray-50">
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-3 rounded-lg hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-300 focus:ring-offset-2 transition-colors border-2 border-gray-200"
              style={{ color: '#353535', backgroundColor: '#FFFFFF' }}
            >
              {t('cancel')}
            </button>
            <button
              type="button"
              onClick={handleSubmit}
              disabled={!appointmentType || !date || !time || !selectedClientId || !selectedTransactionId}
              className="flex-1 px-4 py-3 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              style={{
                backgroundColor: appointmentType && date && time && selectedClientId && selectedTransactionId ? '#FF6B01' : '#e5e7eb',
                color: appointmentType && date && time && selectedClientId && selectedTransactionId ? '#FFFFFF' : '#9ca3af',
              }}
              aria-disabled={!appointmentType || !date || !time || !selectedClientId || !selectedTransactionId}
            >
              <Send className="w-5 h-5" />
              {t('sendRequest')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}