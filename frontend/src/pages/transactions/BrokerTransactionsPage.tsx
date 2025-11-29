import { TransactionList } from "@/components/TransactionList";
import { useNavigate } from "react-router-dom";

export function BrokerTransactionsPage() {
  const navigate = useNavigate();

  return (
    <div className="space-y-6">
      <TransactionList
        language="en" // later: detect from user profile
        onNavigate={(route) => navigate(route)}
      />
    </div>
  );
}
