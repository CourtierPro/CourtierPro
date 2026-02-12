import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/components/ui/card";
import { Calendar, Clock } from "lucide-react";
import { Badge } from "@/shared/components/ui/badge";
import { Skeleton } from "@/shared/components/ui/skeleton";
import { useAppointments } from "@/features/appointments/api/queries";
// Removed unused UUID import
import type { Appointment } from "@/features/appointments/types";
import { format, parseISO, isToday } from "date-fns";
// Removed unused imports

export function AppointmentWidget() {
  const { t, i18n } = useTranslation("appointments");
  const navigate = useNavigate();
  // Removed unused modal state
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
              {incomingRequests.map((apt: Appointment) => (
                <button
                  key={apt.appointmentId}
                  onClick={() => navigate(`/appointments?id=${encodeURIComponent(apt.appointmentId as string)}`)}
                  className="w-full flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors text-left focus:outline-none focus:ring-2 focus:ring-primary"
                  aria-label={`Open appointment ${apt.title} on ${format(parseISO(apt.fromDateTime), "EEEE, MMMM d, yyyy")} from ${format(parseISO(apt.fromDateTime), "HH:mm")} to ${format(parseISO(apt.toDateTime), "HH:mm")}`}
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <div className="text-xl flex-shrink-0">📅</div>
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-sm">{String(t(apt.title.toLowerCase(), apt.title))}</div>
                      <div className="text-xs text-muted-foreground truncate">{apt.clientName}</div>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-1 ml-2 flex-shrink-0">
                    <Badge variant="secondary" className="text-xs">
                      {parseISO(apt.fromDateTime).toLocaleDateString(i18n.language, { weekday: 'short', month: 'short', day: 'numeric' })}
                    </Badge>
                    <span className="text-xs text-muted-foreground flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {parseISO(apt.fromDateTime).toLocaleTimeString(i18n.language, { hour: '2-digit', minute: '2-digit' })} - {parseISO(apt.toDateTime).toLocaleTimeString(i18n.language, { hour: '2-digit', minute: '2-digit' })}
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
              {confirmedAppointments.map((apt: Appointment) => (
                <button
                  key={apt.appointmentId}
                  onClick={() => navigate(`/appointments?id=${apt.appointmentId as string}`)}
                  className="w-full flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors text-left focus:outline-none focus:ring-2 focus:ring-primary"
                  aria-label={`Open appointment ${apt.title} on ${format(parseISO(apt.fromDateTime), "EEEE, MMMM d, yyyy")} from ${format(parseISO(apt.fromDateTime), "HH:mm")} to ${format(parseISO(apt.toDateTime), "HH:mm")}`}
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <div className="text-xl flex-shrink-0">📅</div>
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-sm">{String(t(apt.title.toLowerCase(), apt.title))}</div>
                      <div className="text-xs text-muted-foreground truncate">{apt.clientName}</div>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-1 ml-2 flex-shrink-0">
                    <Badge variant={(() => { try { return isToday(parseISO(apt.fromDateTime)) ? "warning" : "secondary"; } catch { return "secondary"; } })()} className="text-xs">
                      {(() => { try { return parseISO(apt.fromDateTime).toLocaleDateString(i18n.language, { weekday: 'short', month: 'short', day: 'numeric' }); } catch { return apt.fromDateTime; } })()}
                    </Badge>
                    <span className="text-xs text-muted-foreground flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {(() => { try { return parseISO(apt.fromDateTime).toLocaleTimeString(i18n.language, { hour: '2-digit', minute: '2-digit' }); } catch { return "--:--"; } })()} - {(() => { try { return parseISO(apt.toDateTime).toLocaleTimeString(i18n.language, { hour: '2-digit', minute: '2-digit' }); } catch { return "--:--"; } })()}
                    </span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Removed Request Appointment Button, modal will be triggered from QuickLinksGrid */}
      </CardContent>
    </Card>
  );
}
