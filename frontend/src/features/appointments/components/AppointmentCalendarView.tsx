import { useMemo, useState } from 'react';
import { Calendar } from '@/shared/components/ui/calendar';
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Badge } from '@/shared/components/ui/badge';
import { Button } from '@/shared/components/ui/button';
import { ChevronLeft, ChevronRight, CalendarDays } from 'lucide-react';
import { type Appointment, getAppointmentTimeRange } from '../types';
import { getStatusBadgeVariant } from '../enums';
import { useTranslation } from 'react-i18next';
import { format } from 'date-fns';
import { fr, enUS } from 'date-fns/locale';

interface AppointmentCalendarViewProps {
    appointments: Appointment[];
    currentDate: Date;
    onPreviousMonth: () => void;
    onNextMonth: () => void;
    onToday: () => void;
    isLoading?: boolean;
}

export function AppointmentCalendarView({
    appointments,
    currentDate,
    onPreviousMonth,
    onNextMonth,
    onToday,
    isLoading = false,
}: AppointmentCalendarViewProps) {
    const { t, i18n } = useTranslation('appointments');
    const [selectedDate, setSelectedDate] = useState<Date | undefined>(undefined);

    // Get the appropriate locale for date formatting
    const dateLocale = useMemo(() => {
        return i18n.language?.startsWith('fr') ? fr : enUS;
    }, [i18n.language]);

    // Create a map of dates to appointments for quick lookup
    const appointmentsByDate = useMemo(() => {
        const map = new Map<string, Appointment[]>();
        for (const apt of appointments) {
            const date = new Date(apt.fromDateTime);
            const key = format(date, 'yyyy-MM-dd');
            if (!map.has(key)) {
                map.set(key, []);
            }
            map.get(key)!.push(apt);
        }
        return map;
    }, [appointments]);

    // Get dates that have appointments
    const datesWithAppointments = useMemo(() => {
        return Array.from(appointmentsByDate.keys()).map(dateStr => new Date(dateStr));
    }, [appointmentsByDate]);

    // Get appointments for selected date
    const selectedDateAppointments = useMemo(() => {
        if (!selectedDate) return [];
        const key = format(selectedDate, 'yyyy-MM-dd');
        return appointmentsByDate.get(key) || [];
    }, [selectedDate, appointmentsByDate]);

    const monthYear = format(currentDate, 'MMMM yyyy', { locale: dateLocale });

    return (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Calendar Section */}
            <Card className="lg:col-span-2 relative">
                {isLoading && (
                    <div className="absolute inset-0 bg-background/50 z-10 flex items-center justify-center backdrop-blur-[1px] rounded-lg">
                        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                    </div>
                )}
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
                    <CardTitle className="text-xl font-semibold capitalize">{monthYear}</CardTitle>
                    <div className="flex items-center gap-2">
                        <Button variant="outline" size="sm" onClick={() => {
                            onToday();
                            setSelectedDate(new Date());
                        }}>
                            {t('today', 'Today')}
                        </Button>
                        <Button variant="outline" size="icon" onClick={onPreviousMonth}>
                            <ChevronLeft className="h-4 w-4" />
                        </Button>
                        <Button variant="outline" size="icon" onClick={onNextMonth}>
                            <ChevronRight className="h-4 w-4" />
                        </Button>
                    </div>
                </CardHeader>
                <CardContent>
                    <Calendar
                        mode="single"
                        selected={selectedDate}
                        onSelect={setSelectedDate}
                        month={currentDate}
                        locale={dateLocale}
                        hideNavigation
                        modifiers={{
                            hasAppointment: datesWithAppointments,
                        }}
                        modifiersClassNames={{
                            hasAppointment: 'font-bold underline decoration-primary decoration-2 underline-offset-4',
                        }}
                        className="rounded-md w-full"
                        classNames={{
                            months: "w-full",
                            month: "w-full space-y-4",
                            caption: "hidden",
                            caption_label: "hidden",
                            nav: "hidden",
                            month_grid: "w-full border-collapse",
                            weekdays: "grid grid-cols-7 w-full",
                            weekday: "text-muted-foreground font-normal text-sm text-center py-2",
                            week: "grid grid-cols-7 w-full",
                            day: "h-12 w-full text-center text-sm p-0 relative flex items-center justify-center [&.selected]:text-black",
                            day_button: "h-9 w-9 font-normal rounded-md transition-colors bg-transparent text-inherit hover:bg-accent hover:text-accent-foreground",
                            outside: "text-muted-foreground opacity-50 [&>button]:opacity-50",
                            today: "bg-accent text-accent-foreground rounded-md [&>button]:bg-accent [&>button]:text-accent-foreground",
                            selected: "bg-primary text-black rounded-md [&>button]:bg-primary [&>button]:text-black [&>button]:hover:bg-primary [&>button]:hover:text-black",
                        }}
                    />
                </CardContent>
            </Card>

            {/* Selected Date Appointments */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <CalendarDays className="h-5 w-5" />
                        {selectedDate
                            ? format(selectedDate, 'EEEE, d MMMM', { locale: dateLocale })
                            : t('selectDate', 'Select a date')}
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    {!selectedDate ? (
                        <p className="text-sm text-muted-foreground">
                            {t('clickToSee', 'Click on a date to see appointments')}
                        </p>
                    ) : selectedDateAppointments.length === 0 ? (
                        <p className="text-sm text-muted-foreground">
                            {t('noAppointmentsDate', 'No appointments on this date')}
                        </p>
                    ) : (
                        <div className="space-y-3">
                            {selectedDateAppointments.map((apt) => (
                                <div
                                    key={apt.appointmentId}
                                    className="p-3 border rounded-lg space-y-2"
                                >
                                    <div className="flex items-start justify-between">
                                        <h4 className="font-medium text-sm">{apt.title}</h4>
                                        <Badge variant={getStatusBadgeVariant(apt.status)} className="text-xs">
                                            {t(`status.${apt.status.toLowerCase()}`, apt.status)}
                                        </Badge>
                                    </div>
                                    <p className="text-xs text-muted-foreground">
                                        {getAppointmentTimeRange(apt)}
                                    </p>
                                    <p className="text-xs">
                                        {apt.clientName}
                                    </p>
                                    {apt.location && (
                                        <p className="text-xs text-muted-foreground truncate">
                                            📍 {apt.location}
                                        </p>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
