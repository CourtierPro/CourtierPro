import React, { useState, useRef, useEffect } from 'react';
import { ChevronLeft, Search, AlertCircle, CheckCircle } from 'lucide-react';
import axiosInstance from "@/api/axiosInstace";
import type { TransactionRequestDTO } from '@/api/types';
import { getStagesForSide, enumToLabel } from '@/utils/stages';


interface TransactionCreateFormProps {
  language: 'en' | 'fr';
  onNavigate: (route: string) => void;
}

interface Client {
  id: string;
  name: string;
  email: string;
}

const translations = {
  en: {
    backToTransactions: 'Back to Transactions',
    title: 'Create New Transaction',
    subtitle: 'Assign a client, define transaction details, and initiate the process',
    transactionSide: 'Transaction Side',
    required: 'Required',
    buySide: 'Buy-Side',
    sellSide: 'Sell-Side',
    buySideDescription: 'Client is purchasing a property',
    sellSideDescription: 'Client is selling a property',
    client: 'Client',
    searchClient: 'Search for a client...',
    selectClient: 'Select a client',
    noClientsFound: 'No clients found',
    propertyAddress: 'Property Address',
    propertyAddressPlaceholder: 'Enter full property address',
    initialStage: 'Initial Stage',
    selectStage: 'Select initial stage',
    cancel: 'Cancel',
    createTransaction: 'Create Transaction',
    errorRequired: 'This field is required',
    errorSelectClient: 'Please select a client',
    errorSelectSide: 'Please select a transaction side',
    errorSelectStage: 'Please select an initial stage',
    // buy/sell stage arrays removed — use centralized enums in src/utils/stages.ts
  },
  fr: {
    backToTransactions: 'Retour aux transactions',
    title: 'Créer une nouvelle transaction',
    subtitle: 'Assigner un client, définir les détails de la transaction et initier le processus',
    transactionSide: 'Type de transaction',
    required: 'Requis',
    buySide: 'Achat',
    sellSide: 'Vente',
    buySideDescription: 'Le client achète une propriété',
    sellSideDescription: 'Le client vend une propriété',
    client: 'Client',
    searchClient: 'Rechercher un client...',
    selectClient: 'Sélectionner un client',
    noClientsFound: 'Aucun client trouvé',
    propertyAddress: 'Adresse de la propriété',
    propertyAddressPlaceholder: 'Entrer l\'adresse complète de la propriété',
    initialStage: 'Étape initiale',
    selectStage: 'Sélectionner l\'étape initiale',
    cancel: 'Annuler',
    createTransaction: 'Créer la transaction',
    errorRequired: 'Ce champ est requis',
    errorSelectClient: 'Veuillez sélectionner un client',
    errorSelectSide: 'Veuillez sélectionner un type de transaction',
    errorSelectStage: 'Veuillez sélectionner une étape initiale',
    // buy/sell stage arrays removed — use centralized enums in src/utils/stages.ts
  },
};



export function TransactionCreateForm({ language, onNavigate }: TransactionCreateFormProps) {
  const [transactionSide, setTransactionSide] = useState<'buy' | 'sell' | ''>('');
  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [clientSearch, setClientSearch] = useState('');
  const [showClientDropdown, setShowClientDropdown] = useState(false);
  const [propertyAddress, setPropertyAddress] = useState('');
  const [initialStage, setInitialStage] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const [clients, setClients] = useState<Client[]>([]);

  const mockClients: Client[] = [
    { id: 'CLI-001', name: 'Sarah Johnson', email: 'sarah.johnson@email.com' },
    { id: 'CLI-002', name: 'Michael Chen', email: 'michael.chen@email.com' },
    { id: 'CLI-003', name: 'Emma Williams', email: 'emma.williams@email.com' },
    { id: 'CLI-004', name: 'David Brown', email: 'david.brown@email.com' },
    { id: 'CLI-005', name: 'Lisa Anderson', email: 'lisa.anderson@email.com' },
    { id: 'CLI-006', name: 'James Wilson', email: 'james.wilson@email.com' },
    { id: 'CLI-007', name: 'Maria Garcia', email: 'maria.garcia@email.com' },
    { id: 'CLI-008', name: 'Robert Taylor', email: 'robert.taylor@email.com' },
  ];

  // TODO: replace later with real API GET /clients for broker
  useEffect(() => {
    setClients(mockClients); // temporary until API exists
  }, []);

  const clientSearchRef = useRef<HTMLDivElement>(null);

  const t = translations[language];

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (clientSearchRef.current && !clientSearchRef.current.contains(event.target as Node)) {
        setShowClientDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Reset initial stage when transaction side changes
  useEffect(() => {
    setInitialStage('');
  }, [transactionSide]);

  // Filter clients based on search
  const filteredClients = clients.filter(
    (client) =>
      client.name.toLowerCase().includes(clientSearch.toLowerCase()) ||
      client.email.toLowerCase().includes(clientSearch.toLowerCase())
  );

  // Get available stage enums for the selected side
  const stageEnums = transactionSide
    ? getStagesForSide(transactionSide === 'buy' ? 'BUY_SIDE' : 'SELL_SIDE')
    : [];

  // Validate form
  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (!transactionSide) {
      newErrors.transactionSide = t.errorSelectSide;
    }

    if (!selectedClient) {
      newErrors.client = t.errorSelectClient;
    }

    if (!propertyAddress.trim()) {
      newErrors.propertyAddress = t.errorRequired;
    }

    if (!initialStage) {
      newErrors.initialStage = t.errorSelectStage;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle field blur
  const handleBlur = (field: string) => {
    setTouched({ ...touched, [field]: true });
  };

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Mark all fields as touched
    setTouched({
      transactionSide: true,
      client: true,
      propertyAddress: true,
      initialStage: true,
    });

    if (validateForm()) {
      try {
        const payload: TransactionRequestDTO = {
          clientId: selectedClient!.id,
          side: transactionSide === "buy" ? "BUY_SIDE" : "SELL_SIDE",
          initialStage: initialStage,
          propertyAddress: {
            street: propertyAddress,
            city: "Unknown",
            province: "Unknown",
            postalCode: "00000",
          },
        };

        // Debug: show exact payload being sent to backend
        // eslint-disable-next-line no-console
        console.debug('Creating transaction payload:', payload);

        const response = await axiosInstance.post(
          "/transactions",
          payload,
          { headers: { "x-broker-id": "BROKER1" }, withCredentials: true }
        );

        const createdId = response?.data?.transactionId;
        if (createdId) {
          onNavigate(`/transactions/${createdId}`);
        } else {
          // eslint-disable-next-line no-console
          console.error('Transaction created but no transactionId returned', response?.data);
          setErrors({ ...errors, form: language === 'en' ? 'Transaction created but no id returned' : 'Transaction créée mais aucun identifiant retourné' });
        }
      } catch (err: any) {
        // eslint-disable-next-line no-console
        console.error('Error creating transaction', err);

        // Log backend response body explicitly for debugging
        // eslint-disable-next-line no-console
        console.debug('Backend error response data:', err?.response?.data);

        const serverMsg = err?.response?.data;
        const newErrors: Record<string, string> = { ...errors };

        if (typeof serverMsg === 'string') {
          // Map known backend messages to fields
          if (serverMsg.toLowerCase().includes('initialstage')) {
            newErrors.initialStage = serverMsg;
          } else if (serverMsg.toLowerCase().includes('clientid')) {
            newErrors.client = serverMsg;
          } else if (serverMsg.toLowerCase().includes('brokerid')) {
            newErrors.client = serverMsg; // broker id is a header; show on form top
            newErrors.form = serverMsg;
          } else if (serverMsg.toLowerCase().includes('side')) {
            newErrors.transactionSide = serverMsg;
          } else if (serverMsg.toLowerCase().includes('propertyaddress.street')) {
            newErrors.propertyAddress = serverMsg;
          } else {
            newErrors.form = serverMsg;
          }
        } else if (err?.message) {
          newErrors.form = err.message;
        } else {
          newErrors.form = 'Unknown error';
        }

        setErrors(newErrors);
      }
    }
  };

  // Handle cancel
  const handleCancel = () => {
    onNavigate('/transactions');
  };

  // Check if form is valid for submit button
  const isFormValid = transactionSide && selectedClient && propertyAddress.trim() && initialStage;

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

      {/* Header */}
      <div>
        <h1 style={{ color: '#353535' }}>{t.title}</h1>
        <p style={{ color: '#353535', opacity: 0.7 }}>{t.subtitle}</p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} noValidate>
        {errors.form && (
          <div className="mb-4 p-3 rounded border border-red-200 bg-red-50" role="alert">
            <p style={{ color: '#ef4444' }}>{errors.form}</p>
          </div>
        )}
        <div
          className="p-6 rounded-xl shadow-md"
          style={{ backgroundColor: '#FFFFFF' }}
        >
          <div className="space-y-6">
            {/* Transaction Side */}
            <fieldset>
              <legend
                style={{ color: '#353535' }}
                className="mb-3 flex items-center gap-2"
              >
                {t.transactionSide}
                <span
                  style={{ color: '#ef4444', fontSize: '0.875rem' }}
                  aria-label="required"
                >
                  *
                </span>
              </legend>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Buy-Side Radio */}
                <label
                  className={`p-4 rounded-lg border-2 cursor-pointer transition-all ${
                    transactionSide === 'buy'
                      ? 'border-[#FF6B01] bg-[#FFF5F0]'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <input
                      type="radio"
                      name="transactionSide"
                      value="buy"
                      checked={transactionSide === 'buy'}
                      onChange={(e) => setTransactionSide(e.target.value as 'buy')}
                      onBlur={() => handleBlur('transactionSide')}
                      className="mt-1 w-4 h-4 focus:ring-2 focus:ring-[#FF6B01]"
                      style={{ accentColor: '#FF6B01' }}
                      aria-describedby="buy-side-description"
                    />
                    <div className="flex-1">
                      <p style={{ color: '#353535' }} className="mb-1">
                        {t.buySide}
                      </p>
                      <p
                        id="buy-side-description"
                        style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}
                      >
                        {t.buySideDescription}
                      </p>
                    </div>
                  </div>
                </label>

                {/* Sell-Side Radio */}
                <label
                  className={`p-4 rounded-lg border-2 cursor-pointer transition-all ${
                    transactionSide === 'sell'
                      ? 'border-[#FF6B01] bg-[#FFF5F0]'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <input
                      type="radio"
                      name="transactionSide"
                      value="sell"
                      checked={transactionSide === 'sell'}
                      onChange={(e) => setTransactionSide(e.target.value as 'sell')}
                      onBlur={() => handleBlur('transactionSide')}
                      className="mt-1 w-4 h-4 focus:ring-2 focus:ring-[#FF6B01]"
                      style={{ accentColor: '#FF6B01' }}
                      aria-describedby="sell-side-description"
                    />
                    <div className="flex-1">
                      <p style={{ color: '#353535' }} className="mb-1">
                        {t.sellSide}
                      </p>
                      <p
                        id="sell-side-description"
                        style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}
                      >
                        {t.sellSideDescription}
                      </p>
                    </div>
                  </div>
                </label>
              </div>

              {touched.transactionSide && errors.transactionSide && (
                <div
                  className="flex items-center gap-2 mt-2"
                  role="alert"
                  aria-live="polite"
                >
                  <AlertCircle className="w-4 h-4" style={{ color: '#ef4444' }} />
                  <p style={{ color: '#ef4444', fontSize: '0.875rem' }}>
                    {errors.transactionSide}
                  </p>
                </div>
              )}
            </fieldset>

            {/* Client and Property Address Row */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Client Search */}
              <div ref={clientSearchRef}>
                <label
                  htmlFor="client-search"
                  style={{ color: '#353535' }}
                  className="block mb-2 flex items-center gap-2"
                >
                  {t.client}
                  <span
                    style={{ color: '#ef4444', fontSize: '0.875rem' }}
                    aria-label="required"
                  >
                    *
                  </span>
                </label>

                <div className="relative">
                  <div className="relative">
                    <input
                      id="client-search"
                      type="text"
                      value={selectedClient ? selectedClient.name : clientSearch}
                      onChange={(e) => {
                        setClientSearch(e.target.value);
                        setSelectedClient(null);
                        setShowClientDropdown(true);
                      }}
                      onFocus={() => setShowClientDropdown(true)}
                      onBlur={() => handleBlur('client')}
                      placeholder={t.searchClient}
                      className="w-full p-3 pr-10 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent"
                      style={{ color: '#353535' }}
                      aria-describedby={touched.client && errors.client ? 'client-error' : undefined}
                      aria-invalid={touched.client && errors.client ? 'true' : 'false'}
                    />
                    <Search
                      className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5"
                      style={{ color: '#353535', opacity: 0.5 }}
                    />
                  </div>

                  {/* Dropdown */}
                  {showClientDropdown && (
                    <div
                      className="absolute z-10 w-full mt-2 rounded-lg border border-gray-200 shadow-lg max-h-60 overflow-y-auto"
                      style={{ backgroundColor: '#FFFFFF' }}
                      role="listbox"
                      aria-label="Client list"
                    >
                      {filteredClients.length === 0 ? (
                        <div className="p-4 text-center" style={{ color: '#353535', opacity: 0.7 }}>
                          {t.noClientsFound}
                        </div>
                      ) : (
                        filteredClients.map((client) => (
                          <button
                            key={client.id}
                            type="button"
                            onClick={() => {
                              setSelectedClient(client);
                              setClientSearch('');
                              setShowClientDropdown(false);
                            }}
                            className="w-full p-3 text-left hover:bg-gray-50 focus:outline-none focus:bg-gray-50 transition-colors"
                            role="option"
                            aria-selected={selectedClient?.id === client.id}
                          >
                            <p style={{ color: '#353535' }}>{client.name}</p>
                            <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}>
                              {client.email}
                            </p>
                          </button>
                        ))
                      )}
                    </div>
                  )}
                </div>

                {selectedClient && (
                  <div
                    className="flex items-center gap-2 mt-2 p-2 rounded-lg"
                    style={{ backgroundColor: '#f0fdf4' }}
                  >
                    <CheckCircle className="w-4 h-4" style={{ color: '#10b981' }} />
                    <p style={{ color: '#10b981', fontSize: '0.875rem' }}>
                      {selectedClient.name} {language === 'en' ? 'selected' : 'sélectionné'}
                    </p>
                  </div>
                )}

                {touched.client && errors.client && !selectedClient && (
                  <div
                    id="client-error"
                    className="flex items-center gap-2 mt-2"
                    role="alert"
                    aria-live="polite"
                  >
                    <AlertCircle className="w-4 h-4" style={{ color: '#ef4444' }} />
                    <p style={{ color: '#ef4444', fontSize: '0.875rem' }}>
                      {errors.client}
                    </p>
                  </div>
                )}
              </div>

              {/* Property Address */}
              <div>
                <label
                  htmlFor="property-address"
                  style={{ color: '#353535' }}
                  className="block mb-2 flex items-center gap-2"
                >
                  {t.propertyAddress}
                  <span
                    style={{ color: '#ef4444', fontSize: '0.875rem' }}
                    aria-label="required"
                  >
                    *
                  </span>
                </label>

                <textarea
                  id="property-address"
                  value={propertyAddress}
                  onChange={(e) => setPropertyAddress(e.target.value)}
                  onBlur={() => handleBlur('propertyAddress')}
                  placeholder={t.propertyAddressPlaceholder}
                  rows={3}
                  className="w-full p-3 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent resize-none"
                  style={{ color: '#353535' }}
                  aria-describedby={touched.propertyAddress && errors.propertyAddress ? 'address-error' : undefined}
                  aria-invalid={touched.propertyAddress && errors.propertyAddress ? 'true' : 'false'}
                />

                {touched.propertyAddress && errors.propertyAddress && (
                  <div
                    id="address-error"
                    className="flex items-center gap-2 mt-2"
                    role="alert"
                    aria-live="polite"
                  >
                    <AlertCircle className="w-4 h-4" style={{ color: '#ef4444' }} />
                    <p style={{ color: '#ef4444', fontSize: '0.875rem' }}>
                      {errors.propertyAddress}
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Initial Stage */}
            <div>
              <label
                htmlFor="initial-stage"
                style={{ color: '#353535' }}
                className="block mb-2 flex items-center gap-2"
              >
                {t.initialStage}
                <span
                  style={{ color: '#ef4444', fontSize: '0.875rem' }}
                  aria-label="required"
                >
                  *
                </span>
              </label>

              <select
                id="initial-stage"
                value={initialStage}
                onChange={(e) => setInitialStage(e.target.value)}
                onBlur={() => handleBlur('initialStage')}
                disabled={!transactionSide}
                className="w-full p-3 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent disabled:opacity-50 disabled:cursor-not-allowed"
                style={{ color: '#353535' }}
                aria-describedby={touched.initialStage && errors.initialStage ? 'stage-error' : undefined}
                aria-invalid={touched.initialStage && errors.initialStage ? 'true' : 'false'}
              >
                <option value="">{t.selectStage}</option>
                {stageEnums.map((stageEnum) => (
                  <option key={stageEnum} value={stageEnum}>
                    {enumToLabel(stageEnum)}
                  </option>
                ))}
              </select>

              {!transactionSide && (
                <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }} className="mt-2">
                  {language === 'en'
                    ? 'Select a transaction side first'
                    : 'Sélectionnez d\'abord un type de transaction'}
                </p>
              )}

              {touched.initialStage && errors.initialStage && (
                <div
                  id="stage-error"
                  className="flex items-center gap-2 mt-2"
                  role="alert"
                  aria-live="polite"
                >
                  <AlertCircle className="w-4 h-4" style={{ color: '#ef4444' }} />
                  <p style={{ color: '#ef4444', fontSize: '0.875rem' }}>
                    {errors.initialStage}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Form Actions */}
        <div className="flex flex-col sm:flex-row items-center gap-4 mt-6">
          <button
            type="button"
            onClick={handleCancel}
            className="w-full sm:w-auto px-6 py-3 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-300 focus:ring-offset-2 transition-colors border-2 border-gray-200"
            style={{ color: '#353535' }}
          >
            {t.cancel}
          </button>

          <button
            type="submit"
            disabled={!isFormValid}
            className="w-full sm:w-auto px-6 py-3 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            style={{
              backgroundColor: isFormValid ? '#FF6B01' : '#e5e7eb',
              color: isFormValid ? '#FFFFFF' : '#9ca3af',
            }}
          >
            {t.createTransaction}
          </button>
        </div>
      </form>
    </div>
  );
}
