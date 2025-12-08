import { useParams } from "react-router-dom";
import { DocumentsPage } from "./DocumentsPage";

export function TransactionDocumentsPage() {
  const { transactionId } = useParams<{ transactionId: string }>();

  if (!transactionId) {
    return null; // Or some error state
  }

  return <DocumentsPage transactionId={transactionId} />;
}
