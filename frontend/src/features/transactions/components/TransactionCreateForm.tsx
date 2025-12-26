import { useState, useRef, useEffect } from 'react';
import { ChevronLeft, Search, CheckCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

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
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form';

import { useCreateTransaction } from '../api/mutations';
import type { TransactionRequestDTO } from '@/shared/api/types';
import { getStagesForSide, getStageLabel } from '@/shared/utils/stages';
import { logError, getErrorMessage } from '@/shared/utils/error-utils';
import { useClientsForDisplay } from '@/features/clients';
import { transactionCreateSchema, type TransactionCreateFormValues } from '@/shared/schemas';

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

  // Local state for client search UI
  const [clientSearch, setClientSearch] = useState('');
  const [showClientDropdown, setShowClientDropdown] = useState(false);
  const [selectedClientForDisplay, setSelectedClientForDisplay] = useState<Client | null>(null);

  const clientSearchRef = useRef<HTMLDivElement>(null);

  const { data: clients = [] } = useClientsForDisplay();
  const createTransaction = useCreateTransaction();

  // Initialize form
  const form = useForm<TransactionCreateFormValues>({
    resolver: zodResolver(transactionCreateSchema),
    mode: 'onBlur',
    defaultValues: {
      transactionSide: undefined,
      clientId: '',
      streetNumber: '',
      streetName: '',
      city: '',
      province: '',
      postalCode: '',
      initialStage: '',
    },
  });

  const { setValue, control } = form;
  const transactionSide = useWatch({ control, name: 'transactionSide' });

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

  const onSubmit = async (data: TransactionCreateFormValues) => {
    try {
      const payload: TransactionRequestDTO = {
        clientId: data.clientId,
        side: data.transactionSide === "buy" ? "BUY_SIDE" : "SELL_SIDE",
        initialStage: data.initialStage,
        propertyAddress: {
          street: `${data.streetNumber.trim()} ${data.streetName.trim()}`,
          city: data.city.trim(),
          province: data.province.trim(),
          postalCode: data.postalCode.trim(),
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
    }
  };

  const handleClientSelect = (client: Client) => {
    setSelectedClientForDisplay(client);
    setValue('clientId', client.id, { shouldValidate: true });
    setClientSearch('');
    setShowClientDropdown(false);
  };

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

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">

          {/* Section 1: Transaction Basics */}
          <div className={`space-y-6 ${isModal ? '' : 'bg-card p-6 rounded-xl border border-border shadow-sm'}`}>
            <h2 className="text-lg font-semibold text-foreground border-b border-border pb-2">
              {t('transactionDetails')}
            </h2>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {/* Left Column: Side Selection */}
              <FormField
                control={form.control}
                name="transactionSide"
                render={({ field }) => (
                  <FormItem className="space-y-4">
                    <FormLabel className="block text-sm font-medium text-foreground mb-4">
                      {t('transactionSide')} <span className="text-destructive">*</span>
                    </FormLabel>
                    <FormControl>
                      <RadioGroup
                        onValueChange={(val) => {
                          field.onChange(val);
                          setValue('initialStage', ''); // Reset stage on side change
                        }}
                        defaultValue={field.value}
                        className="grid grid-cols-1 sm:grid-cols-2 gap-4"
                      >
                        <FormItem>
                          <label
                            className={`relative flex flex-col p-4 rounded-xl border-2 cursor-pointer transition-all hover:bg-muted/50 ${field.value === 'buy'
                              ? 'border-primary bg-primary/5 dark:bg-primary/10'
                              : 'border-border hover:border-sidebar-primary/50'
                              }`}
                          >
                            <FormControl>
                              <RadioGroupItem value="buy" className="sr-only" />
                            </FormControl>
                            <span className="font-semibold text-foreground mb-1">{t('buySide')}</span>
                            <span className="text-xs text-muted-foreground">{t('buySideDescription')}</span>
                            {field.value === 'buy' && (
                              <div className="absolute top-3 right-3 text-primary">
                                <CheckCircle className="w-5 h-5" />
                              </div>
                            )}
                          </label>
                        </FormItem>
                        <FormItem>
                          <label
                            className={`relative flex flex-col p-4 rounded-xl border-2 cursor-pointer transition-all hover:bg-muted/50 ${field.value === 'sell'
                              ? 'border-primary bg-primary/5 dark:bg-primary/10'
                              : 'border-border hover:border-sidebar-primary/50'
                              }`}
                          >
                            <FormControl>
                              <RadioGroupItem value="sell" className="sr-only" />
                            </FormControl>
                            <span className="font-semibold text-foreground mb-1">{t('sellSide')}</span>
                            <span className="text-xs text-muted-foreground">{t('sellSideDescription')}</span>
                            {field.value === 'sell' && (
                              <div className="absolute top-3 right-3 text-primary">
                                <CheckCircle className="w-5 h-5" />
                              </div>
                            )}
                          </label>
                        </FormItem>
                      </RadioGroup>
                    </FormControl>
                    <FormMessage>{form.formState.errors.transactionSide?.message && t(form.formState.errors.transactionSide?.message)}</FormMessage>
                  </FormItem>
                )}
              />

              {/* Right Column: Client Selection */}
              <FormField
                control={form.control}
                name="clientId"
                render={({ field }) => (
                  <FormItem ref={clientSearchRef} className="space-y-2">
                    <FormLabel htmlFor="client-search" className="block text-sm font-medium text-foreground">
                      {t('client')} <span className="text-destructive">*</span>
                    </FormLabel>
                    <div className="relative">
                      <FormControl>
                        <Input
                          id="client-search"
                          value={selectedClientForDisplay ? selectedClientForDisplay.name : clientSearch}
                          onChange={(e) => {
                            setClientSearch(e.target.value);
                            setSelectedClientForDisplay(null);
                            field.onChange(''); // Clear form value
                            setShowClientDropdown(true);
                          }}
                          onFocus={() => setShowClientDropdown(true)}
                          placeholder={t('searchClient')}
                          className="pr-10 bg-background"
                        />
                      </FormControl>
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
                                onClick={() => handleClientSelect(client)}
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
                    {selectedClientForDisplay && (
                      <div className="flex items-center gap-2 mt-2 p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-sm">
                        <CheckCircle className="w-4 h-4" />
                        <span>{selectedClientForDisplay.name}</span>
                      </div>
                    )}
                    <FormMessage>{form.formState.errors.clientId?.message && t(form.formState.errors.clientId?.message)}</FormMessage>
                  </FormItem>
                )}
              />
            </div>
          </div>

          {/* Section 2: Property Address */}
          <div className={`space-y-6 ${isModal ? '' : 'bg-card p-6 rounded-xl border border-border shadow-sm'}`}>
            <h2 className="text-lg font-semibold text-foreground border-b border-border pb-2">
              {t('propertyAddress')}
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="md:col-span-1">
                <FormField
                  control={form.control}
                  name="streetNumber"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('streetNumber')} <span className="text-destructive">*</span></FormLabel>
                      <FormControl>
                        <Input {...field} className="bg-background" />
                      </FormControl>
                      <FormMessage>{form.formState.errors.streetNumber?.message && t(form.formState.errors.streetNumber?.message)}</FormMessage>
                    </FormItem>
                  )}
                />
              </div>
              <div className="md:col-span-3">
                <FormField
                  control={form.control}
                  name="streetName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('streetName')} <span className="text-destructive">*</span></FormLabel>
                      <FormControl>
                        <Input {...field} className="bg-background" />
                      </FormControl>
                      <FormMessage>{form.formState.errors.streetName?.message && t(form.formState.errors.streetName?.message)}</FormMessage>
                    </FormItem>
                  )}
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <FormField
                control={form.control}
                name="city"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('city')} <span className="text-destructive">*</span></FormLabel>
                    <FormControl>
                      <Input {...field} className="bg-background" />
                    </FormControl>
                    <FormMessage>{form.formState.errors.city?.message && t(form.formState.errors.city?.message)}</FormMessage>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="province"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('province')} <span className="text-destructive">*</span></FormLabel>
                    <FormControl>
                      <Input {...field} className="bg-background" />
                    </FormControl>
                    <FormMessage>{form.formState.errors.province?.message && t(form.formState.errors.province?.message)}</FormMessage>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="postalCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('postalCode')} <span className="text-destructive">*</span></FormLabel>
                    <FormControl>
                      <Input {...field} className="bg-background" />
                    </FormControl>
                    <FormMessage>{form.formState.errors.postalCode?.message && t(form.formState.errors.postalCode?.message)}</FormMessage>
                  </FormItem>
                )}
              />
            </div>
          </div>

          {/* Section 3: Initial Stage */}
          <div className={`space-y-6 ${isModal ? '' : 'bg-card p-6 rounded-xl border border-border shadow-sm'}`}>
            <h2 className="text-lg font-semibold text-foreground border-b border-border pb-2">
              {t('initialStage')}
            </h2>
            <FormField
              control={form.control}
              name="initialStage"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('selectInitialStage')} <span className="text-destructive">*</span></FormLabel>
                  <Select
                    onValueChange={field.onChange}
                    defaultValue={field.value}
                    disabled={!transactionSide}
                    value={field.value}
                  >
                    <FormControl>
                      <SelectTrigger className="w-full md:w-1/2 bg-background">
                        <SelectValue placeholder={t('selectInitialStage')} />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {stageEnums.map(stage => (
                        <SelectItem key={stage} value={stage}>{getStageLabel(stage, tTx, transactionSide === 'buy' ? 'BUY_SIDE' : 'SELL_SIDE')}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {!transactionSide && (
                    <p className="text-muted-foreground text-sm mt-2">{t('errorSelectSide')}</p>
                  )}
                  <FormMessage>{form.formState.errors.initialStage?.message && t(form.formState.errors.initialStage?.message)}</FormMessage>
                </FormItem>
              )}
            />
          </div>

          <div className="flex flex-col-reverse sm:flex-row items-center justify-end gap-3 mt-8 pt-4 border-t border-border">
            <Button
              type="button"
              variant="outline"
              onClick={() => onNavigate('/transactions')} // Changed to use onNavigate explicitly as per props
              className="w-full sm:w-auto"
            >
              {t('cancel')}
            </Button>

            <Button
              type="submit"
              disabled={!form.formState.isValid || form.formState.isSubmitting}
              className="w-full sm:w-auto min-w-[150px]"
            >
              {t('createTransaction')}
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
