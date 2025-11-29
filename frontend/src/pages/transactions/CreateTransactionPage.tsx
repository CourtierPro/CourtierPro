import React from 'react';
import { useNavigate } from 'react-router-dom';
import { TransactionCreateForm } from '@/components/TransactionCreateForm';

export default function CreateTransactionPage() {
  const navigate = useNavigate();

  return (
    <div className="space-y-6">
      <TransactionCreateForm
        language="en"
        onNavigate={(route) => navigate(route)}
      />
    </div>
  );
}
