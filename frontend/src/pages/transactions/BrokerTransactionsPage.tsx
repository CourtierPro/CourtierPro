import { TransactionList } from "@/features/transactions/components/TransactionList";
import { ErrorBoundary } from "@/shared/components/error/ErrorBoundary";
import { useNavigate } from "react-router-dom";

import { useLanguage } from '@/app/providers/LanguageContext';

export function BrokerTransactionsPage() {
  const navigate = useNavigate();
  const { language } = useLanguage();

  return (
    <div className="space-y-6">
      <ErrorBoundary>
        <TransactionList
          language={language}
          onNavigate={(route) => navigate(route)}
        />
      </ErrorBoundary>
    </div>
  );
}
