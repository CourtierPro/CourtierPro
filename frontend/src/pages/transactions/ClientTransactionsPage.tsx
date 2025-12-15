import { useNavigate } from "react-router-dom";
import { ClientTransactionList } from "@/features/transactions/components/ClientTransactionList";

export function ClientTransactionsPage() {
  const navigate = useNavigate();

  return (
    <ClientTransactionList onNavigate={(route) => navigate(route)} />
  );
}
