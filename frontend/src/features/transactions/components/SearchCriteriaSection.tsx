import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'sonner';
import { Trash2, Save, ChevronDown, ChevronUp } from 'lucide-react';
import { Section } from '@/shared/components/branded/Section';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Label } from '@/shared/components/ui/label';
import { Checkbox } from '@/shared/components/ui/checkbox';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/shared/components/ui/select';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/shared/components/ui/alert-dialog';
import { useSearchCriteria } from '@/features/transactions/api/queries';
import { useUpdateSearchCriteria, useDeleteSearchCriteria } from '@/features/transactions/api/mutations';
import { useParticipantPermissions } from '@/features/transactions/hooks/useParticipantPermissions';
import type {
    SearchCriteriaRequestDTO,
    SearchCriteriaPropertyType,
    SearchCriteriaBuildingStyle,
    SearchCriteriaPlexType,
    QuebecRegion,
} from '@/shared/api/types';

interface SearchCriteriaSectionProps {
    transactionId: string;
    isReadOnly?: boolean;
}

const PROPERTY_TYPES: SearchCriteriaPropertyType[] = [
    'SINGLE_FAMILY_HOME', 'CONDO', 'LOFT_STUDIO', 'PLEX', 'INTERGENERATIONAL',
    'MOBILE_HOME', 'HOBBY_FARM', 'COTTAGE', 'LOT'
];

const BUILDING_STYLES: SearchCriteriaBuildingStyle[] = [
    'NEW_CONSTRUCTION', 'CENTURY_HISTORIC', 'BUNGALOW', 'MORE_THAN_ONE_STOREY',
    'SPLIT_LEVEL', 'DETACHED', 'SEMI_DETACHED', 'ATTACHED'
];

const PLEX_TYPES: SearchCriteriaPlexType[] = ['DUPLEX', 'TRIPLEX', 'QUADRUPLEX', 'QUINTUPLEX'];

const QUEBEC_REGIONS: QuebecRegion[] = [
    'BAS_SAINT_LAURENT', 'SAGUENAY_LAC_SAINT_JEAN', 'CAPITALE_NATIONALE', 'MAURICIE',
    'ESTRIE', 'MONTREAL', 'OUTAOUAIS', 'ABITIBI_TEMISCAMINGUE', 'COTE_NORD',
    'NORD_DU_QUEBEC', 'GASPESIE_ILES_DE_LA_MADELEINE', 'CHAUDIERE_APPALACHES',
    'LAVAL', 'LANAUDIERE', 'LAURENTIDES', 'MONTEREGIE', 'CENTRE_DU_QUEBEC'
];

type FormValues = SearchCriteriaRequestDTO;

interface CollapsibleSectionProps {
    title: string;
    children: React.ReactNode;
    defaultOpen?: boolean;
}

function CollapsibleSection({ title, children, defaultOpen = true }: CollapsibleSectionProps) {
    const [isOpen, setIsOpen] = useState(defaultOpen);

    return (
        <div className="border border-border rounded-lg">
            <button
                type="button"
                onClick={() => setIsOpen(!isOpen)}
                className="w-full flex items-center justify-between p-3 hover:bg-muted/50 transition-colors"
            >
                <h3 className="text-sm font-medium">{title}</h3>
                {isOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </button>
            {isOpen && <div className="p-3 pt-0 space-y-4">{children}</div>}
        </div>
    );
}

interface CheckboxGroupProps<T extends string> {
    options: T[];
    value: T[] | undefined;
    onChange: (value: T[]) => void;
    labelKey: string;
    t: (key: string) => string;
    disabled?: boolean;
}

function CheckboxGroup<T extends string>({ options, value, onChange, labelKey, t, disabled }: CheckboxGroupProps<T>) {
    const selected = new Set(value || []);

    const toggle = (option: T) => {
        const newSet = new Set(selected);
        if (newSet.has(option)) {
            newSet.delete(option);
        } else {
            newSet.add(option);
        }
        onChange(Array.from(newSet));
    };

    return (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
            {options.map(option => (
                <label
                    key={option}
                    className="flex items-center gap-2 text-sm cursor-pointer"
                >
                    <Checkbox
                        checked={selected.has(option)}
                        onCheckedChange={() => toggle(option)}
                        disabled={disabled}
                    />
                    <span>{t(`${labelKey}.${option}`)}</span>
                </label>
            ))}
        </div>
    );
}

export function SearchCriteriaSection({ transactionId }: SearchCriteriaSectionProps) {
    const { t } = useTranslation('transactions');
    const { data: criteria, isLoading, error, refetch } = useSearchCriteria(transactionId);
    const updateCriteria = useUpdateSearchCriteria();
    const deleteCriteria = useDeleteSearchCriteria();
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const { checkPermission } = useParticipantPermissions(transactionId);

    // Use permission-based check: allow editing if user has EDIT_SEARCH_CRITERIA permission
    // Only use isReadOnlyProp as fallback when canEdit is false (e.g., archived transactions)
    const canEdit = checkPermission('EDIT_SEARCH_CRITERIA');
    // If user has edit permission, allow editing regardless of isReadOnlyProp
    // This ensures clients can edit their search criteria even when the page uses isReadOnly mode for other sections
    const isReadOnly = !canEdit;

    const form = useForm<FormValues>({
        defaultValues: {},
    });

    // Populate form when data loads
    useEffect(() => {
        if (criteria) {
            form.reset({
                propertyTypes: criteria.propertyTypes,
                minBedrooms: criteria.minBedrooms,
                minBathrooms: criteria.minBathrooms,
                minParkingSpaces: criteria.minParkingSpaces,
                minGarages: criteria.minGarages,
                hasPool: criteria.hasPool,
                hasElevator: criteria.hasElevator,
                adaptedForReducedMobility: criteria.adaptedForReducedMobility,
                hasWaterfront: criteria.hasWaterfront,
                hasAccessToWaterfront: criteria.hasAccessToWaterfront,
                hasNavigableWater: criteria.hasNavigableWater,
                isResort: criteria.isResort,
                petsAllowed: criteria.petsAllowed,
                smokingAllowed: criteria.smokingAllowed,
                minLivingArea: criteria.minLivingArea,
                maxLivingArea: criteria.maxLivingArea,
                livingAreaUnit: criteria.livingAreaUnit,
                minYearBuilt: criteria.minYearBuilt,
                maxYearBuilt: criteria.maxYearBuilt,
                buildingStyles: criteria.buildingStyles,
                plexTypes: criteria.plexTypes,
                minLandArea: criteria.minLandArea,
                maxLandArea: criteria.maxLandArea,
                landAreaUnit: criteria.landAreaUnit,
                newSince: criteria.newSince,
                moveInDate: criteria.moveInDate,
                openHousesOnly: criteria.openHousesOnly,
                repossessionOnly: criteria.repossessionOnly,
                minPrice: criteria.minPrice,
                maxPrice: criteria.maxPrice,
                regions: criteria.regions,
            });
        }
    }, [criteria, form]);

    const onSubmit = async (data: FormValues) => {
        try {
            await updateCriteria.mutateAsync({ transactionId, data });
            toast.success(t('searchCriteria.saved'));
        } catch {
            toast.error(t('searchCriteria.saveError'));
        }
    };

    const handleDelete = async () => {
        try {
            await deleteCriteria.mutateAsync(transactionId);
            form.reset({});
            toast.success(t('searchCriteria.deleted'));
            setShowDeleteDialog(false);
        } catch {
            toast.error(t('searchCriteria.deleteError'));
        }
    };

    if (isLoading) {
        return <LoadingState message={t('searchCriteria.loading')} />;
    }

    if (error) {
        return <ErrorState message={t('searchCriteria.loadError')} onRetry={() => refetch()} />;
    }

    const isPending = updateCriteria.isPending || deleteCriteria.isPending;

    return (
        <Section className="p-4 md:p-6">
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                {/* Price Range */}
                <CollapsibleSection title={t('searchCriteria.priceRange')}>
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.minPrice')}</Label>
                            <Input
                                type="number"
                                {...form.register('minPrice', { valueAsNumber: true })}
                                placeholder="300000"
                                disabled={isReadOnly}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.maxPrice')}</Label>
                            <Input
                                type="number"
                                {...form.register('maxPrice', { valueAsNumber: true })}
                                placeholder="500000"
                                disabled={isReadOnly}
                            />
                        </div>
                    </div>
                </CollapsibleSection>

                {/* Regions */}
                <CollapsibleSection title={t('searchCriteria.regions')}>
                    <Controller
                        name="regions"
                        control={form.control}
                        render={({ field }) => (
                            <CheckboxGroup
                                options={QUEBEC_REGIONS}
                                value={field.value}
                                onChange={field.onChange}
                                labelKey="searchCriteria.regionLabels"
                                t={t}
                                disabled={isReadOnly}
                            />
                        )}
                    />
                </CollapsibleSection>

                {/* Property Types */}
                <CollapsibleSection title={t('searchCriteria.propertyTypes')}>
                    <Controller
                        name="propertyTypes"
                        control={form.control}
                        render={({ field }) => (
                            <CheckboxGroup
                                options={PROPERTY_TYPES}
                                value={field.value}
                                onChange={field.onChange}
                                labelKey="searchCriteria.propertyTypeLabels"
                                t={t}
                                disabled={isReadOnly}
                            />
                        )}
                    />
                </CollapsibleSection>

                {/* Features */}
                <CollapsibleSection title={t('searchCriteria.features')}>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.bedrooms')}</Label>
                            <Controller
                                name="minBedrooms"
                                control={form.control}
                                render={({ field }) => (
                                    <Select
                                        onValueChange={(val) => field.onChange(val ? Number(val) : undefined)}
                                        value={field.value?.toString() || ''}
                                        disabled={isReadOnly}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder={t('searchCriteria.any')} />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="1">1+</SelectItem>
                                            <SelectItem value="2">2+</SelectItem>
                                            <SelectItem value="3">3+</SelectItem>
                                            <SelectItem value="4">4+</SelectItem>
                                        </SelectContent>
                                    </Select>
                                )}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.bathrooms')}</Label>
                            <Controller
                                name="minBathrooms"
                                control={form.control}
                                render={({ field }) => (
                                    <Select
                                        onValueChange={(val) => field.onChange(val ? Number(val) : undefined)}
                                        value={field.value?.toString() || ''}
                                        disabled={isReadOnly}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder={t('searchCriteria.any')} />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="1">1+</SelectItem>
                                            <SelectItem value="2">2+</SelectItem>
                                            <SelectItem value="3">3+</SelectItem>
                                            <SelectItem value="4">4+</SelectItem>
                                        </SelectContent>
                                    </Select>
                                )}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.parking')}</Label>
                            <Controller
                                name="minParkingSpaces"
                                control={form.control}
                                render={({ field }) => (
                                    <Select
                                        onValueChange={(val) => field.onChange(val ? Number(val) : undefined)}
                                        value={field.value?.toString() || ''}
                                        disabled={isReadOnly}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder={t('searchCriteria.any')} />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="1">1+</SelectItem>
                                            <SelectItem value="2">2+</SelectItem>
                                            <SelectItem value="3">3+</SelectItem>
                                            <SelectItem value="4">4+</SelectItem>
                                        </SelectContent>
                                    </Select>
                                )}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.garages')}</Label>
                            <Controller
                                name="minGarages"
                                control={form.control}
                                render={({ field }) => (
                                    <Select
                                        onValueChange={(val) => field.onChange(val ? Number(val) : undefined)}
                                        value={field.value?.toString() || ''}
                                        disabled={isReadOnly}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder={t('searchCriteria.any')} />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="1">1+</SelectItem>
                                            <SelectItem value="2">2+</SelectItem>
                                            <SelectItem value="3">3+</SelectItem>
                                            <SelectItem value="4">4+</SelectItem>
                                        </SelectContent>
                                    </Select>
                                )}
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-2 md:grid-cols-3 gap-2 mt-4">
                        {(['hasPool', 'hasElevator', 'adaptedForReducedMobility', 'hasWaterfront', 'hasAccessToWaterfront', 'hasNavigableWater', 'isResort', 'petsAllowed', 'smokingAllowed'] as const).map(field => (
                            <Controller
                                key={field}
                                name={field}
                                control={form.control}
                                render={({ field: { value, onChange } }) => (
                                    <label className="flex items-center gap-2 text-sm cursor-pointer">
                                        <Checkbox
                                            checked={value === true}
                                            onCheckedChange={onChange}
                                            disabled={isReadOnly}
                                        />
                                        <span>{t(`searchCriteria.${field}`)}</span>
                                    </label>
                                )}
                            />
                        ))}
                    </div>
                </CollapsibleSection>

                {/* Building */}
                <CollapsibleSection title={t('searchCriteria.building')}>
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.minLivingArea')}</Label>
                            <Input
                                type="number"
                                {...form.register('minLivingArea', { valueAsNumber: true })}
                                disabled={isReadOnly}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.maxLivingArea')}</Label>
                            <Input
                                type="number"
                                {...form.register('maxLivingArea', { valueAsNumber: true })}
                                disabled={isReadOnly}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.areaUnit')}</Label>
                            <Controller
                                name="livingAreaUnit"
                                control={form.control}
                                render={({ field }) => (
                                    <Select onValueChange={field.onChange} value={field.value || ''} disabled={isReadOnly}>
                                        <SelectTrigger>
                                            <SelectValue placeholder={t('searchCriteria.selectUnit')} />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="SQFT">{t('searchCriteria.sqft')}</SelectItem>
                                            <SelectItem value="SQM">{t('searchCriteria.sqm')}</SelectItem>
                                        </SelectContent>
                                    </Select>
                                )}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.minYearBuilt')}</Label>
                            <Input
                                type="number"
                                {...form.register('minYearBuilt', { valueAsNumber: true })}
                                placeholder="1990"
                                disabled={isReadOnly}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.maxYearBuilt')}</Label>
                            <Input
                                type="number"
                                {...form.register('maxYearBuilt', { valueAsNumber: true })}
                                placeholder="2024"
                                disabled={isReadOnly}
                            />
                        </div>
                    </div>

                    <div className="mt-4">
                        <Label className="mb-2 block">{t('searchCriteria.buildingStyles')}</Label>
                        <Controller
                            name="buildingStyles"
                            control={form.control}
                            render={({ field }) => (
                                <CheckboxGroup
                                    options={BUILDING_STYLES}
                                    value={field.value}
                                    onChange={field.onChange}
                                    labelKey="searchCriteria.buildingStyleLabels"
                                    t={t}
                                    disabled={isReadOnly}
                                />
                            )}
                        />
                    </div>
                </CollapsibleSection>

                {/* Plex Types */}
                <CollapsibleSection title={t('searchCriteria.plexTypes')} defaultOpen={false}>
                    <Controller
                        name="plexTypes"
                        control={form.control}
                        render={({ field }) => (
                            <CheckboxGroup
                                options={PLEX_TYPES}
                                value={field.value}
                                onChange={field.onChange}
                                labelKey="searchCriteria.plexTypeLabels"
                                t={t}
                                disabled={isReadOnly}
                            />
                        )}
                    />
                </CollapsibleSection>

                {/* Other Criteria */}
                <CollapsibleSection title={t('searchCriteria.otherCriteria')} defaultOpen={false}>
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.minLandArea')}</Label>
                            <Input
                                type="number"
                                {...form.register('minLandArea', { valueAsNumber: true })}
                                disabled={isReadOnly}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.maxLandArea')}</Label>
                            <Input
                                type="number"
                                {...form.register('maxLandArea', { valueAsNumber: true })}
                                disabled={isReadOnly}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.landAreaUnit')}</Label>
                            <Controller
                                name="landAreaUnit"
                                control={form.control}
                                render={({ field }) => (
                                    <Select onValueChange={field.onChange} value={field.value || ''} disabled={isReadOnly}>
                                        <SelectTrigger>
                                            <SelectValue placeholder={t('searchCriteria.selectUnit')} />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="SQFT">{t('searchCriteria.sqft')}</SelectItem>
                                            <SelectItem value="SQM">{t('searchCriteria.sqm')}</SelectItem>
                                        </SelectContent>
                                    </Select>
                                )}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.newSince')}</Label>
                            <Input
                                type="date"
                                {...form.register('newSince')}
                                disabled={isReadOnly}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label>{t('searchCriteria.moveInDate')}</Label>
                            <Input
                                type="date"
                                {...form.register('moveInDate')}
                                disabled={isReadOnly}
                            />
                        </div>
                    </div>

                    <div className="flex gap-4 mt-4">
                        <Controller
                            name="openHousesOnly"
                            control={form.control}
                            render={({ field: { value, onChange } }) => (
                                <label className="flex items-center gap-2 text-sm cursor-pointer">
                                    <Checkbox
                                        checked={value === true}
                                        onCheckedChange={onChange}
                                        disabled={isReadOnly}
                                    />
                                    <span>{t('searchCriteria.openHousesOnly')}</span>
                                </label>
                            )}
                        />
                        <Controller
                            name="repossessionOnly"
                            control={form.control}
                            render={({ field: { value, onChange } }) => (
                                <label className="flex items-center gap-2 text-sm cursor-pointer">
                                    <Checkbox
                                        checked={value === true}
                                        onCheckedChange={onChange}
                                        disabled={isReadOnly}
                                    />
                                    <span>{t('searchCriteria.repossessionOnly')}</span>
                                </label>
                            )}
                        />
                    </div>
                </CollapsibleSection>

                {/* Actions */}
                {!isReadOnly && (
                    <div className="flex justify-between pt-4 border-t border-border">
                        <Button
                            type="button"
                            variant="destructive"
                            onClick={() => setShowDeleteDialog(true)}
                            disabled={isPending || !criteria}
                            className="gap-2"
                        >
                            <Trash2 className="h-4 w-4" />
                            {t('searchCriteria.delete')}
                        </Button>
                        <Button type="submit" disabled={isPending} className="gap-2">
                            <Save className="h-4 w-4" />
                            {isPending ? t('saving') : t('searchCriteria.save')}
                        </Button>
                    </div>
                )}
            </form>

            <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t('searchCriteria.deleteConfirmTitle')}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t('searchCriteria.deleteConfirmDescription')}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t('cancel')}</AlertDialogCancel>
                        <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground hover:bg-destructive/90">
                            {t('delete')}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </Section>
    );
}
