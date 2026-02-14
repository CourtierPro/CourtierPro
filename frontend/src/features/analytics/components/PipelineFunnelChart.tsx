import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip as RechartsTooltip,
    ResponsiveContainer,
    Cell
} from "recharts";
import { Home, Target, ChevronLeft, ChevronRight } from "lucide-react";
import type { PipelineStageDTO } from "../types";
import { Button } from "@/shared/components/ui/button";
import { cn } from "@/shared/utils/utils";

interface PipelineFunnelChartProps {
    data: PipelineStageDTO[];
}

interface ClientRow {
    clientName: string;
    stageIndex: number;
    stageName: string;
    daysInStage: number;
}

interface TooltipPayload {
    payload: ClientRow;
}

const CustomTooltip = ({ active, payload, t }: {
    active?: boolean;
    payload?: TooltipPayload[];
    t: (key: string, options?: { value: string }) => string
}) => {
    if (active && payload && payload.length) {
        const row = payload[0].payload;
        return (
            <div className="bg-popover/95 backdrop-blur-sm border border-primary/20 p-3 rounded-lg shadow-xl text-sm leading-relaxed">
                <p className="font-bold mb-2 border-b border-primary/10 pb-1 text-primary italic">
                    {row.clientName}
                </p>
                <div className="space-y-1">
                    <div className="flex items-center justify-between gap-4">
                        <span className="text-muted-foreground text-xs">{t("sections.transactionPipeline")}:</span>
                        <span className="font-semibold text-xs">{row.stageName}</span>
                    </div>
                    <div className="flex items-center justify-between gap-4">
                        <span className="text-muted-foreground text-xs">{t("stats.avgDuration")}:</span>
                        <span className="font-bold text-xs">{row.daysInStage.toFixed(1)} {t("units.days")}</span>
                    </div>
                </div>
            </div>
        );
    }
    return null;
};

export function PipelineFunnelChart({ data }: PipelineFunnelChartProps) {
    const { t } = useTranslation("analytics");
    const [currentPage, setCurrentPage] = useState(0);
    const pageSize = 14;

    if (!data || data.length === 0) return null;

    // 1. Unroll data: Create a row for each client
    const stageMilestones = data.map((s, i) => ({
        index: i + 1,
        name: t(`stages.${s.stageName}`, s.stageName),
        rawName: s.stageName,
        avgDays: s.avgDays
    }));

    const clientRows: ClientRow[] = [];

    data.forEach((stage, stageIndex) => {
        if (stage.clients) {
            stage.clients.forEach(client => {
                clientRows.push({
                    clientName: client.clientName,
                    stageIndex: stageIndex + 1,
                    stageName: t(`stages.${stage.stageName}`, stage.stageName),
                    daysInStage: client.daysInStage
                });
            });
        }
    });

    // Sort clients by progress
    clientRows.sort((a, b) => b.stageIndex - a.stageIndex);

    // Pagination Logic
    const totalPages = Math.ceil(clientRows.length / pageSize);
    // Ensure currentPage is valid if data changes
    const validPage = Math.min(currentPage, Math.max(0, totalPages - 1));
    const paginatedRows = clientRows.slice(validPage * pageSize, (validPage + 1) * pageSize);

    if (clientRows.length === 0) {
        const isBuyer = data[0]?.stageName.startsWith('BUYER');
        const emptyMessage = isBuyer ? t("emptyBuyerPipeline") : t("emptySellerPipeline");
        const Icon = isBuyer ? Target : Home;

        return (
            <div className="w-full h-[320px] mt-6 bg-card/10 rounded-2xl border border-dashed border-primary/20 flex flex-col items-center justify-center p-8 text-center transition-all hover:bg-card/20 group">
                <div className="relative mb-6">
                    <div className="absolute inset-0 bg-primary/20 blur-2xl rounded-full opacity-0 group-hover:opacity-100 transition-opacity" />
                    <div className="relative w-20 h-20 bg-primary/10 rounded-3xl flex items-center justify-center ring-1 ring-primary/20 shadow-inner group-hover:scale-110 transition-transform duration-500 overflow-hidden">
                        <div className="absolute inset-0 bg-gradient-to-br from-primary/10 to-transparent" />
                        <Icon className="h-10 w-10 text-primary drop-shadow-sm" strokeWidth={1.5} />
                    </div>
                </div>

                <h4 className="text-lg font-semibold text-foreground/90 mb-2">
                    {isBuyer ? t("stageLabels.buyerStages") : t("stageLabels.sellerStages")}
                </h4>

                <p className="text-muted-foreground font-medium max-w-[320px] leading-relaxed text-sm">
                    {emptyMessage}
                </p>
            </div>
        );
    }

    // Calculate height based on pageSize or remaining clients
    const displayCount = Math.min(pageSize, paginatedRows.length);
    const chartHeight = Math.max(400, displayCount * 40 + 100);

    const translate = (key: string, options?: { value: string }) => t(key, options);

    return (
        <div className="relative group/chart">
            {/* Navigation Arrows - Side Centered */}
            {totalPages > 1 && (
                <>
                    <div className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-2 z-10 transition-all opacity-0 group-hover/chart:opacity-100 group-hover/chart:translate-x-0">
                        <Button
                            variant="ghost"
                            size="icon"
                            className={cn(
                                "h-12 w-8 rounded-l-none rounded-r-xl bg-background/80 blur-0 backdrop-blur-md border border-l-0 border-border/50 shadow-lg text-primary hover:bg-primary hover:text-primary-foreground",
                                currentPage === 0 && "opacity-20 cursor-not-allowed grayscale"
                            )}
                            onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                            disabled={currentPage === 0}
                        >
                            <ChevronLeft className="h-6 w-6" />
                        </Button>
                    </div>

                    <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-2 z-10 transition-all opacity-0 group-hover/chart:opacity-100 group-hover/chart:translate-x-0">
                        <Button
                            variant="ghost"
                            size="icon"
                            className={cn(
                                "h-12 w-8 rounded-r-none rounded-l-xl bg-background/80 blur-0 backdrop-blur-md border border-r-0 border-border/50 shadow-lg text-primary hover:bg-primary hover:text-primary-foreground",
                                currentPage === totalPages - 1 && "opacity-20 cursor-not-allowed grayscale"
                            )}
                            onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                            disabled={currentPage === totalPages - 1}
                        >
                            <ChevronRight className="h-6 w-6" />
                        </Button>
                    </div>

                    {/* Page Indicator */}
                    <div className="absolute -bottom-4 left-1/2 -translate-x-1/2 px-3 py-1 bg-background/50 backdrop-blur-xs rounded-full border border-border/30 text-[10px] font-bold text-muted-foreground tabular-nums">
                        {currentPage + 1} / {totalPages}
                    </div>
                </>
            )}

            <div style={{ height: chartHeight }} className="w-full mt-6 bg-card/30 rounded-xl border border-border/50 p-4 transition-all hover:border-primary/20 overflow-hidden">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                        data={paginatedRows}
                        layout="vertical"
                        margin={{ top: 20, right: 40, left: 20, bottom: 60 }}
                    >
                        <CartesianGrid
                            strokeDasharray="3 3"
                            horizontal={false}
                            vertical={true}
                            opacity={0.15}
                            stroke="currentColor"
                        />

                        {/* X Axis: The Stages (Numerical mapped to Stage Names) */}
                        <XAxis
                            type="number"
                            domain={[0, stageMilestones.length]}
                            ticks={[...stageMilestones.map(m => m.index)]}
                            tickFormatter={(value) => {
                                const milestone = stageMilestones.find(m => m.index === value);
                                return milestone ? milestone.name : "";
                            }}
                            stroke="currentColor"
                            fontSize={10}
                            tickLine={true}
                            axisLine={true}
                            className="text-muted-foreground font-medium"
                            interval={0}
                            angle={-30}
                            textAnchor="end"
                        />

                        {/* Y Axis: Individual Clients */}
                        <YAxis
                            dataKey="clientName"
                            type="category"
                            stroke="currentColor"
                            fontSize={11}
                            tickLine={false}
                            axisLine={true}
                            width={80}
                            className="text-muted-foreground italic"
                        />

                        <RechartsTooltip content={<CustomTooltip t={translate} />} cursor={{ fill: 'currentColor', opacity: 0.03 }} />

                        <Bar
                            dataKey="stageIndex"
                            barSize={20}
                            radius={[0, 10, 10, 0]}
                            animationDuration={1500}
                        >
                            {paginatedRows.map((_, index) => (
                                <Cell
                                    key={`cell-${index}`}
                                    className="fill-primary/80 hover:fill-primary transition-colors cursor-pointer"
                                    style={{
                                        filter: 'drop-shadow(2px 0px 4px rgba(var(--primary-rgb), 0.2))'
                                    }}
                                />
                            ))}
                        </Bar>
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}
