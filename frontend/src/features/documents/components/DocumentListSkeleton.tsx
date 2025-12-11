import { Skeleton } from "@/shared/components/ui/skeleton";
import { Section } from "@/shared/components/branded/Section";

export function DocumentListSkeleton() {
    return (
        <div className="space-y-4">
            {[1, 2, 3].map((i) => (
                <Section key={i} className="p-4 border border-border">
                    <div className="flex items-start justify-between">
                        <div className="flex gap-4 w-full">
                            <Skeleton className="w-10 h-10 rounded-lg" />
                            <div className="space-y-2 flex-1">
                                <Skeleton className="h-5 w-1/3" />
                                <div className="flex items-center gap-2">
                                    <Skeleton className="h-4 w-4 rounded-full" />
                                    <Skeleton className="h-4 w-1/4" />
                                </div>
                            </div>
                        </div>
                        <div className="flex flex-col items-end gap-3 min-w-[100px]">
                            <Skeleton className="h-6 w-20 rounded-full" />
                            <div className="flex gap-2">
                                <Skeleton className="h-9 w-24" />
                            </div>
                        </div>
                    </div>
                </Section>
            ))}
        </div>
    );
}
