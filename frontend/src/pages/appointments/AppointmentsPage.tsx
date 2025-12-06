import { useState } from "react";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Calendar, Plus } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { useAppointments } from "@/features/appointments/api/queries";
import { useCreateAppointment, type CreateAppointmentDTO } from "@/features/appointments/api/mutations";
import { CreateAppointmentModal, type AppointmentFormData } from "@/features/appointments/components/CreateAppointmentModal";
import { AppointmentList } from "@/features/appointments/components/AppointmentList";
import { useTranslation } from "react-i18next";

export function AppointmentsPage() {
  const { t } = useTranslation('appointments');
  const { data: appointments = [], isLoading, error, refetch } = useAppointments();
  const createAppointment = useCreateAppointment();
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleCreateAppointment = async (data: AppointmentFormData) => {
    try {
      await createAppointment.mutateAsync({
        ...data,
      } as CreateAppointmentDTO);
      setIsModalOpen(false);
    } catch (err) {
      console.error("Failed to create appointment", err);
    }
  };

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
            title={t('noAppointmentsTitle', 'No appointments scheduled')}
            description={t('noAppointmentsDesc', "You don't have any upcoming appointments. Schedule one now.")}
            action={
              <Button onClick={() => setIsModalOpen(true)}>{t('scheduleAppointment', 'Schedule Appointment')}</Button>
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
