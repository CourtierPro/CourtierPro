import { useParams, useNavigate } from "react-router-dom";
import { TransactionSummary } from "@/components/TransactionSummary";

export function ClientTransactionDetailsPage() {
  const { transactionId } = useParams();
  const navigate = useNavigate();

  if (!transactionId) {
    return (
      <div className="p-6">
        <h1 className="text-xl font-semibold">Transaction Not Found</h1>
        <p className="text-sm text-muted-foreground">
          No transaction ID was provided in the URL.
        </p>

        <button
          onClick={() => navigate("/my-transaction")}
          className="mt-4 px-4 py-2 rounded-lg"
          style={{ backgroundColor: "#FF6B01", color: "#FFFFFF" }}
        >
          Go Back
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <TransactionSummary
        language="en"     // TODO: hook to client's preferred language
        transactionId={transactionId}
      />
    </div>
  );
}
