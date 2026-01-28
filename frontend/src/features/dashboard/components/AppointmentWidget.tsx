import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/components/ui/card";
import { Calendar, ArrowRight, Clock } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { Skeleton } from "@/shared/components/ui/skeleton";
import { useQuery } from "@tanstack/react-query";
import axiosInstance from "@/shared/api/axiosInstance";
import { format, parseISO, isToday } from "date-fns";

interface Appointment {
  appointmentId: string;
  title: string;
  fromDateTime: string;
  toDateTime: string;
  location: string | null;
  clientName: string;
}

export function AppointmentWidget() {
  const { t } = useTranslation("dashboard");
  const navigate = useNavigate();
  const { data, isLoading, error } = useQuery({
    queryKey: ["top-appointments"],
    queryFn: async () => {
      const res = await axiosInstance.get<Appointment[]>("/appointments/top-upcoming");
      return res.data;
    },
  });

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t("appointments.title", "Upcoming Appointments")}</CardTitle>
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
          <CardTitle>{t("appointments.title", "Upcoming Appointments")}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-destructive text-sm">{t("appointments.error", "Failed to load appointments.")}</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle className="text-lg font-semibold">
            {t("appointments.title", "Upcoming Appointments")}
          </CardTitle>
          <CardDescription className="text-sm text-muted-foreground mt-1">
            {t("appointments.description", "Your next 3 appointments.")}
          </CardDescription>
        </div>
        <Calendar className="w-5 h-5 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        {data && data.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <Calendar className="h-10 w-10 text-muted-foreground/50 mb-2" />
            <p className="text-sm text-muted-foreground">
              {t("appointments.empty", "No upcoming appointments.")}
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {data?.map((apt) => (
              <button
                key={apt.appointmentId}
                onClick={() => navigate(`/appointments?id=${apt.appointmentId}`)}
                className="w-full flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors text-left focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <div className="flex items-center gap-3 flex-1 min-w-0">
                  <div className="text-xl flex-shrink-0">📅</div>
                  <div className="flex-1 min-w-0">
                    <div className="font-medium text-sm">{apt.title}</div>
                    <div className="text-xs text-muted-foreground truncate">
                      {apt.location}
                    </div>
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
            <Button
              variant="ghost"
              size="sm"
              className="w-full mt-2"
              onClick={() => navigate("/appointments")}
            >
              {t("appointments.seeAll", "See all appointments")}
              <ArrowRight className="ml-1 h-4 w-4" />
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
