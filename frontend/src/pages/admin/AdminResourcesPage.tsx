import { useState } from "react";
import {
    Database,
    FileText,
    Trash2,
    RotateCcw,
    AlertTriangle,
    Search,
    History,
    ChevronDown,
    ChevronRight,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Badge } from "@/shared/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/components/ui/tabs";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/shared/components/ui/dialog";
import { Checkbox } from "@/shared/components/ui/checkbox";
import { Label } from "@/shared/components/ui/label";
import {
    useAdminResources,
    useResourcePreview,
    useAuditHistory,
} from "@/features/admin/api/queries";
import { useDeleteResource, useRestoreResource } from "@/features/admin/api/mutations";
import { type ResourceType, type ResourceItem } from "@/features/admin/api/adminResourcesApi";
import { logError, getErrorMessage } from "@/shared/utils/error-utils";

export function AdminResourcesPage() {
    const { t } = useTranslation("admin");
    const [activeTab, setActiveTab] = useState<ResourceType | "deleted" | "audit">("TRANSACTION");
    const [searchTerm, setSearchTerm] = useState("");
    const [selectedResource, setSelectedResource] = useState<{
        type: ResourceType;
        id: string;
    } | null>(null);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [confirmChecked, setConfirmChecked] = useState(false);
    const [expandedLogIds, setExpandedLogIds] = useState<Set<number>>(new Set());
    const [expandedResourceIds, setExpandedResourceIds] = useState<Set<string>>(new Set());

    // Queries - regular tabs (active resources only)
    const transactionsQuery = useAdminResources("TRANSACTION", false);
    const documentsQuery = useAdminResources("DOCUMENT_REQUEST", false);

    // Queries - deleted tab (all resources including deleted, then filter client-side)
    const deletedTransactionsQuery = useAdminResources("TRANSACTION", true);
    const deletedDocumentsQuery = useAdminResources("DOCUMENT_REQUEST", true);

    const previewQuery = useResourcePreview(
        selectedResource?.type || "TRANSACTION",
        selectedResource?.id || null
    );
    const auditQuery = useAuditHistory();

    // Mutations
    const deleteResource = useDeleteResource();
    const restoreResource = useRestoreResource();

    const handleDeleteClick = (type: ResourceType, id: string) => {
        setSelectedResource({ type, id });
        setShowDeleteModal(true);
        setConfirmChecked(false);
    };

    const handleConfirmDelete = async () => {
        if (!selectedResource || !confirmChecked) return;

        try {
            await deleteResource.mutateAsync({
                type: selectedResource.type,
                resourceId: selectedResource.id,
            });
            toast.success(t("resourceDeleted"));
            setShowDeleteModal(false);
            setSelectedResource(null);
        } catch (err) {
            const message = getErrorMessage(err, t("failedToDeleteResource"));
            toast.error(message);
            if (err instanceof Error) logError(err);
        }
    };

    const handleRestore = async (type: ResourceType, id: string) => {
        try {
            await restoreResource.mutateAsync({ type, resourceId: id });
            toast.success(t("resourceRestored"));
        } catch (err) {
            const message = getErrorMessage(err, t("failedToRestoreResource"));
            toast.error(message);
            if (err instanceof Error) logError(err);
        }
    };

    const filterItems = (items: ResourceItem[] | undefined, onlyDeleted = false) => {
        if (!items) return [];
        let filtered = items;

        // Filter by deleted status if onlyDeleted is true
        if (onlyDeleted) {
            filtered = filtered.filter((item) => item.deleted);
        }

        // Filter by search term
        if (searchTerm) {
            filtered = filtered.filter((item) =>
                item.summary.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        return filtered;
    };

    const renderResourceTable = (
        data: typeof transactionsQuery.data,
        isLoading: boolean,
        error: Error | null,
        type: ResourceType,
        showRestore = false,
        onlyDeleted = false
    ) => {
        if (isLoading) return <LoadingState />;
        if (error) return <ErrorState title={t("errorLoadingResources")} message={error.message} />;

        const items = filterItems(data?.items, onlyDeleted);

        const toggleResourceExpand = (id: string) => {
            setExpandedResourceIds(prev => {
                const newSet = new Set(prev);
                if (newSet.has(id)) {
                    newSet.delete(id);
                } else {
                    newSet.add(id);
                }
                return newSet;
            });
        };

        return (
            <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-muted/50 text-muted-foreground text-xs uppercase tracking-wider">
                            <tr>
                                <th className="w-10 px-3 py-4 font-medium"></th>
                                <th className="px-6 py-4 font-medium">{t("resource")}</th>
                                <th className="px-6 py-4 font-medium">{t("created")}</th>
                                <th className="px-6 py-4 font-medium">{t("status")}</th>
                                <th className="px-6 py-4 font-medium text-right">
                                    <span className="sr-only">Actions</span>
                                </th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-border">
                            {items.length > 0 ? (
                                items.map((item) => {
                                    const isExpanded = expandedResourceIds.has(item.id);

                                    return (
                                        <>
                                            <tr
                                                key={item.id}
                                                className="group transition-colors hover:bg-muted/30 cursor-pointer"
                                                onClick={() => toggleResourceExpand(item.id)}
                                            >
                                                <td className="px-3 py-4 text-center">
                                                    {isExpanded
                                                        ? <ChevronDown className="h-4 w-4 text-muted-foreground" />
                                                        : <ChevronRight className="h-4 w-4 text-muted-foreground" />
                                                    }
                                                </td>
                                                <td className="px-6 py-4">
                                                    <div className="flex items-center gap-3">
                                                        {type === "TRANSACTION" ? (
                                                            <Database className="h-5 w-5 text-muted-foreground" />
                                                        ) : (
                                                            <FileText className="h-5 w-5 text-muted-foreground" />
                                                        )}
                                                        <div className="flex flex-col">
                                                            <div className="font-semibold text-foreground">{item.summary}</div>
                                                            <div className="text-xs text-muted-foreground font-mono">{item.id}</div>
                                                        </div>
                                                    </div>
                                                </td>
                                                <td className="px-6 py-4 text-muted-foreground">
                                                    {item.createdAt
                                                        ? new Date(item.createdAt).toLocaleDateString()
                                                        : "-"}
                                                </td>
                                                <td className="px-6 py-4">
                                                    <Badge variant={item.deleted ? "destructive" : "success"}>
                                                        {item.deleted ? t("deleted") : t("active")}
                                                    </Badge>
                                                </td>
                                                <td className="px-6 py-4 text-right" onClick={(e) => e.stopPropagation()}>
                                                    {showRestore && item.deleted ? (
                                                        <Button
                                                            variant="ghost"
                                                            size="sm"
                                                            onClick={() => handleRestore(type, item.id)}
                                                            className="text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 dark:text-emerald-400"
                                                        >
                                                            <RotateCcw className="h-4 w-4 mr-1" />
                                                            {t("restore")}
                                                        </Button>
                                                    ) : !item.deleted ? (
                                                        <Button
                                                            variant="ghost"
                                                            size="sm"
                                                            onClick={() => handleDeleteClick(type, item.id)}
                                                            className="text-destructive hover:text-destructive hover:bg-destructive/10"
                                                        >
                                                            <Trash2 className="h-4 w-4 mr-1" />
                                                            {t("delete")}
                                                        </Button>
                                                    ) : null}
                                                </td>
                                            </tr>
                                            {isExpanded && (
                                                <tr key={`${item.id}-details`} className="bg-muted/20">
                                                    <td colSpan={5} className="px-10 py-4">
                                                        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 text-sm">
                                                            {type === "TRANSACTION" && (
                                                                <>
                                                                    <div>
                                                                        <span className="text-muted-foreground">{t("clientEmail")}:</span>
                                                                        <div className="font-medium">{item.clientEmail || "—"}</div>
                                                                    </div>
                                                                    <div>
                                                                        <span className="text-muted-foreground">{t("brokerEmail")}:</span>
                                                                        <div className="font-medium">{item.brokerEmail || "—"}</div>
                                                                    </div>
                                                                    <div>
                                                                        <span className="text-muted-foreground">{t("side")}:</span>
                                                                        <div className="font-medium">{item.side || "—"}</div>
                                                                    </div>
                                                                    <div>
                                                                        <span className="text-muted-foreground">{t("propertyAddress")}:</span>
                                                                        <div className="font-medium">{item.address || "—"}</div>
                                                                    </div>
                                                                </>
                                                            )}
                                                            {type === "DOCUMENT_REQUEST" && (
                                                                <>
                                                                    <div>
                                                                        <span className="text-muted-foreground">{t("parentTransaction")}:</span>
                                                                        <div className="font-mono text-xs">{item.transactionId || "—"}</div>
                                                                    </div>
                                                                    <div>
                                                                        <span className="text-muted-foreground">{t("documentType")}:</span>
                                                                        <div className="font-medium">
                                                                            <Badge variant="outline">{item.docType || "—"}</Badge>
                                                                        </div>
                                                                    </div>
                                                                    <div>
                                                                        <span className="text-muted-foreground">{t("submittedDocuments")}:</span>
                                                                        <div className="font-medium">{item.submittedDocCount}</div>
                                                                    </div>
                                                                </>
                                                            )}
                                                        </div>
                                                    </td>
                                                </tr>
                                            )}
                                        </>
                                    );
                                })
                            ) : (
                                <tr>
                                    <td colSpan={5} className="px-6 py-8 text-center text-muted-foreground">
                                        {t("noResourcesFound")}
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    };

    const renderAuditHistory = () => {
        if (auditQuery.isLoading) return <LoadingState />;
        if (auditQuery.error)
            return <ErrorState title={t("errorLoadingAuditHistory")} message={auditQuery.error.message} />;

        const logs = auditQuery.data || [];

        const parseCascaded = (cascadedJson: string | null): string[] => {
            if (!cascadedJson) return [];
            try {
                const parsed = JSON.parse(cascadedJson);
                return Array.isArray(parsed) ? parsed : [];
            } catch {
                return [];
            }
        };

        const toggleExpand = (logId: number) => {
            setExpandedLogIds(prev => {
                const newSet = new Set(prev);
                if (newSet.has(logId)) {
                    newSet.delete(logId);
                } else {
                    newSet.add(logId);
                }
                return newSet;
            });
        };

        return (
            <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-muted/50 text-muted-foreground text-xs uppercase tracking-wider">
                            <tr>
                                <th className="w-10 px-3 py-4 font-medium"></th>
                                <th className="px-6 py-4 font-medium">{t("action")}</th>
                                <th className="px-6 py-4 font-medium">{t("timestamp")}</th>
                                <th className="px-6 py-4 font-medium">{t("admin")}</th>
                                <th className="px-6 py-4 font-medium">{t("resourceType")}</th>
                                <th className="px-6 py-4 font-medium">{t("resourceId")}</th>
                                <th className="px-6 py-4 font-medium">{t("relatedItems")}</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-border">
                            {logs.length > 0 ? (
                                logs.map((log) => {
                                    const cascadedItems = parseCascaded(log.cascadedDeletions);
                                    const hasRelated = cascadedItems.length > 0;
                                    const isExpanded = expandedLogIds.has(log.id);

                                    return (
                                        <>
                                            <tr
                                                key={log.id}
                                                className={`hover:bg-muted/30 ${hasRelated ? "cursor-pointer" : ""}`}
                                                onClick={() => hasRelated && toggleExpand(log.id)}
                                            >
                                                <td className="px-3 py-4 text-center">
                                                    {hasRelated && (
                                                        isExpanded
                                                            ? <ChevronDown className="h-4 w-4 text-muted-foreground" />
                                                            : <ChevronRight className="h-4 w-4 text-muted-foreground" />
                                                    )}
                                                </td>
                                                <td className="px-6 py-4">
                                                    <Badge
                                                        variant={log.action === "DELETE" ? "destructive" : "success"}
                                                    >
                                                        {log.action === "DELETE" ? t("delete") : t("restore")}
                                                    </Badge>
                                                </td>
                                                <td className="px-6 py-4 text-foreground">
                                                    {new Date(log.timestamp).toLocaleString()}
                                                </td>
                                                <td className="px-6 py-4 text-muted-foreground">{log.adminEmail}</td>
                                                <td className="px-6 py-4">
                                                    <Badge variant="outline">{log.resourceType}</Badge>
                                                </td>
                                                <td className="px-6 py-4 font-mono text-xs text-muted-foreground">
                                                    {log.resourceId}
                                                </td>
                                                <td className="px-6 py-4">
                                                    {hasRelated ? (
                                                        <Badge variant="secondary">
                                                            +{cascadedItems.length} {t("items")}
                                                        </Badge>
                                                    ) : (
                                                        <span className="text-muted-foreground">—</span>
                                                    )}
                                                </td>
                                            </tr>
                                            {isExpanded && hasRelated && (
                                                <tr key={`${log.id}-expanded`} className="bg-muted/20">
                                                    <td colSpan={7} className="px-10 py-4">
                                                        <div className="text-sm">
                                                            <p className="font-medium text-muted-foreground mb-2">
                                                                {t("relatedItemsAffected")}:
                                                            </p>
                                                            <ul className="space-y-1 ml-4">
                                                                {cascadedItems.map((item, idx) => {
                                                                    const [type, id] = item.split(":");
                                                                    return (
                                                                        <li key={idx} className="flex items-center gap-2 text-xs">
                                                                            <Badge variant="outline" className="text-xs">
                                                                                {type}
                                                                            </Badge>
                                                                            <span className="font-mono text-muted-foreground">
                                                                                {id}
                                                                            </span>
                                                                        </li>
                                                                    );
                                                                })}
                                                            </ul>
                                                        </div>
                                                    </td>
                                                </tr>
                                            )}
                                        </>
                                    );
                                })
                            ) : (
                                <tr>
                                    <td colSpan={7} className="px-6 py-8 text-center text-muted-foreground">
                                        {t("noAuditLogs")}
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    };

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            {/* Header */}
            <PageHeader
                title={t("resources")}
                subtitle={t("browseAndManageResources")}
            />

            {/* Tabs */}
            <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as typeof activeTab)}>
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                    <TabsList>
                        <TabsTrigger value="TRANSACTION">
                            <Database className="h-4 w-4 mr-1" />
                            {t("transactions")}
                        </TabsTrigger>
                        <TabsTrigger value="DOCUMENT_REQUEST">
                            <FileText className="h-4 w-4 mr-1" />
                            {t("documents")}
                        </TabsTrigger>
                        <TabsTrigger value="deleted">
                            <Trash2 className="h-4 w-4 mr-1" />
                            {t("deletedResources")}
                        </TabsTrigger>
                        <TabsTrigger value="audit">
                            <History className="h-4 w-4 mr-1" />
                            {t("auditLog")}
                        </TabsTrigger>
                    </TabsList>
                </div>

                {/* Search bar */}
                {activeTab !== "audit" && (
                    <div className="bg-card rounded-xl border border-border p-4 shadow-sm">
                        <div className="relative flex items-center">
                            <div className="absolute left-3 flex items-center pointer-events-none">
                                <Search className="h-5 w-5 text-muted-foreground" />
                            </div>
                            <Input
                                type="text"
                                className="pl-10"
                                placeholder={t("searchResources")}
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                        </div>
                    </div>
                )}

                <TabsContent value="TRANSACTION">
                    {renderResourceTable(
                        transactionsQuery.data,
                        transactionsQuery.isLoading,
                        transactionsQuery.error,
                        "TRANSACTION"
                    )}
                </TabsContent>

                <TabsContent value="DOCUMENT_REQUEST">
                    {renderResourceTable(
                        documentsQuery.data,
                        documentsQuery.isLoading,
                        documentsQuery.error,
                        "DOCUMENT_REQUEST"
                    )}
                </TabsContent>

                <TabsContent value="deleted">
                    <div className="space-y-6">
                        <h3 className="text-lg font-semibold">{t("deletedTransactions")}</h3>
                        {renderResourceTable(
                            deletedTransactionsQuery.data,
                            deletedTransactionsQuery.isLoading,
                            deletedTransactionsQuery.error,
                            "TRANSACTION",
                            true,
                            true
                        )}
                        <h3 className="text-lg font-semibold">{t("deletedDocuments")}</h3>
                        {renderResourceTable(
                            deletedDocumentsQuery.data,
                            deletedDocumentsQuery.isLoading,
                            deletedDocumentsQuery.error,
                            "DOCUMENT_REQUEST",
                            true,
                            true
                        )}
                    </div>
                </TabsContent>

                <TabsContent value="audit">{renderAuditHistory()}</TabsContent>
            </Tabs>

            {/* Delete Confirmation Modal */}
            <Dialog open={showDeleteModal} onOpenChange={setShowDeleteModal}>
                <DialogContent className="sm:max-w-lg">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2 text-destructive">
                            <AlertTriangle className="h-5 w-5" />
                            {t("confirmDeletion")}
                        </DialogTitle>
                        <DialogDescription>
                            {t("deletionWarning")}
                        </DialogDescription>
                    </DialogHeader>

                    {previewQuery.data && (
                        <div className="space-y-4 py-4">
                            <div className="rounded-lg bg-muted p-4">
                                <div className="font-semibold">{previewQuery.data.resourceSummary}</div>
                                <div className="text-xs text-muted-foreground font-mono mt-1">
                                    {previewQuery.data.resourceId}
                                </div>
                            </div>

                            {previewQuery.data.linkedResources.length > 0 && (
                                <div>
                                    <h4 className="text-sm font-medium mb-2">{t("linkedResourcesWillBeDeleted")}</h4>
                                    <ul className="text-sm text-muted-foreground space-y-1 max-h-32 overflow-auto">
                                        {previewQuery.data.linkedResources.map((lr, i) => (
                                            <li key={i} className="flex items-center gap-2">
                                                <Badge variant="outline" className="text-xs">
                                                    {lr.type}
                                                </Badge>
                                                {lr.summary}
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}

                            {previewQuery.data.s3FilesToDelete.length > 0 && (
                                <div>
                                    <h4 className="text-sm font-medium mb-2 text-destructive">
                                        {t("filesWillBePermanentlyDeleted")}
                                    </h4>
                                    <ul className="text-sm text-muted-foreground space-y-1">
                                        {previewQuery.data.s3FilesToDelete.map((f, i) => (
                                            <li key={i}>
                                                {f.fileName} ({(f.sizeBytes / 1024).toFixed(1)} KB)
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </div>
                    )}

                    <div className="flex items-center space-x-2 py-4 border-t">
                        <Checkbox
                            id="confirm-delete"
                            checked={confirmChecked}
                            onCheckedChange={(checked) => setConfirmChecked(checked === true)}
                        />
                        <Label htmlFor="confirm-delete" className="text-sm">
                            {t("iUnderstandThisActionIsIrreversible")}
                        </Label>
                    </div>

                    <DialogFooter>
                        <Button variant="outline" onClick={() => setShowDeleteModal(false)}>
                            {t("cancel")}
                        </Button>
                        <Button
                            variant="destructive"
                            onClick={handleConfirmDelete}
                            disabled={!confirmChecked || deleteResource.isPending}
                        >
                            {deleteResource.isPending ? t("deleting") : t("confirmDelete")}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
