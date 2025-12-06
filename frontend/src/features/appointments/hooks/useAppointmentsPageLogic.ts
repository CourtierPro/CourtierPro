import { useState } from 'react';
import { toast } from 'sonner';
import { useAppointments } from '../api/queries';
import { useCreateAppointment, type CreateAppointmentDTO } from '../api/mutations';
import { type AppointmentFormData } from '../components/CreateAppointmentModal';

export function useAppointmentsPageLogic() {
    const { data: appointments = [], isLoading, error, refetch } = useAppointments();
    const createAppointment = useCreateAppointment();
    const [isModalOpen, setIsModalOpen] = useState(false);

    const handleCreateAppointment = async (data: AppointmentFormData) => {
        try {
            await createAppointment.mutateAsync({
                ...data,
            } as CreateAppointmentDTO);
            setIsModalOpen(false);
            toast.success("Appointment created successfully");
        } catch (err) {
            toast.error("Failed to create appointment");
        }
    };

    return {
        appointments,
        isLoading,
        error,
        refetch,
        isModalOpen,
        setIsModalOpen,
        handleCreateAppointment,
    };
}
