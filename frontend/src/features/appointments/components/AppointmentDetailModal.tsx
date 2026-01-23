import { useState, useMemo } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Textarea } from "@/shared/components/ui/textarea";
import { useTranslation } from "react-i18next";
import { type Appointment, getAppointmentTimeRange, getAppointmentDate } from "../types";
import { format } from "date-fns";
import { fr, enUS } from 'date-fns/locale';
import { Calendar, Clock, MapPin, User, FileText, Check, X, AlertTriangle, CalendarClock } from "lucide-react";
import { useReviewAppointment } from "../api/mutations";
import { Badge } from "@/shared/components/ui/badge";
import { getStatusBadgeVariant } from "../enums";
import { useAuth0 } from "@auth0/auth0-react";
import { Section } from "@/shared/components/branded/Section";
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
    const [isRescheduleOpen, setIsRescheduleOpen] = useState(false);
    const [refusalReason, setRefusalReason] = useState("");

    // Reschedule state
    const [newDate, setNewDate] = useState("");
    const [newStartTime, setNewStartTime] = useState("");
    const [newEndTime, setNewEndTime] = useState("");

    const reviewMutation = useReviewAppointment();

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
    const canReview = appointment ? appointment.status === 'PROPOSED' && appointment.initiatedBy !== myRole : false;

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
            onSuccess: () => onClose()
        });
    };

    const handleDecline = () => {
        if (!refusalReason.trim() || !appointment) return;
        reviewMutation.mutate({
            id: appointment.appointmentId,
            data: { action: 'DECLINE', refusalReason }
        }, {
            onSuccess: () => {
                setIsDeclineOpen(false);
                setRefusalReason("");
                onClose();
            }
        });
    };

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
                setIsRescheduleOpen(false);
                onClose();
            }
        });
    };

    // Helper to prepopulate reschedule fields
    const openReschedule = () => {
        if (!appointment) return;

        const currentStart = new Date(appointment.fromDateTime);
        const currentEnd = new Date(appointment.toDateTime);

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
        return null; // No special banner for Confirmed/Declined/etc for now
    }, [appointment, canReview, t]);

    if (!appointment) return null;

    // Background color based on who initiated (Sender vs Receiver context)
    // If I initiated (isMe) -> Light Gray/Neutral
    // If They initiated (!isMe) -> Light Primary Tint (very subtle)
    // We determine 'isMe' logic:
    const isMe = (isBroker && appointment.initiatedBy === 'BROKER') || (!isBroker && appointment.initiatedBy === 'CLIENT');
    const bgClass = !isMe ? "bg-orange-50/30 dark:bg-orange-900/10" : "bg-background";

    return (
        <Dialog open={isOpen} onOpenChange={(val) => !val && onClose()}>
            <DialogContent className={`sm:max-w-md max-h-[85vh] overflow-y-auto ${bgClass}`}>
                <DialogHeader>
                    {/* Status Banner */}
                    {statusBanner && (
                        <div className={`mb-4 p-3 rounded-lg border flex items-start gap-3 ${statusBanner.variant === 'action'
                            ? 'bg-primary/10 border-primary/20 text-primary-foreground dark:text-primary'
                            : 'bg-muted border-border text-muted-foreground'
                            }`}>
                            {statusBanner.variant === 'action' ? (
                                <AlertTriangle className="w-5 h-5 ml-2 mt-0.5 text-primary" />
                            ) : (
                                <Clock className="w-5 h-5 ml-2 mt-0.5" />
                            )}
                            <div>
                                <p className={`font-semibold text-sm ${statusBanner.variant === 'action' ? 'text-primary' : ''}`}>
                                    {statusBanner.title}
                                </p>
                                <p className={`text-xs ${statusBanner.variant === 'action' ? 'text-primary/80' : ''}`}>
                                    {statusBanner.desc}
                                </p>
                            </div>
                        </div>
                    )}

                    <div className="flex items-center gap-3">
                        <div className="p-2 rounded-lg bg-primary/10">
                            <Calendar className="w-6 h-6 text-primary" />
                        </div>
                        <DialogTitle>{t(appointment.title.toLowerCase(), appointment.title)}</DialogTitle>
                    </div>
                </DialogHeader>

                <div className="space-y-6 pt-2">
                    {/* Status Badge */}
                    <div className="flex justify-between items-center">
                        <span className="text-sm text-muted-foreground">{t('statusLabel', 'Status')}</span>
                        <Badge variant={getStatusBadgeVariant(appointment.status)}>
                            {t(`status.${appointment.status.toLowerCase()}`, appointment.status)}
                        </Badge>
                    </div>

                    {/* Conflict Warning */}
                    {canReview && hasConflict && !isRescheduleOpen && (
                        <div className="bg-destructive/15 text-destructive border border-destructive/30 rounded-lg p-3 flex items-start gap-3">
                            <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
                            <div className="text-sm">
                                <p className="font-semibold">{t('conflictWarningTitle', 'Scheduling Conflict')}</p>
                                <p>{t('conflictWarningDesc', 'You already have an appointment scheduled at this time.')}</p>
                            </div>
                        </div>
                    )}

                    {/* Date and Time */}
                    <Section className="p-4 space-y-3 bg-muted/30">
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
                    <div className="grid grid-cols-2 gap-4">
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

                    {/* Review Actions */}
                    {canReview && !isDeclineOpen && !isRescheduleOpen && (
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

                    {/* Reschedule Form */}
                    {isRescheduleOpen && (
                        <div className="pt-4 space-y-4 animate-in fade-in slide-in-from-bottom-2">
                            <p className="text-sm font-medium">{t('proposeNewTime', 'Propose New Time')}</p>

                            {/* Visual Comparison: Current Time */}
                            <div className="bg-muted/50 p-2 rounded-md border text-xs flex justify-between items-center text-muted-foreground mb-2">
                                <span>{t('currentTime', 'Current Time')}:</span>
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
                                        min={new Date().toISOString().split('T')[0]}
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
                                        // Disable if end time is before or equal to start time
                                        (newStartTime >= newEndTime) ||
                                        // Disable if the time hasn't changed
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

                </div>
            </DialogContent>
        </Dialog>
    );
}
