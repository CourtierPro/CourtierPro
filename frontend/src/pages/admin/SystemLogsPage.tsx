// frontend/src/pages/admin/SystemLogsPage.tsx
import { Fragment, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { ChevronDown, ChevronRight } from "lucide-react";

import { PageHeader } from "@/shared/components/branded/PageHeader";
import { formatDateTime } from "@/shared/utils/date";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/components/ui/table";
import { Button } from "@/shared/components/ui/button";

import {
  fetchOrgSettingsLogs,
  type OrgSettingsLogEntry,
} from "@/pages/admin/systemLogsApi";

import "./SystemLogsPage.css";

export function SystemLogsPage() {
  const { t } = useTranslation("admin");

  const [logs, setLogs] = useState<OrgSettingsLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  useEffect(() => {
    let isMounted = true;

    (async () => {
      try {
        setLoading(true);
        const data = await fetchOrgSettingsLogs();
        if (isMounted) {
          setLogs(data);
          setError(null);
        }
      } catch {
        toast.error(t("settings.errors.loadFailed"));
        if (isMounted) {
          setError(t("settings.errors.loadFailed"));
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    })();

    return () => {
      isMounted = false;
    };
  }, [t]);

  const toggleRow = (id: string) => {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const formatLang = (code: string | null) => {
    if (!code) return "—";
    if (code === "en") return t("settings.languages.en");
    if (code === "fr") return t("settings.languages.fr");
    return code.toUpperCase();
  };

  const formatSummary = (log: OrgSettingsLogEntry) => {
    if (!log.previousDefaultLanguage || !log.newDefaultLanguage) {
      return t("settings.systemLogs_languageUnchanged");
    }

    if (log.previousDefaultLanguage === log.newDefaultLanguage) {
      return t("settings.systemLogs_languageUnchanged");
    }

    return t("settings.systemLogs_languageChanged", {
      from: formatLang(log.previousDefaultLanguage),
      to: formatLang(log.newDefaultLanguage),
    });
  };

  return (
    <div className="admin-page system-logs-page">
      <div className="system-logs-header-wrapper">
        <PageHeader
          title={t("settings.systemLogs")}
          subtitle={t("settings.systemLogsSubtitle")}
        />
      </div>

      <div className="bg-white shadow rounded-lg overflow-hidden system-logs-card">
        {loading && (
          <p className="system-logs-status system-logs-status--muted">
            {t("settings.loading")}
          </p>
        )}

        {error && (
          <p className="system-logs-status system-logs-status--error">
            {error}
          </p>
        )}

        {!loading && !error && (
          <Table className="admin-table system-logs-table">
            <TableHeader>
              <TableRow>
                <TableHead className="w-[50px]" />
                <TableHead>{t("settings.systemLogs_when")}</TableHead>
                <TableHead>{t("settings.systemLogs_inviteTemplateEn")}</TableHead>
                <TableHead>{t("settings.systemLogs_inviteTemplateFr")}</TableHead>
                <TableHead>
                  {t("settings.systemLogs_defaultLanguageLabel")}
                </TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {logs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="h-24 text-center">
                    {t("settings.noSystemLogs")}
                  </TableCell>
                </TableRow>
              )}

              {logs.map((log, index) => (
                <Fragment key={log.id}>
                  <TableRow
                    className={`system-logs-row cursor-pointer hover:bg-muted/50 system-logs-row-${index + 1
                      }`}
                    onClick={() => toggleRow(log.id)}
                  >
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 p-0"
                      >
                        {expandedRows.has(log.id) ? (
                          <ChevronDown className="h-4 w-4" />
                        ) : (
                          <ChevronRight className="h-4 w-4" />
                        )}
                      </Button>
                    </TableCell>

                    <TableCell className="whitespace-nowrap text-sm">
                      {log.timestamp ? formatDateTime(log.timestamp) : "—"}
                    </TableCell>

                    <TableCell className="text-sm">
                      {log.inviteTemplateEnChanged
                        ? t("settings.systemLogs_changed")
                        : t("settings.systemLogs_notChanged")}
                    </TableCell>

                    <TableCell className="text-sm">
                      {log.inviteTemplateFrChanged
                        ? t("settings.systemLogs_changed")
                        : t("settings.systemLogs_notChanged")}
                    </TableCell>

                    <TableCell className="text-sm">
                      {formatSummary(log)}
                    </TableCell>
                  </TableRow>

                  {expandedRows.has(log.id) && (
                    <TableRow className="bg-muted/50 hover:bg-muted/50">
                      <TableCell colSpan={5}>
                        <div className="p-4 space-y-2 text-sm">
                          <div className="grid grid-cols-[150px_1fr] gap-2">
                            <span className="font-medium text-muted-foreground">
                              {t("settings.systemLogs_defaultLanguageLabel")}:
                            </span>
                            <span>
                              {formatLang(log.previousDefaultLanguage)}{" "}
                              {" → "}
                              {formatLang(log.newDefaultLanguage)}
                            </span>
                          </div>

                          <div className="grid grid-cols-[150px_1fr] gap-2">
                            <span className="font-medium text-muted-foreground">
                              {t("eventId")}:
                            </span>
                            <span className="font-mono text-xs">{log.id}</span>
                          </div>
                        </div>
                      </TableCell>
                    </TableRow>
                  )}
                </Fragment>
              ))}
            </TableBody>
          </Table>
        )}
      </div>
    </div>
  );
}
