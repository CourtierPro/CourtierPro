import { useState, Fragment } from "react";
import { useTranslation } from "react-i18next";
import { formatDateTime } from "@/shared/utils/date";
import { ChevronDown, ChevronRight } from "lucide-react";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { useLoginAudit } from "@/features/admin/hooks/useLoginAudit";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/components/ui/table";
import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";

export function LoginAuditPage() {
  const { t } = useTranslation("admin");
  const { data: events, isLoading, error } = useLoginAudit();
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  const toggleRow = (id: string) => {
    const newExpandedRows = new Set(expandedRows);
    if (newExpandedRows.has(id)) {
      newExpandedRows.delete(id);
    } else {
      newExpandedRows.add(id);
    }
    setExpandedRows(newExpandedRows);
  };

  if (isLoading) {
    return <LoadingState message={t("loadingLoginAudit")} />;
  }

  if (error) {
    return (
      <ErrorState
        title={t("errorLoadingLoginAudit")}
        message={error.message}
      />
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={t("loginAudit")}
        subtitle={t("loginAuditSubtitle")}
      />
      <div className="bg-card shadow rounded-lg border border-border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[50px]"></TableHead>
              <TableHead>{t("timestamp")}</TableHead>
              <TableHead>{t("loginUser")}</TableHead>
              <TableHead>{t("role")}</TableHead>
              <TableHead>{t("ipAddress")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {events?.map((event) => (
              <Fragment key={event.id}>
                <TableRow
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => toggleRow(event.id)}
                >
                  <TableCell>
                    <Button variant="ghost" size="icon" className="h-8 w-8 p-0">
                      {expandedRows.has(event.id) ? (
                        <ChevronDown className="h-4 w-4" />
                      ) : (
                        <ChevronRight className="h-4 w-4" />
                      )}
                    </Button>
                  </TableCell>
                  <TableCell>
                    {event.timestamp ? formatDateTime(event.timestamp) : "-"}
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="font-medium">{event.email}</span>
                      <span className="text-xs text-muted-foreground">
                        {event.userId}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{event.role}</Badge>
                  </TableCell>
                  <TableCell>{event.ipAddress || "-"}</TableCell>
                </TableRow>
                {expandedRows.has(event.id) && (
                  <TableRow className="bg-muted/50 hover:bg-muted/50">
                    <TableCell colSpan={5}>
                      <div className="p-4 space-y-2 text-sm">
                        <div className="grid grid-cols-[100px_1fr] gap-2">
                          <span className="font-medium text-muted-foreground">
                            {t("userAgent")}:
                          </span>
                          <span className="break-all">
                            {event.userAgent || "-"}
                          </span>
                        </div>
                        <div className="grid grid-cols-[100px_1fr] gap-2">
                          <span className="font-medium text-muted-foreground">
                            {t("eventId")}:
                          </span>
                          <span className="font-mono text-xs">{event.id}</span>
                        </div>
                      </div>
                    </TableCell>
                  </TableRow>
                )}
              </Fragment>
            ))}
            {(!events || events.length === 0) && (
              <TableRow>
                <TableCell colSpan={5} className="h-24 text-center">
                  {t("noLoginEvents")}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
