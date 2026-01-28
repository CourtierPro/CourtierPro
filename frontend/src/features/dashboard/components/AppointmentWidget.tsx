
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/components/ui/card";
import { Calendar, Clock, Plus } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { Skeleton } from "@/shared/components/ui/skeleton";
import { useAppointments } from "@/features/appointments/api/queries";
import { format, parseISO, isToday } from "date-fns";
import { CreateAppointmentModal } from "@/features/appointments/components/CreateAppointmentModal";

export function AppointmentWidget() {
  const { t } = useTranslation("appointments");
  const navigate = useNavigate();
  const [modalOpen, setModalOpen] = useState(false);
  const { data: appointments, isLoading, error } = useAppointments();

  // Split appointments
  const incomingRequests = (appointments ?? []).filter(a => a.status === "PROPOSED");
  const confirmedAppointments = (appointments ?? []).filter(a => a.status === "CONFIRMED");

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t("appointments.title")}</CardTitle>
        </CardHeader>
        <CardContent>
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-16 w-full mb-2" />
          ))}
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t("appointments.title")}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-destructive text-sm">{t("appointments.error")}</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle className="text-lg font-semibold">
            {t("title")}
          </CardTitle>
          <CardDescription className="text-sm text-muted-foreground mt-1">
            {t("subtitle")}
          </CardDescription>
        </div>
        <Calendar className="w-5 h-5 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        {/* Incoming Requests Section */}
        <div className="mb-4">
          <div className="font-semibold mb-2">{t("incomingRequests", "Incoming Appointment Requests")}</div>
          {incomingRequests.length === 0 ? (
            <div className="text-sm text-muted-foreground mb-2">{t("noIncomingRequests", "No incoming requests.")}</div>
          ) : (
            <div className="space-y-2 mb-2">
              {incomingRequests.map((apt) => (
                <button
                  key={apt.appointmentId}
                  onClick={() => navigate(`/appointments?id=${apt.appointmentId}`)}
                  className="w-full flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors text-left focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <div className="text-xl flex-shrink-0">📅</div>
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-sm">{t(apt.title.toLowerCase(), apt.title)}</div>
                      <div className="text-xs text-muted-foreground truncate">{apt.clientName}</div>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-1 ml-2 flex-shrink-0">
                    <Badge variant="secondary" className="text-xs">
                      {format(parseISO(apt.fromDateTime), "EEE, MMM d")}
                    </Badge>
                    <span className="text-xs text-muted-foreground flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {format(parseISO(apt.fromDateTime), "HH:mm")} - {format(parseISO(apt.toDateTime), "HH:mm")}
                    </span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Confirmed Appointments Section */}
        <div className="mb-4">
          <div className="font-semibold mb-2">{t("upcomingConfirmed", "Upcoming Confirmed Appointments")}</div>
          {confirmedAppointments.length === 0 ? (
            <div className="text-sm text-muted-foreground mb-2">{t("noConfirmed", "No confirmed appointments.")}</div>
          ) : (
            <div className="space-y-2 mb-2">
              {confirmedAppointments.map((apt) => (
                <button
                  key={apt.appointmentId}
                  onClick={() => navigate(`/appointments?id=${apt.appointmentId}`)}
                  className="w-full flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors text-left focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <div className="text-xl flex-shrink-0">📅</div>
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-sm">{apt.title}</div>
                      <div className="text-xs text-muted-foreground truncate">{apt.clientName}</div>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-1 ml-2 flex-shrink-0">
                    <Badge variant={isToday(parseISO(apt.fromDateTime)) ? "warning" : "secondary"} className="text-xs">
                      {format(parseISO(apt.fromDateTime), "EEE, MMM d")}
                    </Badge>
                    <span className="text-xs text-muted-foreground flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {format(parseISO(apt.fromDateTime), "HH:mm")} - {format(parseISO(apt.toDateTime), "HH:mm")}
                    </span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Request Appointment Button (modal trigger) */}
        <div className="flex justify-end">
          <Button
            variant="default"
            size="sm"
            className="rounded-md px-4 py-2"
            onClick={() => setModalOpen(true)}
            onMouseDown={e => e.preventDefault()}
          >
            <Plus className="mr-2 h-4 w-4" />
            {t("appointments.requestAppointment", "Request Appointment")}
          </Button>
        </div>
        <CreateAppointmentModal isOpen={modalOpen} onClose={() => setModalOpen(false)} onSubmit={() => setModalOpen(false)} />
      </CardContent>
    </Card>
  );
}
