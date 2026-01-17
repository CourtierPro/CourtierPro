import { ExpiringOffersCard } from "./ExpiringOffersCard";
import { PendingDocumentsCard } from "./PendingDocumentsCard";
// import { UpcomingAppointmentsCard } from "./UpcomingAppointmentsCard";

interface PriorityCardsSectionProps {
    className?: string;
}

export function PriorityCardsSection({ className }: PriorityCardsSectionProps) {
    return (
        <div className={className}>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <ExpiringOffersCard />
                <PendingDocumentsCard />
                {/* <UpcomingAppointmentsCard /> */}
            </div>
        </div>
    );
}
