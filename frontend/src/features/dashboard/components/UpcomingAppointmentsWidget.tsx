import { useTranslation } from "react-i18next";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/components/ui/card";
import { Calendar } from "lucide-react";

export function UpcomingAppointmentsWidget() {
  const { t } = useTranslation("dashboard");

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div>
            <CardTitle className="text-lg font-semibold">
              {t("appointments.title", "Upcoming Appointments")}
            </CardTitle>
            <CardDescription className="text-sm text-muted-foreground mt-1">
              {t("appointments.description", "Your schedule for the next 7 days.")}
            </CardDescription>
          </div>
          <Calendar className="w-5 h-5 text-muted-foreground" />
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">
          {t("appointments.placeholder", "Calendar placeholder...")}
        </p>
      </CardContent>
    </Card>
  );
}
