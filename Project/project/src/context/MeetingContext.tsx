import React, {createContext, ReactNode, useCallback, useEffect, useState} from 'react';
import {Client} from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  AcceptEventRequest,
  AvailabilityResponse,
  CreatingEventRequest,
  Event,
  ScheduleRequest,
  SortOption,
  User,
  WebSocketAction,
  WebSocketMessage
} from '../types';
import {isAuthenticated} from '../utils/authUtils';

interface MeetingContextType {
    meetings: Event[];
    currentUser: User | undefined;
    isLoading: boolean;
    isUserLoading: boolean;
    addMeeting: (meeting: CreatingEventRequest) => Promise<Event>;
    getUserById: (id: number) => Promise<User | undefined>;
    getUserByUsername: (username: string) => Promise<User | undefined>;
    deleteMeeting: (meetingId: string | number) => Promise<void>;
    respondToMeeting: (meetingId: number, request: AcceptEventRequest) => Promise<void>;
    confirmMeeting: (meetingId: number, request: ScheduleRequest) => Promise<void>;
    sortMeetings: (option: SortOption) => void;
    getUserMeetings: () => Promise<Event[]>;
    getBestPossibleDates: (meetingId: number) => Promise<AvailabilityResponse>;
    isAuthenticated: () => Promise<boolean>;
    login: (token: string) => void;
    logout: () => void;
}

const MeetingContext = createContext<MeetingContextType | undefined>(undefined);

const API_BASE_URL = '/secured/meetings';
const API_BASE_USERS_URL = '/secured/users';

export const MeetingProvider: React.FC<{ children: ReactNode }> = ({children}) => {
    console.log('MeetingProvider mounted');
    const [meetings, setMeetings] = useState<Event[]>([]);
    const [currentUser, setCurrentUser] = useState<User | undefined>(undefined);
    const [isLoading, setIsLoading] = useState(false);
    const [isUserLoading, setIsUserLoading] = useState(true);
    const [stompClient, setStompClient] = useState<Client | null>(null);
    const [userSubscriptionId, setUserSubscriptionId] = useState<string | null>(null);
    const [deletingMeetings, setDeletingMeetings] = useState<Set<string | number>>(new Set());

    const getAuthHeaders = useCallback(() => {
        const token = localStorage.getItem('token');
        return {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
        };
    }, []);

    const handleResponse = useCallback(async (response: Response) => {
        if (!response.ok) {
            let errorMessage = 'Произошла ошибка';
            try {
                const error = await response.json();
                errorMessage = error.message || errorMessage;
            } catch (e) {
                console.warn('No JSON in error response or empty body');
            }
            throw new Error(errorMessage);
        }
        if (response.status === 204) {
            return null;
        }
        try {
            const text = await response.text();
            if (!text) {
                return null;
            }
            return JSON.parse(text);
        } catch (e) {
            console.error('Failed to parse JSON:', e);
            throw new SyntaxError('Unexpected end of JSON input');
        }
    }, []);

    const getCurrentUser = useCallback(async (): Promise<User | undefined> => {
        try {
            const response = await fetch('/secured/user', {headers: getAuthHeaders()});
            const user = await handleResponse(response);
            return user;
        } catch (error) {
            console.error('Ошибка при загрузке пользователя:', error);
            return undefined;
        }
    }, [getAuthHeaders, handleResponse]);

    const handleMeetingUpdate = useCallback((msg: WebSocketMessage) => {
        if (msg.action === WebSocketAction.CREATE && msg.data && 'id' in msg.data && typeof msg.data.id === 'number') {
            setMeetings((prev) => {
                if (prev.some((m) => m.id === msg.data!.id)) {
                    return prev;
                }
                return [...prev, msg.data as Event];
            });
        } else if (
            (msg.action === WebSocketAction.UPDATE || msg.action === WebSocketAction.SCHEDULE) &&
            msg.data &&
            'id' in msg.data &&
            typeof msg.data.id === 'number'
        ) {
            setMeetings((prev) => {
                const updatedMeeting = msg.data as Event;
                const isPersonal = updatedMeeting.isPersonal || (updatedMeeting.participants?.length === 1 && updatedMeeting.authorId === updatedMeeting.participants[0].id);
                const hasOnlyAuthor = updatedMeeting.participants?.length === 1 && !isPersonal;
                if (hasOnlyAuthor) {
                    return prev.filter((m) => m.id !== msg.meetingId);
                }
                if (prev.some((m) => m.id === msg.meetingId)) {
                    return prev.map((m) => (m.id === msg.meetingId ? {...m, ...updatedMeeting} : m));
                } else {
                    return [...prev, updatedMeeting];
                }
            });
        } else if (msg.action === WebSocketAction.DELETE) {
            setMeetings((prev) => {
                return prev.filter((m) => m.id !== msg.meetingId);
            });
            setDeletingMeetings((prev) => {
                const newSet = new Set(prev);
                newSet.delete(msg.meetingId);
                return newSet;
            });
        } else {
            console.warn('Unknown or invalid meeting update:', msg);
        }
    }, []);

    // Инициализация WebSocket
    useEffect(() => {
        console.log('Initializing WebSocket');
        const client = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8189/ws'),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            debug: (str) => console.log('STOMP debug:', str),
        });

        client.onConnect = () => {
            setStompClient(client);
        };

        client.onStompError = (frame) => {
            console.error('STOMP error:', frame);
            setStompClient(null);
            setUserSubscriptionId(null);
        };

        client.onDisconnect = () => {
            setStompClient(null);
            setUserSubscriptionId(null);
        };

        client.activate();

        return () => {
            if (client.active) {
                client.deactivate();
            }
            setStompClient(null);
            setUserSubscriptionId(null);
            setDeletingMeetings(new Set());
        };
    }, []);

    useEffect(() => {
        if (!stompClient || !currentUser || !stompClient.active) {
            return;
        }

        const token = localStorage.getItem('token');
        if (token) {
            stompClient.configure({
                connectHeaders: {Authorization: `Bearer ${token}`},
            });
        }

        if (!userSubscriptionId) {
            const userChannel = `/user/${currentUser.id}/queue/updates`;
            const subscription = stompClient.subscribe(userChannel, (message) => {
                try {
                    const msg: WebSocketMessage = JSON.parse(message.body);
                    handleMeetingUpdate(msg);
                } catch (error) {
                    console.error('Error parsing message:', error);
                }
            });
            setUserSubscriptionId(subscription.id);
        }
    }, [currentUser, stompClient, userSubscriptionId, handleMeetingUpdate]);

    const getUserMeetings = useCallback(async (): Promise<Event[]> => {
        try {
            setIsLoading(true);
            const response = await fetch(`${API_BASE_USERS_URL}/meetings`, {
                headers: getAuthHeaders(),
            });
            const data = await handleResponse(response);
            if (data && typeof data === 'object' && Array.isArray(data.eventDtoList)) {
                return data.eventDtoList;
            }
            console.error('Неверный формат данных встреч, ожидался объект с eventDtoList:', data);
            return [];
        } catch (error) {
            console.error('Ошибка при получении встреч пользователя:', error);
            return [];
        } finally {
            setIsLoading(false);
        }
    }, [getAuthHeaders, handleResponse]);

    useEffect(() => {
        const fetchCurrentUser = async () => {
            setIsUserLoading(true);
            try {
                const authStatus = await isAuthenticated();
                if (authStatus) {
                    const user = await getCurrentUser();
                    setCurrentUser(user);
                    if (user) {
                        const userMeetings = await getUserMeetings();
                        setMeetings(userMeetings);
                    }
                } else {
                    setCurrentUser(undefined);
                }
            } catch (error) {
                console.error('Ошибка при загрузке пользователя:', error);
                setCurrentUser(undefined);
            } finally {
                setIsUserLoading(false);
            }
        };
        fetchCurrentUser();
    }, [getCurrentUser, getUserMeetings]);

    const login = useCallback(async (token: string) => {
        localStorage.setItem('token', token);
        try {
            setIsLoading(true);
            const user = await getCurrentUser();
            setCurrentUser(user);
            if (user) {
                const userMeetings = await getUserMeetings();
                setMeetings(userMeetings);
            }
        } catch (error) {
            console.error('Ошибка при логине:', error);
            throw error;
        } finally {
            setIsLoading(false);
        }
    }, [getCurrentUser, getUserMeetings]);

    const logout = useCallback(() => {
        localStorage.removeItem('token');
        setCurrentUser(undefined);
        setMeetings([]);
        setUserSubscriptionId(null);
        setDeletingMeetings(new Set());
        if (stompClient?.active) {
            stompClient.deactivate();
            setStompClient(null);
        }
    }, [stompClient]);

    const addMeeting = useCallback(async (meetingData: CreatingEventRequest): Promise<Event> => {
        try {
            setIsLoading(true);
            const response = await fetch(API_BASE_URL, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify(meetingData),
            });
            const newMeeting = await handleResponse(response);

            setMeetings((prev) => {
                if (prev.some((m) => m.id === newMeeting.id)) {
                    return prev;
                }
                return [...prev, newMeeting];
            });
            return newMeeting;
        } catch (error) {
            console.error('Ошибка при создании встречи:', error);
            throw error;
        } finally {
            setIsLoading(false);
        }
    }, [getAuthHeaders, handleResponse]);

    const getUserById = useCallback(async (id: number): Promise<User | undefined> => {
        try {
            const response = await fetch(`${API_BASE_USERS_URL}/${id}`, {headers: getAuthHeaders()});
            return handleResponse(response);
        } catch (error) {
            console.error('Ошибка при загрузке пользователя:', error);
            return undefined;
        }
    }, [getAuthHeaders, handleResponse]);

    const getUserByUsername = useCallback(async (username: string): Promise<User | undefined> => {
        try {
            const response = await fetch(`${API_BASE_USERS_URL}?username=${encodeURIComponent(username)}`, {
                headers: getAuthHeaders(),
            });
            return handleResponse(response);
        } catch (error) {
            console.error('Ошибка при загрузке пользователя:', error);
            return undefined;
        }
    }, [getAuthHeaders, handleResponse]);

    const deleteMeeting = useCallback(async (meetingId: string | number): Promise<void> => {
        if (deletingMeetings.has(meetingId)) {
            return;
        }
        try {
            setIsLoading(true);
            setDeletingMeetings((prev) => new Set(prev).add(meetingId));
            const response = await fetch(`${API_BASE_URL}/${meetingId}`, {
                method: 'DELETE',
                headers: getAuthHeaders(),
            });
            await handleResponse(response);
        } catch (error) {
            console.error('Ошибка при удалении встречи:', error);
            throw error;
        } finally {
            setIsLoading(false);
            setDeletingMeetings((prev) => {
                const newSet = new Set(prev);
                newSet.delete(meetingId);
                return newSet;
            });
        }
    }, [getAuthHeaders, handleResponse]);

    const respondToMeeting = useCallback(async (meetingId: number, request: AcceptEventRequest): Promise<void> => {
        try {
            setIsLoading(true);
            const response = await fetch(`${API_BASE_URL}/${meetingId}/selectDays`, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify(request),
            });
            await handleResponse(response);
        } catch (error) {
            console.error('Ошибка при ответе на встречу:', error);
            throw error;
        } finally {
            setIsLoading(false);
        }
    }, [getAuthHeaders, handleResponse]);

    const confirmMeeting = useCallback(async (meetingId: number, request: ScheduleRequest): Promise<void> => {
        try {
            setIsLoading(true);
            const response = await fetch(`${API_BASE_URL}/${meetingId}/schedule`, {
                method: 'PUT',
                headers: getAuthHeaders(),
                body: JSON.stringify(request),
            });
            await handleResponse(response);
        } catch (error) {
            console.error('Ошибка при подтверждении встречи:', error);
            throw error;
        } finally {
            setIsLoading(false);
        }
    }, [getAuthHeaders, handleResponse]);

    const sortMeetings = useCallback((option: SortOption) => {
        setMeetings((prev) => {
            const sorted = [...prev];
            switch (option) {
                case 'DATE':
                    return sorted.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
                case 'TITLE':
                    return sorted.sort((a, b) => a.title.localeCompare(b.title));
                case 'STATUS':
                    return sorted.sort((a, b) => a.status.localeCompare(b.status));
                default:
                    return sorted;
            }
        });
    }, []);


    const getBestPossibleDates = useCallback(async (meetingId: number): Promise<AvailabilityResponse> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${meetingId}/availability`, {
                method: 'GET',
                headers: getAuthHeaders(),
            });
            return await handleResponse(response);
        } catch (error: any) {
            console.error('Ошибка при получении лучших дат:', error);
            throw new Error(error.message || 'Failed to fetch best possible dates');
        }
    }, [getAuthHeaders, handleResponse]);

    return (
        <MeetingContext.Provider
            value={{
                meetings,
                currentUser,
                isLoading,
                isUserLoading,
                addMeeting,
                getUserById,
                getUserByUsername,
                deleteMeeting,
                respondToMeeting,
                confirmMeeting,
                sortMeetings,
                getUserMeetings,
                getBestPossibleDates,
                isAuthenticated,
                login,
                logout,
            }}
        >
            {children}
        </MeetingContext.Provider>
    );
};

export const MeetingProviderMemo = React.memo(MeetingProvider);

export const useMeetingContext = () => {
    const context = React.useContext(MeetingContext);
    if (context === undefined) {
        throw new Error('useMeetingContext должен использоваться внутри MeetingProvider');
    }
    return context;
};