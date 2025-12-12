import React, { useState, useRef, useEffect } from 'react';
import { ChevronLeft, Search, AlertCircle, CheckCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
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
import { getStagesForSide, getStageLabel } from '@/shared/utils/stages';
import { logError, getErrorMessage } from '@/shared/utils/error-utils';
import { useClientsForDisplay } from '@/features/clients';


interface TransactionCreateFormProps {
  onNavigate: (route: string) => void;
  isModal?: boolean;
}

interface Client {
  id: string;
  name: string;
  email: string;
}

export function TransactionCreateForm({ onNavigate, isModal = false }: TransactionCreateFormProps) {
  const { t } = useTranslation('transactions');
  const { t: tTx } = useTranslation('transactions');
  const [transactionSide, setTransactionSide] = useState<'buy' | 'sell' | ''>('');
  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [clientSearch, setClientSearch] = useState('');
  const [showClientDropdown, setShowClientDropdown] = useState(false);

  // Address fields
  const [streetNumber, setStreetNumber] = useState('');
  const [streetName, setStreetName] = useState('');
  const [city, setCity] = useState('');
  const [province, setProvince] = useState('');
  const [postalCode, setPostalCode] = useState('');

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

    if (!streetNumber.trim()) newErrors.streetNumber = t('errorRequired');
    if (!streetName.trim()) newErrors.streetName = t('errorRequired');
    if (!city.trim()) newErrors.city = t('errorRequired');
    if (!province.trim()) newErrors.province = t('errorRequired');
    if (!postalCode.trim()) newErrors.postalCode = t('errorRequired');

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
      streetNumber: true,
      streetName: true,
      city: true,
      province: true,
      postalCode: true,
      initialStage: true,
    });

    if (validateForm()) {
      try {
        const payload: TransactionRequestDTO = {
          clientId: selectedClient!.id,
          side: transactionSide === "buy" ? "BUY_SIDE" : "SELL_SIDE",
          initialStage: initialStage,
          propertyAddress: {
            street: `${streetNumber.trim()} ${streetName.trim()}`,
            city: city.trim(),
            province: province.trim(),
            postalCode: postalCode.trim(),
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
          newErrors.streetNumber = serverMsg; // Assign to first field for visibility
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

  const isFormValid =
    transactionSide &&
    selectedClient &&
    streetNumber.trim() &&
    streetName.trim() &&
    city.trim() &&
    province.trim() &&
    postalCode.trim() &&
    initialStage;

  return (
    <div className="space-y-6">
      {!isModal && (
        <>
          <Button
            variant="ghost"
            onClick={() => onNavigate('/transactions')}
            className="gap-2 text-primary hover:text-primary/80 hover:bg-primary/10"
          >
            <ChevronLeft className="w-5 h-5" />
            {t('backToTransactions')}
          </Button>

          <div>
            <h1 className="text-foreground">{t('createTransactionTitle')}</h1>
            <p className="text-muted-foreground">{t('createSubtitle')}</p>
          </div>
        </>
      )}

      <form onSubmit={handleSubmit} noValidate>
        {errors.form && (
          <div className="mb-6 p-4 rounded-lg border border-destructive/20 bg-destructive/10" role="alert">
            <p className="text-destructive font-medium flex items-center gap-2">
              <AlertCircle className="w-5 h-5" />
              {errors.form}
            </p>
          </div>
        )}

        <div className="space-y-8">
          {/* Section 1: Transaction Basics */}
          <div className={`space-y-6 ${isModal ? '' : 'bg-card p-6 rounded-xl border border-border shadow-sm'}`}>
            <h2 className="text-lg font-semibold text-foreground border-b border-border pb-2">
              {t('transactionDetails')}
            </h2>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {/* Left Column: Side Selection */}
              <fieldset className="space-y-4">
                <legend className="block text-sm font-medium text-foreground mb-4">
                  {t('transactionSide')} <span className="text-destructive">*</span>
                </legend>

                <RadioGroup
                  value={transactionSide}
                  onValueChange={(val) => {
                    setTransactionSide(val as 'buy' | 'sell');
                    setInitialStage('');
                    handleBlur('transactionSide');
                  }}
                  className="grid grid-cols-1 sm:grid-cols-2 gap-4"
                >
                  <label
                    className={`relative flex flex-col p-4 rounded-xl border-2 cursor-pointer transition-all hover:bg-muted/50 ${transactionSide === 'buy'
                      ? 'border-primary bg-primary/5 dark:bg-primary/10'
                      : 'border-border hover:border-sidebar-primary/50'
                      }`}
                  >
                    <RadioGroupItem value="buy" id="buy" className="sr-only" />
                    <span className="font-semibold text-foreground mb-1">{t('buySide')}</span>
                    <span className="text-xs text-muted-foreground">{t('buySideDescription')}</span>
                    {transactionSide === 'buy' && (
                      <div className="absolute top-3 right-3 text-primary">
                        <CheckCircle className="w-5 h-5" />
                      </div>
                    )}
                  </label>

                  <label
                    className={`relative flex flex-col p-4 rounded-xl border-2 cursor-pointer transition-all hover:bg-muted/50 ${transactionSide === 'sell'
                      ? 'border-primary bg-primary/5 dark:bg-primary/10'
                      : 'border-border hover:border-sidebar-primary/50'
                      }`}
                  >
                    <RadioGroupItem value="sell" id="sell" className="sr-only" />
                    <span className="font-semibold text-foreground mb-1">{t('sellSide')}</span>
                    <span className="text-xs text-muted-foreground">{t('sellSideDescription')}</span>
                    {transactionSide === 'sell' && (
                      <div className="absolute top-3 right-3 text-primary">
                        <CheckCircle className="w-5 h-5" />
                      </div>
                    )}
                  </label>
                </RadioGroup>
                {touched.transactionSide && errors.transactionSide && (
                  <p className="text-destructive text-sm mt-1">{errors.transactionSide}</p>
                )}
              </fieldset>

              {/* Right Column: Client Selection */}
              <div ref={clientSearchRef} className="space-y-2">
                <label htmlFor="client-search" className="block text-sm font-medium text-foreground">
                  {t('client')} <span className="text-destructive">*</span>
                </label>

                <div className="relative">
                  <Input
                    id="client-search"
                    /* ... existing input props ... */
                    value={selectedClient ? selectedClient.name : clientSearch}
                    onChange={(e) => {
                      setClientSearch(e.target.value);
                      setSelectedClient(null);
                      setShowClientDropdown(true);
                    }}
                    onFocus={() => setShowClientDropdown(true)}
                    onBlur={() => handleBlur('client')}
                    placeholder={t('searchClient')}
                    className="pr-10 bg-background"
                    aria-invalid={touched.client && errors.client ? 'true' : 'false'}
                  />
                  <Search className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />

                  {showClientDropdown && (
                    <div className="absolute z-20 w-full mt-1 rounded-md border border-border bg-popover text-popover-foreground shadow-lg max-h-60 overflow-y-auto">
                      {filteredClients.length === 0 ? (
                        <div className="p-3 text-sm text-center text-muted-foreground">{t('noClientsFound')}</div>
                      ) : (
                        filteredClients.map(client => (
                          <button
                            key={client.id}
                            type="button"
                            onClick={() => {
                              setSelectedClient(client);
                              setClientSearch('');
                              setShowClientDropdown(false);
                            }}
                            className="w-full text-left px-3 py-2 text-sm hover:bg-accent hover:text-accent-foreground transition-colors"
                          >
                            <div className="font-medium">{client.name}</div>
                            <div className="text-xs text-muted-foreground">{client.email}</div>
                          </button>
                        ))
                      )}
                    </div>
                  )}
                </div>

                {selectedClient && (
                  <div className="flex items-center gap-2 mt-2 p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-sm">
                    <CheckCircle className="w-4 h-4" />
                    <span>{selectedClient.name}</span>
                  </div>
                )}
                {touched.client && errors.client && !selectedClient && (
                  <p className="text-destructive text-sm mt-1">{errors.client}</p>
                )}
              </div>
            </div>
          </div>

          {/* Section 2: Property Address */}
          <div className={`space-y-6 ${isModal ? '' : 'bg-card p-6 rounded-xl border border-border shadow-sm'}`}>
            <h2 className="text-lg font-semibold text-foreground border-b border-border pb-2">
              {t('propertyAddress')}
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="md:col-span-1">
                <label className="block text-sm font-medium text-foreground mb-1.5">{t('streetNumber')} <span className="text-destructive">*</span></label>
                <Input
                  value={streetNumber}
                  onChange={e => setStreetNumber(e.target.value)}
                  onBlur={() => handleBlur('streetNumber')}
                  className="bg-background"
                  aria-invalid={touched.streetNumber && errors.streetNumber ? 'true' : 'false'}
                />
                {touched.streetNumber && errors.streetNumber && <p className="text-destructive text-xs mt-1">{errors.streetNumber}</p>}
              </div>
              <div className="md:col-span-3">
                <label className="block text-sm font-medium text-foreground mb-1.5">{t('streetName')} <span className="text-destructive">*</span></label>
                <Input
                  value={streetName}
                  onChange={e => setStreetName(e.target.value)}
                  onBlur={() => handleBlur('streetName')}
                  className="bg-background"
                  aria-invalid={touched.streetName && errors.streetName ? 'true' : 'false'}
                />
                {touched.streetName && errors.streetName && <p className="text-destructive text-xs mt-1">{errors.streetName}</p>}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-foreground mb-1.5">{t('city')} <span className="text-destructive">*</span></label>
                <Input
                  value={city}
                  onChange={e => setCity(e.target.value)}
                  onBlur={() => handleBlur('city')}
                  className="bg-background"
                />
                {touched.city && errors.city && <p className="text-destructive text-xs mt-1">{errors.city}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground mb-1.5">{t('province')} <span className="text-destructive">*</span></label>
                <Input
                  value={province}
                  onChange={e => setProvince(e.target.value)}
                  onBlur={() => handleBlur('province')}
                  className="bg-background"
                />
                {touched.province && errors.province && <p className="text-destructive text-xs mt-1">{errors.province}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground mb-1.5">{t('postalCode')} <span className="text-destructive">*</span></label>
                <Input
                  value={postalCode}
                  onChange={e => setPostalCode(e.target.value)}
                  onBlur={() => handleBlur('postalCode')}
                  className="bg-background"
                />
                {touched.postalCode && errors.postalCode && <p className="text-destructive text-xs mt-1">{errors.postalCode}</p>}
              </div>
            </div>
          </div>

          {/* Section 3: Initial Stage */}
          <div className={`space-y-6 ${isModal ? '' : 'bg-card p-6 rounded-xl border border-border shadow-sm'}`}>
            <h2 className="text-lg font-semibold text-foreground border-b border-border pb-2">
              {t('initialStage')}
            </h2>
            <div>
              <label className="block text-sm font-medium text-foreground mb-2">
                {t('selectInitialStage')} <span className="text-destructive">*</span>
              </label>
              <Select
                value={initialStage}
                onValueChange={(val) => {
                  setInitialStage(val);
                  handleBlur('initialStage');
                }}
                disabled={!transactionSide}
              >
                <SelectTrigger className="w-full md:w-1/2 bg-background">
                  <SelectValue placeholder={t('selectInitialStage')} />
                </SelectTrigger>
                <SelectContent>
                  {stageEnums.map(stage => (
                    <SelectItem key={stage} value={stage}>{getStageLabel(stage, tTx, transactionSide === 'buy' ? 'BUY_SIDE' : 'SELL_SIDE')}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {!transactionSide && (
                <p className="text-muted-foreground text-sm mt-2">{t('errorSelectSide')}</p>
              )}
              {touched.initialStage && errors.initialStage && (
                <p className="text-destructive text-sm mt-1">{errors.initialStage}</p>
              )}
            </div>
          </div>
        </div>

        <div className="flex flex-col-reverse sm:flex-row items-center justify-end gap-3 mt-8 pt-4 border-t border-border">
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
            className="w-full sm:w-auto min-w-[150px]"
          >
            {t('createTransaction')}
          </Button>
        </div>
      </form>
    </div>
  );
}
