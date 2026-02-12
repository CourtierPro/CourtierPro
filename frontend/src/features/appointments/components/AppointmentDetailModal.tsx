import { useState, useMemo } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Textarea } from "@/shared/components/ui/textarea";
import { useTranslation } from "react-i18next";
import { type Appointment, getAppointmentTimeRange, getAppointmentDate } from "../types";
import { fr, enUS } from 'date-fns/locale';
import { Calendar, Clock, MapPin, User, FileText, Check, X, AlertTriangle, CalendarClock, Ban, Users } from "lucide-react";
import { useReviewAppointment, useCancelAppointment, useUpdateVisitorCount } from "../api/mutations";
import { useTransactionVisitors } from "@/features/transactions/api/queries";
import { Badge } from "@/shared/components/ui/badge";
import { getStatusBadgeVariant } from "../enums";
import { useAuth0 } from "@auth0/auth0-react";
import { format } from "date-fns";
import { getLocalDateString } from '@/shared/utils/date';
import { Section } from "@/shared/components/branded/Section";
import { toast } from "sonner";

import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/shared/components/ui/select';

interface AppointmentDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    appointment: Appointment | null;
    existingAppointments?: Appointment[];
}

export function AppointmentDetailModal({ isOpen, onClose, appointment, existingAppointments = [] }: AppointmentDetailModalProps) {
    const { t, i18n } = useTranslation('appointments');
    const { user } = useAuth0();
    const userRoles = (user?.['https://courtierpro.dev/roles'] as string[]) || [];
    const isBroker = userRoles.includes('BROKER');

    const [isDeclineOpen, setIsDeclineOpen] = useState(false);
    const [isCancelOpen, setIsCancelOpen] = useState(false);
    const [isRescheduleOpen, setIsRescheduleOpen] = useState(false);
    const [refusalReason, setRefusalReason] = useState("");
    const [cancellationReason, setCancellationReason] = useState("");

    // Reschedule state
    const [newDate, setNewDate] = useState("");
    const [newStartTime, setNewStartTime] = useState("");
    const [newEndTime, setNewEndTime] = useState("");

    const reviewMutation = useReviewAppointment();
    const cancelMutation = useCancelAppointment();
    const updateVisitorCountMutation = useUpdateVisitorCount();

    const [editingVisitorCount, setEditingVisitorCount] = useState(false);
    const [visitorCountValue, setVisitorCountValue] = useState<number>(0);

    const isShowingType = appointment ? (appointment.title.toLowerCase() === 'open_house' || appointment.title.toLowerCase() === 'private_showing') : false;
    const eventConcluded = appointment ? new Date(appointment.toDateTime) < new Date() : false;

    const { data: transactionVisitors = [] } = useTransactionVisitors(appointment?.transactionId || '');
    const linkedVisitor = appointment?.visitorId ? transactionVisitors.find(v => v.visitorId === appointment.visitorId) : null;

    // Logic to determine other party name
    // If I am broker, show client name. If I am client, show broker name.
    const otherPartyName = isBroker && appointment ? appointment.clientName : appointment?.brokerName || '';
    const otherPartyLabel = isBroker ? t('client') : t('broker');

    const getTimeSlots = () => {
        const slots: string[] = [];
        for (let hour = 9; hour <= 17; hour++) {
            for (const minute of [0, 30]) {
                if (hour === 17 && minute === 30) break;
                const timeStr = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
                slots.push(timeStr);
            }
        }
        return slots;
    };

    const formatTimeDisplay = (time: string) => {
        if (!time) return '';
        const displayTime = new Date(`2000-01-01T${time}`).toLocaleTimeString(
            i18n.language === 'en' ? 'en-US' : 'fr-FR',
            { hour: 'numeric', minute: '2-digit', hour12: i18n.language === 'en' }
        );
        return displayTime;
    };

    // Determine correctness of roles
    // InitiatorType is 'BROKER' | 'CLIENT'
    const myRole = isBroker ? 'BROKER' : 'CLIENT';

    // Can Review: Only if PROPOSED and I am NOT the initiator
    const canReview = appointment ? appointment.status === 'PROPOSED' && appointment.initiatedBy !== myRole : false;

    // Can Cancel:
    // 1. If CONFIRMED -> Both can cancel.
    // 2. If PROPOSED -> Only Initiator can cancel (Withdraw).
    // 3. Not already cancelled/declined.
    const canCancel = useMemo(() => {
        if (!appointment) return false;
        if (appointment.status === 'CANCELLED' || appointment.status === 'DECLINED') return false;

        if (appointment.status === 'CONFIRMED') return true;
        if (appointment.status === 'PROPOSED' && appointment.initiatedBy === myRole) return true;

        return false;
    }, [appointment, myRole]);

    // Conflict Check (Current Appointment)
    const hasConflict = useMemo(() => {
        if (!canReview || !appointment) return false;
        const aptStart = new Date(appointment.fromDateTime);
        const aptEnd = new Date(appointment.toDateTime);

        return existingAppointments.some(existing => {
            if (existing.appointmentId === appointment.appointmentId) return false; // Skip self
            if (existing.status === 'CANCELLED' || existing.status === 'DECLINED') return false;

            const existingStart = new Date(existing.fromDateTime);
            const existingEnd = new Date(existing.toDateTime);

            return aptStart < existingEnd && aptEnd > existingStart;
        });
    }, [appointment, existingAppointments, canReview]);

    // Conflict Check (New Proposed Time)
    const hasNewConflict = useMemo(() => {
        if (!isRescheduleOpen || !newDate || !newStartTime || !newEndTime || !appointment) return false;

        const start = new Date(`${newDate}T${newStartTime}`);
        const end = new Date(`${newDate}T${newEndTime}`);

        return existingAppointments.some(existing => {
            if (existing.appointmentId === appointment.appointmentId) return false; // Skip self
            if (existing.status === 'CANCELLED' || existing.status === 'DECLINED') return false;

            const existingStart = new Date(existing.fromDateTime);
            const existingEnd = new Date(existing.toDateTime);

            return start < existingEnd && end > existingStart;
        });
    }, [isRescheduleOpen, newDate, newStartTime, newEndTime, existingAppointments, appointment]);

    const handleConfirm = () => {
        if (!appointment) return;
        reviewMutation.mutate({
            id: appointment.appointmentId,
            data: { action: 'CONFIRM' }
        }, {
            onSuccess: () => {
                toast.success(t('appointmentConfirmed'));
                onClose();
            },
            onError: () => toast.error(t('errorActionFailed'))
        });
    };

    const handleDecline = () => {
        if (!refusalReason.trim() || !appointment) return;
        reviewMutation.mutate({
            id: appointment.appointmentId,
            data: { action: 'DECLINE', refusalReason }
        }, {
            onSuccess: () => {
                toast.success(t('appointmentDeclined'));
                setIsDeclineOpen(false);
                setRefusalReason("");
                onClose();
            },
            onError: () => toast.error(t('errorActionFailed'))
        });
    };

    const handleCancel = () => {
        if (!cancellationReason.trim() || !appointment) return;
        cancelMutation.mutate({
            id: appointment.appointmentId,
            data: { reason: cancellationReason }
        }, {
            onSuccess: () => {
                toast.success(t('appointmentCancelledSuccess'));
                setIsCancelOpen(false);
                setCancellationReason("");
                onClose();
            },
            onError: () => toast.error(t('errorActionFailed'))
        });
    }

    const handleReschedule = () => {
        if (!newDate || !newStartTime || !newEndTime || !appointment) return;

        // Simple validation
        const start = new Date(`${newDate}T${newStartTime}`);
        const end = new Date(`${newDate}T${newEndTime}`);

        if (end <= start) {
            // TODO: Toast error? For now rely on button disabled state logic implicitly or add check
            return;
        }

        reviewMutation.mutate({
            id: appointment.appointmentId,
            data: {
                action: 'RESCHEDULE',
                newDate,
                newStartTime,
                newEndTime
            }
        }, {
            onSuccess: () => {
                toast.success(t('appointmentRescheduled'));
                setIsRescheduleOpen(false);
                onClose();
            },
            onError: () => toast.error(t('errorActionFailed'))
        });
    };

    // Helper to prepopulate reschedule fields
    const openReschedule = () => {
        if (!appointment) return;

        const currentStart = new Date(appointment.fromDateTime);
        const currentEnd = new Date(appointment.toDateTime);

        // For "Reviving" a cancelled/declined appointment, strictly prepopulate with OLD times 
        // to allow user to see what it was, or choose new.
        // User requested fields to be filled.
        if (appointment.status === 'CANCELLED' || appointment.status === 'DECLINED') {
            setNewDate(format(currentStart, 'yyyy-MM-dd'));
            setNewStartTime(format(currentStart, 'HH:mm'));
            setNewEndTime(format(currentEnd, 'HH:mm'));

            setIsRescheduleOpen(true);
            setIsDeclineOpen(false);
            setIsCancelOpen(false);
            return;
        }

        // ... existing smart logic for active appointments ...
        // Calculate duration in minutes
        const durationMs = currentEnd.getTime() - currentStart.getTime();

        // Strategy: "Just After" by default
        let newStart = new Date(currentEnd);
        let newEnd = new Date(newStart.getTime() + durationMs);

        // Check if "Just After" exceeds Business Hours (17:00 / 5 PM)
        // Set cutoff to 17:00 of the same day
        const cutoff = new Date(currentStart);
        cutoff.setHours(17, 0, 0, 0);

        if (newEnd > cutoff) {
            // Strategy: "Just Before" if "Just After" is too late
            newStart = new Date(currentStart.getTime() - durationMs);
            newEnd = new Date(currentStart);

            // Check if "Just Before" is too early (before 09:00)
            const openTime = new Date(currentStart);
            openTime.setHours(9, 0, 0, 0);

            if (newStart < openTime) {
                // Determine fallback: Just keep original (user will change it) or reset to defaults?
                // For now, reverting to original start/end to avoid showing invalid times 
                // (though button will be disabled, which is fine)
                newStart = new Date(currentStart);
                newEnd = new Date(currentEnd);
            }
        }

        setNewDate(format(newStart, 'yyyy-MM-dd'));
        setNewStartTime(format(newStart, 'HH:mm'));
        setNewEndTime(format(newEnd, 'HH:mm'));

        setIsRescheduleOpen(true);
        setIsDeclineOpen(false); // Close others
        setIsCancelOpen(false);
    };

    // Status Banner Logic
    const statusBanner = useMemo(() => {
        if (!appointment) return null;
        if (appointment.status === 'PROPOSED') {
            if (canReview) {
                return {
                    variant: 'action',
                    title: t('actionRequired', 'Action Required'),
                    desc: t('actionRequiredDesc', { role: otherPartyLabel.toLowerCase(), defaultValue: 'The other party is waiting for your confirmation.' })
                };
            } else {
                return {
                    variant: 'waiting',
                    title: t('waitingForResponse', 'Waiting for Response'),
                    desc: t('waitingForResponseDesc', 'You have sent a proposal. Waiting for the other party\'s response.')
                };
            }
        }
        if (appointment.status === 'CANCELLED') {
            return {
                variant: 'destructive',
                title: t('cancelled', 'Cancelled'),
                desc: t('appointmentCancelled', 'This appointment has been cancelled.')
            };
        }
        return null; // No special banner for Confirmed/Declined/etc for now
    }, [appointment, canReview, t, otherPartyLabel]);

    if (!appointment) return null;

    // Background color based on who initiated (Sender vs Receiver context)
    // If I initiated (isMe) -> Light Gray/Neutral
    // If They initiated (!isMe) -> Light Primary Tint (very subtle)
    // We determine 'isMe' logic:
    const isMe = (isBroker && appointment.initiatedBy === 'BROKER') || (!isBroker && appointment.initiatedBy === 'CLIENT');
    const bgClass = !isMe ? "bg-orange-50/30 dark:bg-orange-900/10" : "bg-background";
    const isCancelled = appointment.status === 'CANCELLED';

    // Fix: access the correct UUID based on role, because user.sub is Auth0 ID but cancelledBy is internal UUID
    const currentUserUuid = isBroker ? appointment.brokerId : appointment.clientId;
    const canRescheduleCancelled = isCancelled && appointment.cancelledBy === currentUserUuid;



    return (
        <Dialog open={isOpen} onOpenChange={(val) => !val && onClose()}>
            <DialogContent className={`sm:max-w-md max-h-[85vh] overflow-y-auto ${bgClass}`}>
                <DialogHeader>
                    {/* Status Banner */}
                    {statusBanner && (
                        <div className={`mb-4 p-3 rounded-lg border flex items-start gap-3 ${statusBanner.variant === 'action'
                            ? 'bg-primary/10 border-primary/20 text-primary-foreground dark:text-primary'
                            : statusBanner.variant === 'destructive'
                                ? 'bg-destructive/10 border-destructive/20 text-destructive'
                                : 'bg-muted border-border text-muted-foreground'
                            }`}>
                            {statusBanner.variant === 'action' ? (
                                <AlertTriangle className="w-5 h-5 ml-2 mt-0.5 text-primary" />
                            ) : statusBanner.variant === 'destructive' ? (
                                <Ban className="w-5 h-5 ml-2 mt-0.5 text-destructive" />
                            ) : (
                                <Clock className="w-5 h-5 ml-2 mt-0.5" />
                            )}
                            <div>
                                <p className={`font-semibold text-sm ${statusBanner.variant === 'action' ? 'text-primary' : statusBanner.variant === 'destructive' ? 'text-destructive' : ''}`}>
                                    {statusBanner.title}
                                </p>
                                <p className={`text-xs ${statusBanner.variant === 'action' ? 'text-primary/80' : statusBanner.variant === 'destructive' ? 'text-destructive/80' : ''}`}>
                                    {statusBanner.desc}
                                </p>
                            </div>
                        </div>
                    )}

                    <div className="flex items-center gap-3">
                        <div className={`p-2 rounded-lg ${isCancelled ? 'bg-muted' : 'bg-primary/10'}`}>
                            {isCancelled ? (
                                <Ban className="w-6 h-6 text-muted-foreground" />
                            ) : (
                                <Calendar className="w-6 h-6 text-primary" />
                            )}
                        </div>
                        <DialogTitle className={isCancelled ? "text-muted-foreground line-through" : ""}>
                            {t(appointment.title.toLowerCase(), appointment.title)}
                        </DialogTitle>
                    </div>
                </DialogHeader>

                <div className="space-y-6 pt-2">
                    {/* Status Badge */}
                    <div className="flex justify-between items-center">
                        <span className="text-sm text-muted-foreground">{t('statusLabel', 'Status')}</span>
                        <Badge
                            variant={getStatusBadgeVariant(appointment.status)}
                            className={appointment.status === 'CONFIRMED' ? 'bg-green-600 hover:bg-green-700 border-transparent text-white' : ''}
                        >
                            {t(`status.${appointment.status.toLowerCase()}`, appointment.status)}
                        </Badge>
                    </div>

                    {/* Conflict Warning */}
                    {canReview && hasConflict && !isRescheduleOpen && !isCancelled && (
                        <div className="bg-destructive/15 text-destructive border border-destructive/30 rounded-lg p-3 flex items-start gap-3">
                            <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
                            <div className="text-sm">
                                <p className="font-semibold">{t('conflictWarningTitle', 'Scheduling Conflict')}</p>
                                <p>{t('conflictWarningDesc', 'You already have an appointment scheduled at this time.')}</p>
                            </div>
                        </div>
                    )}

                    {/* Date and Time */}
                    <Section className={`p-4 space-y-3 bg-muted/30 ${isCancelled ? 'opacity-60' : ''}`}>
                        <div className="flex items-center gap-3">
                            <Calendar className="w-4 h-4 text-muted-foreground" />
                            <span className="font-medium">
                                {format(getAppointmentDate(appointment), 'EEEE, d MMMM yyyy', { locale: i18n.language === 'fr' ? fr : enUS })}
                            </span>
                        </div>
                        <div className="flex items-center gap-3">
                            <Clock className="w-4 h-4 text-muted-foreground" />
                            <span>{getAppointmentTimeRange(appointment)}</span>
                        </div>
                    </Section>

                    {/* Participants */}
                    <div className={`grid grid-cols-2 gap-4 ${isCancelled ? 'opacity-60' : ''}`}>
                        <div className="space-y-1">
                            <p className="text-xs text-muted-foreground uppercase font-semibold tracking-wider">
                                {otherPartyLabel}
                            </p>
                            <div className="flex items-center gap-2">
                                <User className="w-4 h-4 text-primary" />
                                <span className="font-medium">{otherPartyName}</span>
                            </div>
                        </div>
                        {/* Location */}
                        {appointment.location && (
                            <div className="space-y-1">
                                <p className="text-xs text-muted-foreground uppercase font-semibold tracking-wider">
                                    {t('location', 'Location')}
                                </p>
                                <div className="flex items-center gap-2">
                                    <MapPin className="w-4 h-4 text-primary" />
                                    <span className="font-medium truncate" title={appointment.location}>
                                        {appointment.location}
                                    </span>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Transaction Reference (if any) */}
                    {appointment.transactionId && (
                        <div className="space-y-1 pt-2 border-t border-border/50">
                            <p className="text-xs text-muted-foreground uppercase font-semibold tracking-wider">
                                {t('transaction')}
                            </p>
                            <p className="text-sm text-muted-foreground font-mono">
                                {appointment.transactionId}
                            </p>
                        </div>
                    )}

                    {/* Notes/Message */}
                    {appointment.notes && (
                        <div className="space-y-2 pt-2 border-t border-border/50">
                            <p className="text-xs text-muted-foreground uppercase font-semibold tracking-wider flex items-center gap-2">
                                <FileText className="w-3 h-3" />
                                {t('notes', 'Notes')}
                            </p>
                            <p className="text-sm text-foreground/80 bg-muted p-3 rounded-lg italic">
                                "{appointment.notes}"
                            </p>
                        </div>
                    )}

                    {/* Visitor Count Display/Edit for open_house and private_showing */}
                    {isShowingType && appointment.status === 'CONFIRMED' && (
                        <div className="space-y-2 pt-2 border-t border-border/50">
                            <p className="text-xs text-muted-foreground uppercase font-semibold tracking-wider flex items-center gap-2">
                                <Users className="w-3 h-3" />
                                {t('numberOfVisitors')}
                            </p>
                            {linkedVisitor && (
                                <div className="flex items-center gap-2 text-sm">
                                    <User className="w-3 h-3 text-muted-foreground" />
                                    <span>{linkedVisitor.name}</span>
                                </div>
                            )}
                            {!editingVisitorCount ? (
                                <div className="flex items-center justify-between">
                                    <span className="text-sm font-medium">
                                        {appointment.numberOfVisitors != null ? appointment.numberOfVisitors : '—'}
                                    </span>
                                    {isBroker && eventConcluded && (
                                        <Button
                                            variant="outline"
                                            size="sm"
                                            onClick={() => {
                                                setVisitorCountValue(appointment.numberOfVisitors ?? 0);
                                                setEditingVisitorCount(true);
                                            }}
                                        >
                                            {t('updateVisitorCount')}
                                        </Button>
                                    )}
                                    {isBroker && !eventConcluded && (
                                        <span className="text-xs text-muted-foreground italic">
                                            {t('eventNotConcluded')}
                                        </span>
                                    )}
                                </div>
                            ) : (
                                <div className="flex items-center gap-2">
                                    <Input
                                        type="number"
                                        min={0}
                                        value={visitorCountValue}
                                        onChange={(e) => setVisitorCountValue(parseInt(e.target.value) || 0)}
                                        className="w-24"
                                    />
                                    <Button
                                        size="sm"
                                        onClick={() => {
                                            updateVisitorCountMutation.mutate({
                                                appointmentId: appointment.appointmentId,
                                                numberOfVisitors: visitorCountValue,
                                            }, {
                                                onSuccess: () => setEditingVisitorCount(false),
                                            });
                                        }}
                                        disabled={updateVisitorCountMutation.isPending}
                                    >
                                        <Check className="w-3 h-3" />
                                    </Button>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => setEditingVisitorCount(false)}
                                    >
                                        <X className="w-3 h-3" />
                                    </Button>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Refusal Reason (if Declined) */}
                    {appointment.status === 'DECLINED' && appointment.refusalReason && (
                        <div className="space-y-2 pt-2 border-t border-border/50">
                            <p className="text-xs text-destructive uppercase font-semibold tracking-wider flex items-center gap-2">
                                <AlertTriangle className="w-3 h-3" />
                                {t('refusalReason', 'Refusal Reason')}
                            </p>
                            <p className="text-sm text-destructive bg-destructive/10 p-3 rounded-lg border border-destructive/20">
                                "{appointment.refusalReason}"
                            </p>
                        </div>
                    )}
                    {/* Cancellation Reason (if Cancelled) */}
                    {/* Note: 'cancellationReason' might not be in Type definition yet if frontend type file not updated, but we handle it assuming backend sends it */}
                    {appointment.status === 'CANCELLED' && appointment.cancellationReason && (
                        <div className="space-y-2 pt-2 border-t border-border/50">
                            <p className="text-xs text-destructive uppercase font-semibold tracking-wider flex items-center gap-2">
                                <Ban className="w-3 h-3" />
                                {t('cancellationReason', 'Cancellation Reason')}
                            </p>
                            <p className="text-sm text-destructive bg-destructive/10 p-3 rounded-lg border border-destructive/20">
                                "{appointment.cancellationReason}"
                            </p>
                        </div>
                    )}

                    {/* Review Actions (PROPOSED + NOT ME) */}
                    {canReview && !isDeclineOpen && !isRescheduleOpen && !isCancelOpen && (
                        <div className="pt-4 flex flex-col gap-3">
                            <div className="grid grid-cols-2 gap-3">
                                <Button
                                    className="bg-green-600 hover:bg-green-700"
                                    onClick={handleConfirm}
                                    disabled={hasConflict || reviewMutation.isPending}
                                >
                                    <Check className="w-4 h-4 mr-2" />
                                    {t('confirm', 'Confirm')}
                                </Button>
                                <Button
                                    variant="destructive"
                                    onClick={() => setIsDeclineOpen(true)}
                                    disabled={reviewMutation.isPending}
                                >
                                    <X className="w-4 h-4 mr-2" />
                                    {t('decline', 'Decline')}
                                </Button>
                            </div>
                            <Button variant="outline" className="w-full" onClick={openReschedule}>
                                <CalendarClock className="w-4 h-4 mr-2" />
                                {t('proposeNewTime', 'Propose New Time')}
                            </Button>
                        </div>
                    )}

                    {/* Reschedule Action (CONFIRMED or PROPOSED, not cancelled/declined, not already in reschedule/decline/cancel mode) */}
                    {appointment &&
                        (appointment.status === 'CONFIRMED' || (appointment.status === 'PROPOSED' && appointment.initiatedBy === myRole)) &&
                        !isCancelled && !isDeclineOpen && !isRescheduleOpen && !isCancelOpen && (
                            <div className="pt-4">
                                <Button variant="outline" className="w-full" onClick={openReschedule}>
                                    <CalendarClock className="w-4 h-4 mr-2" />
                                    {t('reschedule', 'Reschedule')}
                                </Button>
                            </div>
                        )}

                    {/* Cancel Action (CONFIRMED or PROPOSED + ME) */}
                    {canCancel && !isCancelOpen && !isRescheduleOpen && !isDeclineOpen && (
                        <div className="pt-4 flex flex-col gap-3">
                            <Button
                                variant="destructive"
                                className="w-full"
                                onClick={() => setIsCancelOpen(true)}
                                disabled={cancelMutation.isPending}
                            >
                                <Ban className="w-4 h-4 mr-2" />
                                {t('cancelAppointment', 'Cancel Appointment')}
                            </Button>

                            {/* Allow rescheduling even if not "cancelled" yet, just a regular reschedule initiated by proper party */}
                            {/* Actually, Reschedule for Confirmed appointments isn't explicitly detailed but implies "Propose New Time" flow again? 
                                 For now, let's keep it simple: Cancel first. 
                                 Or if user wants to existing reschedule? 
                                 The ticket says: "Cancelled appointments can be rescheduled" -> imply after cancellation.
                             */}
                        </div>
                    )}

                    {/* Reschedule Action (Visible for CANCELLED appointments too - BUT only if I cancelled it) */}
                    {(isCancelled || appointment.status === 'DECLINED') && !isRescheduleOpen && (
                        <div className="pt-4">
                            {/* 
                               Check permission:
                               - If status is CANCELLED, only the person who cancelled (cancelledBy) can reschedule.
                               - If status is DECLINED, implied that the person who declined (Reviewer) blocked it? 
                                 Actually for DECLINE, usually the *Proposer* should have the chance to propose again (Reschedule).
                                 But the requirement specifically mentioned "cancelled". 
                                 Let's handle CANCEL: if cancelledBy exists, must match user.sub.
                                 For DECLINED: The *other* party (Initiator) usually fixes it.
                           */}
                            {(() => {
                                // For Declined: If I initiated it, I should be able to reschedule (propose new time).
                                const canRescheduleDeclined = appointment.status === 'DECLINED' && appointment.initiatedBy === myRole;

                                const canShowReschedule = isCancelled ? canRescheduleCancelled : canRescheduleDeclined;

                                if (canShowReschedule) {
                                    return (
                                        <Button variant="outline" className="w-full" onClick={openReschedule}>
                                            <CalendarClock className="w-4 h-4 mr-2" />
                                            {t('reschedule', 'Reschedule')}
                                        </Button>
                                    );
                                } else if (isCancelled) {
                                    // Optional: Show message why they can't?
                                    return <p className="text-xs text-center text-muted-foreground mt-2 italic">{t('onlyCancellerCanReschedule', 'Only the party who cancelled can reschedule.')}</p>;
                                }
                                return null;
                            })()}
                        </div>
                    )}


                    {/* Reschedule Form */}
                    {isRescheduleOpen && (
                        <div className="pt-4 space-y-4 animate-in fade-in slide-in-from-bottom-2">
                            <p className="text-sm font-medium">{t('proposeNewTime', 'Propose New Time')}</p>

                            {/* Visual Comparison: Current Time */}
                            <div className="bg-muted/50 p-2 rounded-md border text-xs flex justify-between items-center text-muted-foreground mb-2">
                                <span>{t('originalTime', 'Original Time')}:</span>
                                <span className="font-mono decoration-dotted line-through opacity-70">
                                    {format(getAppointmentDate(appointment), 'MMM d')} • {getAppointmentTimeRange(appointment)}
                                </span>
                            </div>

                            {/* No Change Warning */}
                            {appointment &&
                                newDate === format(new Date(appointment.fromDateTime), 'yyyy-MM-dd') &&
                                newStartTime === format(new Date(appointment.fromDateTime), 'HH:mm') &&
                                newEndTime === format(new Date(appointment.toDateTime), 'HH:mm') && (
                                    <div className="bg-yellow-500/15 text-yellow-600 dark:text-yellow-500 border border-yellow-500/30 rounded-lg p-3 flex items-start gap-3">
                                        <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
                                        <div className="text-sm">
                                            <p className="font-semibold">{t('noChangeTitle', 'No Changes')}</p>
                                            <p>{t('noChangeDesc', 'The selected time is identical to the current time.')}</p>
                                        </div>
                                    </div>
                                )}

                            {hasNewConflict && (
                                <div className="bg-destructive/15 text-destructive border border-destructive/30 rounded-lg p-3 flex items-start gap-3">
                                    <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
                                    <div className="text-sm">
                                        <p className="font-semibold">{t('conflictWarningTitle', 'Scheduling Conflict')}</p>
                                        <p>{t('conflictWarningDesc', 'The selected time conflicts with another appointment.')}</p>
                                    </div>
                                </div>
                            )}

                            <div className="space-y-4">
                                <div>
                                    <label className="text-sm text-muted-foreground block mb-2">{t('date', 'Date')}</label>
                                    <Input
                                        type="date"
                                        value={newDate}
                                        onChange={(e) => setNewDate(e.target.value)}
                                        min={getLocalDateString()}
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="text-sm text-muted-foreground block mb-2">{t('startTime', 'Start Time')}</label>
                                        <Select
                                            value={newStartTime}
                                            onValueChange={setNewStartTime}
                                        >
                                            <SelectTrigger id="reschedule-start-time" className="w-full">
                                                <SelectValue placeholder={t('selectTime', 'Select time')} />
                                            </SelectTrigger>
                                            <SelectContent>
                                                {getTimeSlots().map((timeSlot) => (
                                                    <SelectItem key={timeSlot} value={timeSlot}>
                                                        {formatTimeDisplay(timeSlot)}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    </div>
                                    <div>
                                        <label className="text-sm text-muted-foreground block mb-2">{t('endTime', 'End Time')}</label>
                                        <Select
                                            value={newEndTime}
                                            onValueChange={setNewEndTime}
                                            disabled={!newStartTime}
                                        >
                                            <SelectTrigger id="reschedule-end-time" className="w-full">
                                                <SelectValue placeholder={t('selectTime', 'Select time')} />
                                            </SelectTrigger>
                                            <SelectContent>
                                                {getTimeSlots().map((timeSlot) => {
                                                    if (!newStartTime) return null;
                                                    if (timeSlot <= newStartTime) return null;
                                                    return (
                                                        <SelectItem key={timeSlot} value={timeSlot}>
                                                            {formatTimeDisplay(timeSlot)}
                                                        </SelectItem>
                                                    );
                                                })}
                                            </SelectContent>
                                        </Select>
                                    </div>
                                </div>
                            </div>

                            <div className="flex gap-2 justify-end pt-2">
                                <Button variant="ghost" onClick={() => setIsRescheduleOpen(false)}>{t('cancel', 'Cancel')}</Button>
                                <Button
                                    onClick={handleReschedule}
                                    disabled={
                                        !newDate ||
                                        !newStartTime ||
                                        !newEndTime ||
                                        hasNewConflict ||
                                        reviewMutation.isPending ||
                                        (newStartTime >= newEndTime) ||
                                        (appointment &&
                                            newDate === format(new Date(appointment.fromDateTime), 'yyyy-MM-dd') &&
                                            newStartTime === format(new Date(appointment.fromDateTime), 'HH:mm') &&
                                            newEndTime === format(new Date(appointment.toDateTime), 'HH:mm'))
                                    }
                                >
                                    {t('submit', 'Submit')}
                                </Button>
                            </div>
                        </div>
                    )}

                    {/* Decline Input */}
                    {isDeclineOpen && (
                        <div className="pt-4 space-y-3 animate-in fade-in slide-in-from-bottom-2">
                            <p className="text-sm font-medium">{t('reasonForDeclining', 'Reason for declining')}</p>
                            <Textarea
                                value={refusalReason}
                                onChange={(e) => setRefusalReason(e.target.value)}
                                placeholder={t('enterReason', 'Enter a reason...')}
                                className="resize-none"
                            />
                            <div className="flex gap-2 justify-end">
                                <Button variant="ghost" onClick={() => setIsDeclineOpen(false)}>{t('cancel', 'Cancel')}</Button>
                                <Button
                                    variant="destructive"
                                    onClick={handleDecline}
                                    disabled={!refusalReason.trim() || reviewMutation.isPending}
                                >
                                    {t('decline', 'Decline')}
                                </Button>
                            </div>
                        </div>
                    )}

                    {/* Cancellation Input */}
                    {isCancelOpen && (
                        <div className="pt-4 space-y-3 animate-in fade-in slide-in-from-bottom-2">
                            <div className="bg-destructive/10 p-3 rounded-lg border border-destructive/20 mb-2">
                                <p className="text-xs text-destructive flex gap-2">
                                    <AlertTriangle className="w-4 h-4" />
                                    {t('cancelWarning', 'This action cannot be undone. Typical reasons include emergencies or scheduling conflicts.')}
                                </p>
                            </div>
                            <p className="text-sm font-medium">{t('reasonForCancelling', 'Reason for cancelling')}</p>
                            <Textarea
                                value={cancellationReason}
                                onChange={(e) => setCancellationReason(e.target.value)}
                                placeholder={t('enterCancellationReason', 'Enter a reason (e.g., Emergency, Illness)...')}
                                className="resize-none"
                            />
                            <div className="flex gap-2 justify-end">
                                <Button variant="ghost" onClick={() => setIsCancelOpen(false)}>{t('close', 'Close')}</Button>
                                <Button
                                    variant="destructive"
                                    onClick={handleCancel}
                                    disabled={!cancellationReason.trim() || cancelMutation.isPending}
                                >
                                    {t('confirmCancellation', 'Confirm Cancellation')}
                                </Button>
                            </div>
                        </div>
                    )}

                </div>
            </DialogContent>


        </Dialog>
    );
}
