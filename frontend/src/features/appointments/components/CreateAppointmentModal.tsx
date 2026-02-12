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
import { useTransactions, useTransactionProperties, type Transaction } from '@/features/transactions/api/queries';
import { useAuth0 } from "@auth0/auth0-react";
import { toast } from "sonner";
import { useRequestAppointment } from "../api/mutations";

import { AlertTriangle } from "lucide-react";
import { type Appointment } from "../types";

type AppointmentType = 'inspection' | 'notary' | 'showing' | 'consultation' | 'walkthrough' | 'meeting' | 'house_visit' | 'other';

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
  existingAppointments?: Appointment[];
  initialData?: Partial<AppointmentFormData> & { title?: string, startTime?: string, endTime?: string };
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
  existingAppointments = [],
  initialData,
}: CreateAppointmentModalProps) {
  const { t, i18n } = useTranslation('appointments');
  const { user } = useAuth0();
  const { mutate: requestAppointment, isPending } = useRequestAppointment();

  // Determine user role (naive check, prefer robust role hook if available)
  // Assuming roles are in a specific claim or just checking if user is broker based on metadata isn't easy without a helper.
  // Ideally, we have a useUserRole hook. For now, we can check the namespace if available or rely on backend.
  // Actually, let's use a simpler heuristic or just default text if we can't easily determine.
  // But wait, the previous code didn't have user info.
  // Let's import useUserContext if it exists, otherwise just generic text or check user.

  // Checking typical Auth0 namespace for roles
  // Determine user role
  const userRoles = (user?.['https://courtierpro.dev/roles'] as string[]) || [];
  const isBroker = userRoles.includes('BROKER');

  // Mock user id retrieval - in real app, we should have this from auth context
  // For now, we rely on the fact that for clients, the backend endpoints resolve "me" or we need to pass it.
  // Actually, we need the current user's client ID if we are a client.
  // Since we don't have a global "user profile" context easily accessible here yet, 
  // we might need to rely on the backend filtering for /transactions/my-transactions or similar?
  // Previous code used useClientTransactions(clientId).
  // Let's assume for now we can't easily get the ID without a query. 
  // However, `useTransactions` might be failing because it calls `/transactions`.
  // `useTransactions` takes filters. maybe we can pass a filter?

  // Correction: client viewing this modal needs to see THEIR transactions.
  // There is `useClientTransactions(clientId)` but we need the clientId.
  // If we can't get it easily, maybe we can use a new endpoint or the existing one with a query param?
  // Wait, `useTransactions` calls `/transactions`. If that endpoint is broker-only, `useTransactions` is broker-only.
  // We need `useClientTransactions` hook which calls `/clients/{id}/transactions`.
  // BUT we need the ID. 
  // Alternative: The backend might have a `/transactions` endpoint that works for clients too (returning their own)? 
  // The user says "403". So likely `/transactions` is broker only.

  // Let's assume we can get the client ID from the user object if it was mapped, but Auth0 user.sub 
  // might not match our DB UUID.
  // Let's try to disable the queries for now if !isBroker to verify at least the modal renders.

  // Actually, if I am a client, I need to pick a transaction.
  // I need to fetch MY transactions.
  // Does `useTransactions` support filtering by "me"?
  // Let's simply disable `useClientsForDisplay` for non-brokers.

  const [appointmentType, setAppointmentType] = useState<AppointmentType | ''>('');
  const [customTitle, setCustomTitle] = useState('');
  const [date, setDate] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [message, setMessage] = useState('');
  const [selectedClientId, setSelectedClientId] = useState('');
  const [selectedTransactionId, setSelectedTransactionId] = useState('');
  const [clientSearchTerm, setClientSearchTerm] = useState('');
  const [showClientDropdown, setShowClientDropdown] = useState(false);
  const [selectedPropertyId, setSelectedPropertyId] = useState('');

  const modalRef = useRef<HTMLDivElement>(null);
  const clientDropdownRef = useRef<HTMLDivElement>(null);

  // Fetch real client and transaction data
  const { data: clients = [] } = useClientsForDisplay({ enabled: isBroker });
  const { data: allTransactions = [] } = useTransactions({}, { enabled: true });

  // Fetch properties for the selected transaction (needed for house_visit property picker)
  const selectedTxDetails = allTransactions.find(tx => tx.transactionId === selectedTransactionId);
  const isBuySide = selectedTxDetails?.side === 'BUY_SIDE';
  const { data: transactionProperties = [] } = useTransactionProperties(selectedTransactionId);
  // We will need a way to fetch client transactions later, for now empty array if not broker to avoid 403
  // Actually, without modifying the hooks to accept 'enabled', we can't stop the query from running this way.
  // We MUST render useClientsForDisplay conditionally or update the hook.
  // Since we can't conditionally call hooks, we MUST update the hook signature.

  // For brokers, allTransactions contains all their transactions.
  // For clients, allTransactions contains only their own transactions (backend filtered).
  const transactions = allTransactions;

  const getMinDate = () => {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
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

  const filteredClients = useMemo(() => {
    // Get Set of client IDs from transactions
    const transactionClientIds = new Set(transactions.map(t => t.clientId));

    return clients.filter(client => {
      // Must allow search, but restrict to those in transactions
      if (!transactionClientIds.has(client.id)) return false;

      const name = client.name ? client.name.toLowerCase() : '';
      const email = client.email ? client.email.toLowerCase() : '';
      const search = clientSearchTerm ? clientSearchTerm.toLowerCase() : '';
      return name.includes(search) || email.includes(search);
    });
  }, [clients, clientSearchTerm, transactions]);

  const getClientDetails = (clientId: string): ClientDisplay | undefined => {
    return clients.find(c => c.id === clientId);
  };

  const getTransactionDetails = (transactionId: string): Transaction | undefined => {
    // If client, we might not have it in `transactions` list if we disabled fetching.
    // However, if `fromTransaction` is true, we assume the parent passed valid IDs.
    // If purely from calendar, client needs to select.
    return transactions.find(t => t.transactionId === transactionId);
  };

  const availableTransactions = useMemo(() => {
    if (isBroker && selectedClientId) {
      return transactions.filter(t => t.clientId === selectedClientId);
    }
    // If client, return their transactions (which might be empty for now if we don't fetch)
    return transactions;
  }, [selectedClientId, transactions, isBroker]);

  const handleTransactionSelect = (txId: string) => {
    setSelectedTransactionId(txId);
    setSelectedPropertyId('');

    const tx = transactions.find(t => t.transactionId === txId);

    if (tx && tx.side !== 'BUY_SIDE' && appointmentType === 'house_visit') {
      setAppointmentType('');
    }

    if (tx && tx.clientId && tx.clientId !== selectedClientId) {
      const client = getClientDetails(tx.clientId);
      if (client) {
        handleClientSelect(client);
      }
    }
    // Update broker info if present
    if (tx && tx.brokerName) {
      setSelectedBrokerName(tx.brokerName);
      setSelectedBrokerId(tx.brokerId || '');
      // Use the single search term state for the input
      if (!isBroker) {
        setClientSearchTerm(tx.brokerName);
      }
    }
  };

  useEffect(() => {
    if (isOpen) {
      if (initialData) {
        setAppointmentType(initialData.type || '');
        setCustomTitle(initialData.title || '');
        setMessage(initialData.message || '');
        // We don't prepopulate date/time for rescheduling usually unless specifically requested to "copy" exactly, 
        // but typically rescheduling implies a *new* time. 
        // However, the requirement says "fields should be filled".
        // Let's fill them if provided, but user will likely change them.
        setDate(initialData.date || '');
        setStartTime(initialData.startTime || initialData.time || '');
        setEndTime(initialData.endTime || initialData.time || '');
      } else {
        setAppointmentType('');
        setCustomTitle('');
        setDate('');
        setStartTime('');
        setEndTime('');
        setMessage('');
      }

      setClientSearchTerm('');
      setShowClientDropdown(false);

      if (fromTransaction) {
        setSelectedClientId(prefilledClientId);
        setSelectedTransactionId(prefilledTransactionId);
        // Try to pre-fill broker info if available in the transaction list
        if (allTransactions.length > 0) {
          const tx = allTransactions.find(t => t.transactionId === prefilledTransactionId);
          if (tx && tx.brokerName) {
            setSelectedBrokerName(tx.brokerName);
            setSelectedBrokerId(tx.brokerId || '');
          }
        }
      } else if (initialData && initialData.transactionId && initialData.clientId) {
        // Robust handling for rescheduling case
        setSelectedTransactionId(initialData.transactionId);
        setSelectedClientId(initialData.clientId);
        // Also try to resolve broker name if possible
        if (allTransactions.length > 0) {
          const tx = allTransactions.find(t => t.transactionId === initialData.transactionId);
          if (tx && tx.brokerName) {
            setSelectedBrokerName(tx.brokerName);
            setSelectedBrokerId(tx.brokerId || '');
          }
        }
      } else {
        setSelectedClientId('');
        setSelectedTransactionId('');
        setSelectedBrokerId('');
        setSelectedBrokerName('');
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, allTransactions]);

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

  // Logic for Clients (Broker selection)
  const [selectedBrokerId, setSelectedBrokerId] = useState('');
  const [selectedBrokerName, setSelectedBrokerName] = useState('');

  const uniqueBrokers = useMemo(() => {
    if (isBroker) return [];
    const brokerMap = new Map<string, { id: string, name: string }>();
    transactions.forEach(t => {
      // Transaction object has brokerName, but maybe not brokerId explicitly in the top level? 
      // Checking Transaction type in queries.ts: brokerId?: string;
      if (t.brokerId && t.brokerName) {
        brokerMap.set(t.brokerId, { id: t.brokerId, name: t.brokerName });
      }
    });
    return Array.from(brokerMap.values());
  }, [transactions, isBroker]);

  const filteredBrokers = useMemo(() => {
    if (isBroker) return [];
    return uniqueBrokers.filter(b =>
      b.name.toLowerCase().includes(clientSearchTerm.toLowerCase())
    );
  }, [uniqueBrokers, clientSearchTerm, isBroker]);

  const handleBrokerSelect = (broker: { id: string, name: string }) => {
    setSelectedBrokerId(broker.id);
    setSelectedBrokerName(broker.name);
    setClientSearchTerm(broker.name);
    setShowClientDropdown(false);

    // Check if current transaction belongs to this broker
    const currentTx = transactions.find(t => t.transactionId === selectedTransactionId);
    if (currentTx && currentTx.brokerId !== broker.id) {
      setSelectedTransactionId('');
    }
  };

  const handleClientSelect = (client: ClientDisplay) => {
    setSelectedClientId(client.id);
    setClientSearchTerm(client.name);
    setShowClientDropdown(false);

    // Check if current transaction belongs to new client
    const currentTx = transactions.find(t => t.transactionId === selectedTransactionId);
    if (currentTx && currentTx.clientId !== client.id) {
      setSelectedTransactionId('');
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Validation:
    // If broker: needs selectedClientId.
    // If client: needs selectedTransactionId (which implies me + broker).

    // Determine effective client ID
    // If isBroker, we selected a client.
    // If isClient, we fetch client ID from the selected transaction.
    const transactionDetails = getTransactionDetails(selectedTransactionId);
    const effectiveClientId = isBroker ? selectedClientId : transactionDetails?.clientId;



    if (!appointmentType || !date || !startTime || !endTime || !selectedTransactionId || !effectiveClientId) {
      toast.error(t('allFieldsRequired'));
      return;
    }

    if (appointmentType === 'other' && !customTitle.trim()) {
      toast.error(t('titleRequired', 'Title is required for custom appointment type'));
      return;
    }

    if (appointmentType === 'house_visit' && !selectedPropertyId) {
      toast.error(t('propertyRequired', 'Please select a property for this house visit'));
      return;
    }

    // Validation for time
    const [startHour, startMin] = startTime.split(':').map(Number);
    const [endHour, endMin] = endTime.split(':').map(Number);
    const startVal = startHour * 60 + startMin;
    const endVal = endHour * 60 + endMin;

    if (endVal <= startVal) {
      toast.error(t('endTimeMustBeAfterStart', 'End time must be after start time'));
      return;
    }

    // Resolve client name
    const effectiveClientName = isBroker
      ? getClientDetails(selectedClientId)?.name
      : transactionDetails?.clientName; // fallback if needed

    if (!transactionDetails) return;

    const appointmentData: AppointmentFormData = {
      type: appointmentType as AppointmentType,
      date,
      time: formatTimeDisplay(startTime), // keeping 'time' for formData compat if needed, or update interface? The generic form data might strictly typically just show start.
      // Actually AppointmentFormData interface strictly has 'time'. Let's just pass start time string there for now or update the interface too.
      // Ideally update interface, but for now let's just use startTime.
      message,
      clientId: effectiveClientId,
      clientName: effectiveClientName || '',
      transactionId: selectedTransactionId,
      transactionAddress: transactionDetails.propertyAddress?.street || '',
    };

    // Call the mutation
    requestAppointment({
      transactionId: selectedTransactionId,
      type: appointmentType,
      title: appointmentType === 'other' ? customTitle : undefined,
      date: date,
      startTime: startTime,
      endTime: endTime,
      message: message,
      propertyId: appointmentType === 'house_visit' ? selectedPropertyId : undefined
    }, {
      onSuccess: () => {
        const successMessageKey = isBroker ? 'appointmentSentToClient' : 'appointmentSentToBroker';
        toast.success(t(successMessageKey, 'Appointment sent'));
        onSubmit(appointmentData);
        onClose();
      },
      onError: (error) => {
        console.error(error);
        toast.error(t('appointmentRequestFailed'));
      }
    });
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
  // availableTransactions calculation moved up

  // Check for conflicts
  // Check for conflicts (naive)
  // Check for conflicts
  const hasTimeConflict = useMemo(() => {
    if (!date || !startTime || !endTime) return false;

    // Create Date objects for selected range
    const selectedStart = new Date(`${date}T${startTime}`);
    const selectedEnd = new Date(`${date}T${endTime}`);

    return existingAppointments.some(apt => {
      // Skip cancelled or declined appointments if necessary, but visually maybe we still care?
      // Assuming we check against all active appointments.
      if (apt.status === 'CANCELLED' || apt.status === 'DECLINED') return false;

      const aptStart = new Date(apt.fromDateTime);
      const aptEnd = new Date(apt.toDateTime);

      // Check for overlap: (StartA < EndB) && (EndA > StartB)
      return selectedStart < aptEnd && selectedEnd > aptStart;
    });
  }, [date, startTime, endTime, existingAppointments]);

  if (!isOpen) return null;

  return (
    <Dialog open={isOpen} onOpenChange={(val) => !val && onClose()}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto p-0 gap-0">
        <DialogHeader className="p-6 border-b border-border sticky top-0 bg-card z-10 flex-row items-center justify-between space-y-0">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/10">
              <Calendar className="w-6 h-6 text-primary" />
            </div>
            <DialogTitle>{t('title')}</DialogTitle>
          </div>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">


          {/* Party Selector (Client for Broker) - Only visible from calendar and if User is Broker */}
          {!fromTransaction && isBroker && (
            <div ref={clientDropdownRef}>
              <label
                htmlFor="party-select"
                className="block mb-2 flex items-center justify-between text-foreground"
              >
                <span className="flex items-center gap-2">
                  <User className="w-4 h-4" />
                  {isBroker ? t('client') : t('broker', 'Broker')}
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
                  id="party-select"
                  value={clientSearchTerm}
                  onChange={(e) => {
                    const value = e.target.value;
                    setClientSearchTerm(value);
                    setShowClientDropdown(true);

                    if (value === '' && selectedClientId) {
                      setSelectedClientId('');
                    }
                  }}
                  onFocus={() => setShowClientDropdown(true)}
                  placeholder={isBroker ? t('searchClient') : t('searchBroker', 'Search brokers...')}
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
                    {isBroker ? (
                      filteredClients.length > 0 ? (
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
                      )
                    ) : (
                      // Render Brokers list (derived from transactions)
                      // Note: We need to derive this list in the component body first
                      // See 'filteredBrokers' below
                      filteredBrokers.length > 0 ? (
                        filteredBrokers.map((broker) => (
                          <Button
                            key={broker.id}
                            type="button"
                            variant="ghost"
                            onClick={() => handleBrokerSelect(broker)}
                            className="w-full h-auto text-left justify-start p-3 hover:bg-muted focus:bg-muted border-b border-border last:border-b-0 rounded-none"
                          >
                            <div className="flex flex-col items-start">
                              <p className="text-foreground mb-1">
                                {broker.name}
                              </p>
                            </div>
                          </Button>
                        ))
                      ) : (
                        <div className="p-4 text-center">
                          <p className="text-foreground/60 text-sm">
                            {t('noBrokersAvailable', 'No brokers available')}
                          </p>
                        </div>
                      )
                    )}
                  </div>
                )}
              </div>
              {/* Display selected party */}
              {isBroker && selectedClient && (
                <p className="mt-2 text-xs text-emerald-500">
                  ✓ {selectedClient.name} ({selectedClient.email})
                </p>
              )}
              {!isBroker && selectedBrokerName && (
                <p className="mt-2 text-xs text-emerald-500">
                  ✓ {selectedBrokerName}
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

              {/* Show selected broker info prominently for Client users */}
              {!isBroker && selectedBrokerName && (
                <div className="mt-4 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20" data-broker-id={selectedBrokerId}>
                  <p className="text-sm font-medium text-emerald-700 dark:text-emerald-400 flex items-center gap-2">
                    <User className="w-4 h-4" />
                    {t('broker')}: {selectedBrokerName}
                  </p>
                </div>
              )}
            </div>
          ) : (
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
                onValueChange={handleTransactionSelect}
              >
                <SelectTrigger id="transaction-select" className="w-full" aria-required="true">
                  <SelectValue placeholder={t('selectTransaction')} />
                </SelectTrigger>
                <SelectContent>
                  {availableTransactions.map((transaction) => (
                    <SelectItem key={transaction.transactionId} value={transaction.transactionId}>
                      {transaction.propertyAddress?.street || t('buyerSideNoAddress', 'No Address (Buyer Side)')} ({transaction.transactionId})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {availableTransactions.length === 0 && (
                <p className="mt-2 text-xs text-destructive">
                  {selectedClientId ? t('noTransactionsAvailable') : t('noTransactionsFound')}
                </p>
              )}

              {/* Show selected broker info prominently for Client users */}
              {!isBroker && selectedBrokerName && (
                <div className="mt-4 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20" data-broker-id={selectedBrokerId}>
                  <p className="text-sm font-medium text-emerald-700 dark:text-emerald-400 flex items-center gap-2">
                    <User className="w-4 h-4" />
                    {t('broker')}: {selectedBrokerName}
                  </p>
                </div>
              )}
            </div>
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
                {isBuySide && (
                  <SelectItem value="house_visit">{t('house_visit', 'House Visit')}</SelectItem>
                )}
                <SelectItem value="other">{t('other', 'Other')}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {appointmentType === 'house_visit' && (
            <div>
              <label
                htmlFor="property-select"
                className="block mb-2 flex items-center justify-between text-foreground"
              >
                <span className="flex items-center gap-2">
                  <MapPin className="w-4 h-4" />
                  {t('selectProperty', 'Select Property')}
                </span>
                <span
                  className="text-destructive text-sm"
                  aria-label="required"
                >
                  {t('required')}
                </span>
              </label>
              <Select
                value={selectedPropertyId}
                onValueChange={setSelectedPropertyId}
              >
                <SelectTrigger id="property-select" className="w-full" aria-required="true">
                  <SelectValue placeholder={t('selectProperty', 'Select Property')} />
                </SelectTrigger>
                <SelectContent>
                  {transactionProperties.length === 0 ? (
                    <div className="px-3 py-2 text-sm text-muted-foreground">
                      {t('noPropertiesAvailable', 'No properties available')}
                    </div>
                  ) : (
                    transactionProperties.map((prop) => (
                      <SelectItem key={prop.propertyId} value={prop.propertyId}>
                        {[prop.address?.street, prop.address?.city].filter(Boolean).join(', ') || prop.propertyId}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>
          )}

          {appointmentType === 'other' && (
            <div>
              <label
                htmlFor="custom-title"
                className="block mb-2 flex items-center justify-between text-foreground"
              >
                <span className="flex items-center gap-2">
                  <FileText className="w-4 h-4" />
                  {t('customTitle', 'Title')}
                </span>
                <span
                  className="text-destructive text-sm"
                  aria-label="required"
                >
                  {t('required')}
                </span>
              </label>
              <Input
                id="custom-title"
                value={customTitle}
                onChange={(e) => setCustomTitle(e.target.value)}
                placeholder={t('enterCustomTitle', 'Enter appointment title')}
                aria-required="true"
              />
            </div>
          )}

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
            {hasTimeConflict && (
              <div className="mb-4 bg-destructive/15 text-destructive border border-destructive/30 rounded-lg p-3 flex items-start gap-3">
                <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
                <div className="text-sm">
                  <p className="font-semibold">{t('conflictWarningTitle', 'Scheduling Conflict')}</p>
                  <p>{t('conflictWarningDesc', 'You already have an appointment scheduled at this time.')}</p>
                </div>
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label
                  htmlFor="appointment-start-time"
                  className="block mb-2 flex items-center justify-between text-foreground"
                >
                  <span className="flex items-center gap-2">
                    <Clock className="w-4 h-4" />
                    {t('startTime', 'Start Time')}
                  </span>
                  <span
                    className="text-destructive text-sm"
                    aria-label="required"
                  >
                    {t('required')}
                  </span>
                </label>
                <Select
                  value={startTime}
                  onValueChange={setStartTime}
                >
                  <SelectTrigger id="appointment-start-time" className="w-full" aria-required="true">
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
                  htmlFor="appointment-end-time"
                  className="block mb-2 flex items-center justify-between text-foreground"
                >
                  <span className="flex items-center gap-2">
                    <Clock className="w-4 h-4" />
                    {t('endTime', 'End Time')}
                  </span>
                  <span
                    className="text-destructive text-sm"
                    aria-label="required"
                  >
                    {t('required')}
                  </span>
                </label>
                <Select
                  value={endTime}
                  onValueChange={setEndTime}
                  disabled={!startTime}
                >
                  <SelectTrigger id="appointment-end-time" className="w-full" aria-required="true">
                    <SelectValue placeholder={t('selectTime')} />
                  </SelectTrigger>
                  <SelectContent>
                    {timeSlots.map((timeSlot) => {
                      if (!startTime) return null;
                      if (timeSlot <= startTime) return null;
                      return (
                        <SelectItem key={timeSlot} value={timeSlot}>
                          {formatTimeDisplay(timeSlot)}
                        </SelectItem>
                      );
                    })}
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>

          <div>
            <label
              htmlFor="appointment-message"
              className="block mb-2 flex items-center justify-between text-foreground"
            >
              <span className="flex items-center gap-2">
                <MessageSquare className="w-4 h-4" />
                {isBroker ? t('messageToClient') : t('messageToBroker')}
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
              disabled={!appointmentType || !date || !startTime || !endTime || (isBroker && !selectedClientId) || !selectedTransactionId || isPending || hasTimeConflict}
              className="flex-1 gap-2"
            >
              <Send className="w-5 h-5" />
              {isPending ? t('sending') : t('sendRequest')}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}