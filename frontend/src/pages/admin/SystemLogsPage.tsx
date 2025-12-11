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
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <PageHeader
          title={t("settings.systemLogs")}
          subtitle={t("settings.systemLogsSubtitle")}
        />
      </div>

      <div className="bg-card shadow rounded-xl border border-border overflow-hidden">
        {loading && (
          <p className="p-8 text-center text-muted-foreground">
            {t("settings.loading")}
          </p>
        )}

        {error && (
          <p className="p-8 text-center text-destructive">
            {error}
          </p>
        )}

        {!loading && !error && (
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/50 hover:bg-muted/50">
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
                  <TableCell colSpan={5} className="h-24 text-center text-muted-foreground">
                    {t("settings.noSystemLogs")}
                  </TableCell>
                </TableRow>
              )}

              {logs.map((log) => (
                <Fragment key={log.id}>
                  <TableRow
                    className="cursor-pointer hover:bg-muted/30 transition-colors"
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

                    <TableCell className="whitespace-nowrap text-sm text-foreground">
                      {log.timestamp ? formatDateTime(log.timestamp) : "—"}
                    </TableCell>

                    <TableCell className="text-sm text-foreground">
                      {log.inviteTemplateEnChanged
                        ? <span className="text-orange-600 dark:text-orange-400 font-medium">{t("settings.systemLogs_changed")}</span>
                        : <span className="text-muted-foreground">{t("settings.systemLogs_notChanged")}</span>}
                    </TableCell>

                    <TableCell className="text-sm text-foreground">
                      {log.inviteTemplateFrChanged
                        ? <span className="text-orange-600 dark:text-orange-400 font-medium">{t("settings.systemLogs_changed")}</span>
                        : <span className="text-muted-foreground">{t("settings.systemLogs_notChanged")}</span>}
                    </TableCell>

                    <TableCell className="text-sm text-foreground">
                      {formatSummary(log)}
                    </TableCell>
                  </TableRow>

                  {expandedRows.has(log.id) && (
                    <TableRow className="bg-muted/30 hover:bg-muted/30 animate-in fade-in duration-200">
                      <TableCell colSpan={5} className="p-0 border-t border-b border-border">
                        <div className="p-4 space-y-3 bg-muted/10">
                          <div className="grid grid-cols-[180px_1fr] gap-4 text-sm">
                            <span className="font-medium text-muted-foreground">
                              {t("settings.systemLogs_defaultLanguageLabel")}:
                            </span>
                            <span className="text-foreground">
                              {formatLang(log.previousDefaultLanguage)}{" "}
                              <span className="text-muted-foreground px-2">→</span>
                              {formatLang(log.newDefaultLanguage)}
                            </span>
                          </div>

                          <div className="grid grid-cols-[180px_1fr] gap-4 text-sm">
                            <span className="font-medium text-muted-foreground">
                              {t("eventId")}:
                            </span>
                            <span className="font-mono text-xs text-muted-foreground bg-muted p-1 rounded w-fit">{log.id}</span>
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
