import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import axiosInstance from "@/api/axiosInstance";
import { TransactionSummary } from "@/components/TransactionSummary";

export function ClientTransactionsPage() {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  // NOTE: replace with real client ID from Auth0 later
  const mockClientId = "CLIENT123";

  useEffect(() => {
    const loadClientTransactions = async () => {
      try {
        const res = await axiosInstance.get(`/clients/${mockClientId}/transactions`);
        setTransactions(res.data);
      } catch (err) {
        console.error("Failed to load client transactions:", err);
      } finally {
        setLoading(false);
      }
    };

    loadClientTransactions();
  }, []);

  if (loading) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-semibold">My Transactions</h1>
        <p className="text-sm text-muted-foreground mt-2">Loading...</p>
      </div>
    );
  }

  if (transactions.length === 0) {
    return (
      <div className="space-y-4 p-6">
        <h1 className="text-2xl font-semibold">My Transactions</h1>
        <p className="text-sm text-muted-foreground">
          You currently have no active transactions.
        </p>

        <button
          onClick={() => navigate("/")}
          className="px-4 py-2 rounded-lg"
          style={{ backgroundColor: "#FF6B01", color: "#FFFFFF" }}
        >
          Return Home
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">My Transactions</h1>
      <p className="text-sm text-muted-foreground">
        View your active, closed, and past transactions.
      </p>

      <div className="space-y-6">
        {transactions.map((tx) => (
          <div key={tx.transactionId} className="rounded-xl shadow p-4 bg-white">
            <TransactionSummary
              language="en"
              transactionId={tx.transactionId}
            />
          </div>
        ))}
      </div>
    </div>
  );
}
