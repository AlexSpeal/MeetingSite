export interface User {
    id: number;
    username: string;
    vkUserId?: number | null;
    dailyLoadMinutes?: number | null;
}

export interface EventParticipant {
    id: number;
    eventId: number;
    userId: number;
    user: User;
    status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'INABILITY';
    selectedDays: string[];
    required?: boolean;
}

export interface Event {
    id: number;
    title: string;
    description: string;
    authorId: number;
    possibleDays: string[];
    participants: EventParticipant[];
    isPersonal: boolean;
    isFixed?: boolean;
    preferredWindowStart?: string | null;  // "HH:mm" local time
    preferredWindowEnd?: string | null;
    startTime?: string;
    duration: number;
    status: 'PENDING' | 'ACCEPTED';
    createdAt: string;
}

export type SortOption = 'DATE' | 'TITLE' | 'STATUS';

export enum WebSocketAction {
    CREATE = 'CREATE',
    UPDATE = 'UPDATE',
    SCHEDULE = 'SCHEDULE',
    DELETE = 'DELETE',
}

export interface WebSocketMessage {
    action: WebSocketAction;
    meetingId: number;
    data: Event | null;
}

export interface AvailabilityResponse {
    meetingId: number;
    maxCount: number;
    possibleIntervals: {
        start: string;  // ISO 8601 UTC, e.g. "2026-05-05T06:00:00Z"
        end: string;
    }[];
    havePending: boolean;
}

export interface CreatingEventParticipantRequest {
    userId: number;
    required: boolean;
}

export interface CreatingEventRequest {
    title: string;
    description: string;
    possibleDays: string[];
    participants?: CreatingEventParticipantRequest[];
    duration: number;
    isFixed?: boolean;
    preferredWindowStart?: string | null;  // "HH:mm" local time, optional
    preferredWindowEnd?: string | null;
}

export interface UpdateDailyLoadRequest {
    dailyLoadMinutes: number | null;
}

export interface AcceptEventRequest {
    status: 'ACCEPTED' | 'DECLINED';
    selectedDays?: string[];
}

export interface ScheduleRequest {
    startTime: string;
}

export interface StartVkBindingRequest {
    screenName: string;
}

export interface ConfirmVkBindingRequest {
    code: string;
}