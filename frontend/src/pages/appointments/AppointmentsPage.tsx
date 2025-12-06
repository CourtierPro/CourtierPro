
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Calendar, Plus } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { useAppointmentsPageLogic } from "@/features/appointments/hooks/useAppointmentsPageLogic";
import { CreateAppointmentModal } from "@/features/appointments/components/CreateAppointmentModal";
import { AppointmentList } from "@/features/appointments/components/AppointmentList";
import { useTranslation } from "react-i18next";

export function AppointmentsPage() {
  const { t } = useTranslation('appointments');
  const {
    appointments,
    isLoading,
    error,
    refetch,
    isModalOpen,
    setIsModalOpen,
    handleCreateAppointment
  } = useAppointmentsPageLogic();

  if (isLoading) return <LoadingState />;
  if (error) return <ErrorState message={error.message} onRetry={() => refetch()} />;

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title', 'Appointments')}
        subtitle={t('subtitle', 'View and manage your upcoming appointments.')}
        actions={
          appointments.length > 0 && (
            <Button onClick={() => setIsModalOpen(true)}>
              <Plus className="w-4 h-4 mr-2" />
              {t('scheduleAppointment', 'Schedule Appointment')}
            </Button>
          )
        }
      />

      {appointments.length === 0 ? (
        <Section>
          <EmptyState
            icon={<Calendar />}
            title={t('noAppointmentsTitle')}
            description={t('noAppointmentsDesc')}
            action={
              <Button onClick={() => setIsModalOpen(true)}>{t('scheduleAppointment')}</Button>
            }
          />
        </Section>
      ) : (
        <AppointmentList appointments={appointments} />
      )}

      <CreateAppointmentModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleCreateAppointment}
      />
    </div>
  );
}
