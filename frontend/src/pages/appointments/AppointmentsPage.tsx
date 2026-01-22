import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Calendar, List, Plus } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/components/ui/tabs";
import { useAppointmentsPageLogic } from "@/features/appointments/hooks/useAppointmentsPageLogic";
import { AppointmentList } from "@/features/appointments/components/AppointmentList";
import { AppointmentCalendarView } from "@/features/appointments/components/AppointmentCalendarView";
import { useTranslation } from "react-i18next";
import { useState } from "react";
import { CreateAppointmentModal } from "@/features/appointments/components/CreateAppointmentModal";

export function AppointmentsPage() {
  const { t } = useTranslation('appointments');
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const {
    appointments,
    groupedAppointments,
    isLoading,
    error,
    refetch,
    viewMode,
    setViewMode,
    currentDate,
    goToPreviousMonth,
    goToNextMonth,
    goToToday,
    isFetching,
  } = useAppointmentsPageLogic();

  if (isLoading && !appointments.length) return <LoadingState />;
  if (error) return <ErrorState message={error.message} onRetry={() => refetch()} />;

  const hasAppointments = appointments.length > 0;

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title', 'Appointments')}
        subtitle={t('subtitle', 'View and manage your upcoming appointments.')}
        actions={
          <Button onClick={() => setIsCreateModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" />
            {t('scheduleAppointment', 'Schedule Appointment')}
          </Button>
        }
      />

      <Tabs
        value={viewMode}
        onValueChange={(value) => setViewMode(value as 'calendar' | 'list')}
        className="w-full"
      >
        <div className="flex items-center justify-between mb-4">
          <TabsList>
            <TabsTrigger value="calendar" className="gap-2">
              <Calendar className="h-4 w-4" />
              {t('calendarView', 'Calendar')}
            </TabsTrigger>
            <TabsTrigger value="list" className="gap-2">
              <List className="h-4 w-4" />
              {t('listView', 'List')}
            </TabsTrigger>
          </TabsList>
        </div>

        <TabsContent value="calendar">
          <AppointmentCalendarView
            appointments={appointments}
            currentDate={currentDate}
            onPreviousMonth={goToPreviousMonth}
            onNextMonth={goToNextMonth}
            onToday={goToToday}
            isLoading={isFetching}
          />
          {!hasAppointments && (
            <div className="mt-6 text-center text-muted-foreground">
              <p className="text-sm">{t('noAppointmentsDesc', "You don't have any upcoming appointments. Schedule one now.")}</p>
            </div>
          )}
        </TabsContent>

        <TabsContent value="list">
          {!hasAppointments ? (
            <Section>
              <EmptyState
                icon={<List />}
                title={t('noAppointmentsTitle', 'No appointments scheduled')}
                description={t('noAppointmentsDesc', "You don't have any upcoming appointments. Schedule one now.")}
                action={
                  <Button onClick={() => setIsCreateModalOpen(true)}>
                    {t('scheduleAppointment', 'Schedule Appointment')}
                  </Button>
                }
              />
            </Section>
          ) : (
            <AppointmentList groupedAppointments={groupedAppointments} />
          )}
        </TabsContent>
      </Tabs>

      <CreateAppointmentModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSubmit={() => {
          refetch();
          setIsCreateModalOpen(false);
        }}
        existingAppointments={appointments}
      />
    </div>
  );
}
