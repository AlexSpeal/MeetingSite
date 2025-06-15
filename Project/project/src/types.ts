export interface User {
    id: number;
    username: string;
}

export interface EventParticipant {
    id: number;
    eventId: number;
    userId: number;
    user: User;
    status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | "INABILITY";
    selectedDays: string[];
}

export interface Event {
    id: number;
    title: string;
    description: string;
    authorId: number;
    possibleDays: string[];
    participants: EventParticipant[];
    isPersonal: boolean;
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
        date: string;
        start: string;
        end: string;
    }[],
    havePending: boolean;
}

export interface CreatingEventRequest {
    title: string;
    description: string;
    possibleDays: string[];
    participants?: number[];
    duration: number;
}

export interface AcceptEventRequest {
    status: "ACCEPTED" | "DECLINED";
    selectedDays?: string[];
}

export interface ScheduleRequest {
    startTime: string;
}