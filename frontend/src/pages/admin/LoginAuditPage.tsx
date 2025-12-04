import { useEffect, useState } from 'react';
import { getLoginAuditEvents, type LoginAuditEvent } from './loginAuditApi.ts';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { formatDistanceToNow } from 'date-fns';

export function LoginAuditPage() {
  const [events, setEvents] = useState<LoginAuditEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadEvents() {
      try {
        const data = await getLoginAuditEvents();
        setEvents(data);
      } catch (err) {
        console.error('Failed to load login audit events:', err);
        setError('Failed to load login audit events');
      } finally {
        setLoading(false);
      }
    }

    loadEvents();
  }, []);

  if (loading) {
    return (
      <div className="p-6">
        <LoadingSpinner message="Loading audit logs..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-2 p-6">
        <h1 className="text-2xl font-semibold">Admin – Login Audit</h1>
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-2xl font-semibold">Admin – Login Audit</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Review login audit logs and security events. Total events: {events.length}
        </p>
      </div>

      <div className="rounded-lg border bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="border-b bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-600">
                  Timestamp
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-600">
                  User
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-600">
                  Role
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-600">
                  IP Address
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-600">
                  User Agent
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {events.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-500">
                    No login audit events found
                  </td>
                </tr>
              ) : (
                events.map((event) => (
                  <tr key={event.id} className="hover:bg-slate-50">
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-900">
                      <div>{new Date(event.timestamp).toLocaleString()}</div>
                      <div className="text-xs text-slate-500">
                        {formatDistanceToNow(new Date(event.timestamp), { addSuffix: true })}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <div className="font-medium text-slate-900">{event.email}</div>
                      <div className="text-xs text-slate-500">{event.userId}</div>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span
                        className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold ${
                          event.role === 'ADMIN'
                            ? 'bg-purple-100 text-purple-800'
                            : event.role === 'BROKER'
                              ? 'bg-blue-100 text-blue-800'
                              : 'bg-green-100 text-green-800'
                        }`}
                      >
                        {event.role}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600">
                      {event.ipAddress || 'N/A'}
                    </td>
                    <td className="max-w-xs truncate px-4 py-3 text-sm text-slate-600">
                      {event.userAgent || 'N/A'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
