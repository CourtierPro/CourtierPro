import React, { useState, useRef, useEffect } from 'react';
import { ChevronLeft, Search, AlertCircle, CheckCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Textarea } from '@/shared/components/ui/textarea';
import { RadioGroup, RadioGroupItem } from '@/shared/components/ui/radio-group';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select';
import { useCreateTransaction } from '../api/mutations';
import type { TransactionRequestDTO } from '@/shared/api/types';
import { getStagesForSide, enumToLabel } from '@/shared/utils/stages';
import { logError, getErrorMessage } from '@/shared/utils/error-utils';
import { useClientsForDisplay } from '@/features/clients';


interface TransactionCreateFormProps {
  onNavigate: (route: string) => void;
}

interface Client {
  id: string;
  name: string;
  email: string;
}

export function TransactionCreateForm({ onNavigate }: TransactionCreateFormProps) {
  const { t, i18n } = useTranslation('transactions');
  const [transactionSide, setTransactionSide] = useState<'buy' | 'sell' | ''>('');
  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [clientSearch, setClientSearch] = useState('');
  const [showClientDropdown, setShowClientDropdown] = useState(false);
  const [propertyAddress, setPropertyAddress] = useState('');
  const [initialStage, setInitialStage] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const { data: clients = [] } = useClientsForDisplay();

  const clientSearchRef = useRef<HTMLDivElement>(null);


  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (clientSearchRef.current && !clientSearchRef.current.contains(event.target as Node)) {
        setShowClientDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    if (transactionSide) {
      setInitialStage('');
    }
  }, [transactionSide]);

  const filteredClients = clients.filter(
    (client) =>
      client.name.toLowerCase().includes(clientSearch.toLowerCase()) ||
      client.email.toLowerCase().includes(clientSearch.toLowerCase())
  );

  const stageEnums = transactionSide
    ? getStagesForSide(transactionSide === 'buy' ? 'BUY_SIDE' : 'SELL_SIDE')
    : [];

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (!transactionSide) {
      newErrors.transactionSide = t('errorSelectSide');
    }

    if (!selectedClient) {
      newErrors.client = t('errorSelectClient');
    }

    if (!propertyAddress.trim()) {
      newErrors.propertyAddress = t('errorRequired');
    }

    if (!initialStage) {
      newErrors.initialStage = t('errorSelectStage');
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleBlur = (field: string) => {
    setTouched({ ...touched, [field]: true });
  };

  const createTransaction = useCreateTransaction();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

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

        const response = await createTransaction.mutateAsync(payload);

        if (response && response.transactionId) {
          toast.success(t('transactionCreated'));
          onNavigate(`/transactions/${response.transactionId}`);
        } else {
          toast.error(t('errorCreatingTransaction'));
        }
      } catch (err: unknown) {
        const errorMessage = getErrorMessage(err, t('errorCreatingTransaction'));
        toast.error(errorMessage);

        if (err instanceof Error) {
          logError(err);
        }

        const newErrors: Record<string, string> = { ...errors };
        const serverMsg = errorMessage;

        // Map known backend messages to fields
        if (serverMsg.toLowerCase().includes('initialstage')) {
          newErrors.initialStage = serverMsg;
        } else if (serverMsg.toLowerCase().includes('clientid')) {
          newErrors.client = serverMsg;
        } else if (serverMsg.toLowerCase().includes('brokerid')) {
          newErrors.client = serverMsg;
          newErrors.form = serverMsg;
        } else if (serverMsg.toLowerCase().includes('side')) {
          newErrors.transactionSide = serverMsg;
        } else if (serverMsg.toLowerCase().includes('propertyaddress.street')) {
          newErrors.propertyAddress = serverMsg;
        } else {
          newErrors.form = serverMsg;
        }

        setErrors(newErrors);
      }
    }
  };

  const handleCancel = () => {
    onNavigate('/transactions');
  };

  const isFormValid = transactionSide && selectedClient && propertyAddress.trim() && initialStage;

  return (
    <div className="space-y-6">
      <Button
        variant="ghost"
        onClick={() => onNavigate('/transactions')}
        className="gap-2 text-[#FF6B01] hover:text-[#FF6B01]/80 hover:bg-[#FF6B01]/10"
      >
        <ChevronLeft className="w-5 h-5" />
        {t('backToTransactions')}
      </Button>

      <div>
        <h1 className="text-foreground">{t('createTransactionTitle')}</h1>
        <p className="text-muted-foreground">{t('createSubtitle')}</p>
      </div>

      <form onSubmit={handleSubmit} noValidate>
        {errors.form && (
          <div className="mb-4 p-3 rounded border border-red-200 bg-red-50" role="alert">
            <p className="text-destructive">{errors.form}</p>
          </div>
        )}
        <div
          className="p-6 rounded-xl shadow-md bg-white"
        >
          <div className="space-y-6">
            <fieldset>
              <legend
                className="mb-3 flex items-center gap-2 text-foreground"
              >
                {t('transactionSide')}
                <span
                  className="text-destructive text-sm"
                  aria-label="required"
                >
                  *
                </span>
              </legend>

              <RadioGroup
                value={transactionSide}
                onValueChange={(val) => {
                  setTransactionSide(val as 'buy' | 'sell');
                  handleBlur('transactionSide');
                }}
                className="grid grid-cols-1 md:grid-cols-2 gap-4"
              >
                <label
                  className={`p-4 rounded-lg border-2 cursor-pointer transition-all ${transactionSide === 'buy'
                    ? 'border-[#FF6B01] bg-[#FFF5F0]'
                    : 'border-gray-200 hover:border-gray-300'
                    }`}
                >
                  <div className="flex items-start gap-3">
                    <RadioGroupItem value="buy" id="buy" className="mt-1" />
                    <div className="flex-1 cursor-pointer" onClick={() => setTransactionSide('buy')}>
                      <p className="text-foreground mb-1 font-medium">
                        {t('buySide')}
                      </p>
                      <p
                        id="buy-side-description"
                        className="text-muted-foreground text-sm"
                      >
                        {t('buySideDescription')}
                      </p>
                    </div>
                  </div>
                </label>

                <label
                  className={`p-4 rounded-lg border-2 cursor-pointer transition-all ${transactionSide === 'sell'
                    ? 'border-[#FF6B01] bg-[#FFF5F0]'
                    : 'border-gray-200 hover:border-gray-300'
                    }`}
                >
                  <div className="flex items-start gap-3">
                    <RadioGroupItem value="sell" id="sell" className="mt-1" />
                    <div className="flex-1 cursor-pointer" onClick={() => setTransactionSide('sell')}>
                      <p className="text-foreground mb-1 font-medium">
                        {t('sellSide')}
                      </p>
                      <p
                        id="sell-side-description"
                        className="text-muted-foreground text-sm"
                      >
                        {t('sellSideDescription')}
                      </p>
                    </div>
                  </div>
                </label>
              </RadioGroup>

              {touched.transactionSide && errors.transactionSide && (
                <div
                  className="flex items-center gap-2 mt-2"
                  role="alert"
                  aria-live="polite"
                >
                  <AlertCircle className="w-4 h-4 text-destructive" />
                  <p className="text-destructive text-sm">
                    {errors.transactionSide}
                  </p>
                </div>
              )}
            </fieldset>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div ref={clientSearchRef}>
                <label
                  htmlFor="client-search"
                  className="block mb-2 flex items-center gap-2 text-foreground"
                >
                  {t('client')}
                  <span
                    className="text-destructive text-sm"
                    aria-label="required"
                  >
                    *
                  </span>
                </label>

                <div className="relative">
                  <div className="relative">
                    <Input
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
                      placeholder={t('searchClient')}
                      className="pr-10"
                      aria-describedby={touched.client && errors.client ? 'client-error' : undefined}
                      aria-invalid={touched.client && errors.client ? 'true' : 'false'}
                    />
                    <Search
                      className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-muted-foreground/50"
                    />
                  </div>

                  {showClientDropdown && (
                    <div
                      className="absolute z-10 w-full mt-2 rounded-lg border border-gray-200 shadow-lg max-h-60 overflow-y-auto bg-white"
                      role="listbox"
                      aria-label="Client list"
                    >
                      {filteredClients.length === 0 ? (
                        <div className="p-4 text-center text-muted-foreground">
                          {t('noClientsFound')}
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
                            <p className="text-foreground">{client.name}</p>
                            <p className="text-muted-foreground text-sm">
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
                    className="flex items-center gap-2 mt-2 p-2 rounded-lg bg-emerald-50"
                  >
                    <CheckCircle className="w-4 h-4 text-emerald-500" />
                    <p className="text-emerald-500 text-sm">
                      {selectedClient.name} {i18n.language === 'en' ? 'selected' : 'sélectionné'}
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
                    <AlertCircle className="w-4 h-4 text-destructive" />
                    <p className="text-destructive text-sm">
                      {errors.client}
                    </p>
                  </div>
                )}
              </div>

              <div>
                <label
                  htmlFor="property-address"
                  className="block mb-2 flex items-center gap-2 text-foreground"
                >
                  {t('propertyAddress')}
                  <span
                    className="text-destructive text-sm"
                    aria-label="required"
                  >
                    *
                  </span>
                </label>

                <Textarea
                  id="property-address"
                  value={propertyAddress}
                  onChange={(e) => setPropertyAddress(e.target.value)}
                  onBlur={() => handleBlur('propertyAddress')}
                  placeholder={t('propertyAddressPlaceholder')}
                  rows={3}
                  className="resize-none"
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
                    <AlertCircle className="w-4 h-4 text-destructive" />
                    <p className="text-destructive text-sm">
                      {errors.propertyAddress}
                    </p>
                  </div>
                )}
              </div>
            </div>

            <div>
              <label
                htmlFor="initial-stage"
                className="block mb-2 flex items-center gap-2 text-foreground"
              >
                {t('initialStage')}
                <span
                  className="text-destructive text-sm"
                  aria-label="required"
                >
                  *
                </span>
              </label>

              <Select
                value={initialStage}
                onValueChange={(value) => {
                  setInitialStage(value);
                  handleBlur('initialStage');
                }}
                disabled={!transactionSide}
              >
                <SelectTrigger
                  id="initial-stage"
                  className="w-full"
                  aria-describedby={touched.initialStage && errors.initialStage ? 'stage-error' : undefined}
                  aria-invalid={touched.initialStage && errors.initialStage ? 'true' : 'false'}
                >
                  <SelectValue placeholder={t('selectInitialStage')} />
                </SelectTrigger>
                <SelectContent>
                  {stageEnums.map((stageEnum) => (
                    <SelectItem key={stageEnum} value={stageEnum}>
                      {enumToLabel(stageEnum)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              {!transactionSide && (
                <p className="text-muted-foreground text-sm mt-2">
                  {i18n.language === 'en'
                    ? t('errorSelectSide')
                    : t('errorSelectSide')}
                </p>
              )}

              {touched.initialStage && errors.initialStage && (
                <div
                  id="stage-error"
                  className="flex items-center gap-2 mt-2"
                  role="alert"
                  aria-live="polite"
                >
                  <AlertCircle className="w-4 h-4 text-destructive" />
                  <p className="text-destructive text-sm">
                    {errors.initialStage}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-4 mt-6">
          <Button
            type="button"
            variant="outline"
            onClick={handleCancel}
            className="w-full sm:w-auto"
          >
            {t('cancel')}
          </Button>

          <Button
            type="submit"
            disabled={!isFormValid}
            className="w-full sm:w-auto bg-[#FF6B01] hover:bg-[#FF6B01]/90"
          >
            {t('createTransaction')}
          </Button>
        </div>
      </form>
    </div>
  );
}
