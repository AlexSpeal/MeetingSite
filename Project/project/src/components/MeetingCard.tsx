import React, {useCallback, useEffect, useState} from 'react';
import {AlertCircle, Calendar, Check, ChevronDown, ChevronUp, Clock, Tag, Trash2, Users, X,} from 'lucide-react';
import {AcceptEventRequest, Event, User} from '../types';
import {useMeetingContext} from '../context/MeetingContext';
import {addMinutes, format, isAfter, isBefore, parseISO, startOfDay} from 'date-fns';
import MeetingConfirmation from './MeetingConfirmation';
import LoadingScreen from './LoadingScreen';

interface MeetingCardProps {
    meeting: Event;
    onDelete?: (meetingId: number) => void;
}

const MeetingCard: React.FC<MeetingCardProps> = ({meeting, onDelete}) => {
    const {getUserById, currentUser, respondToMeeting, deleteMeeting, isLoading} =
        useMeetingContext();
    const [organizer, setOrganizer] = useState<User | undefined>();
    const [isOrganizerLoading, setIsOrganizerLoading] = useState(true);
    const [expanded, setExpanded] = useState(false);
    const [showConfirmation, setShowConfirmation] = useState(false);
    const [selectedDates, setSelectedDates] = useState<string[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [isResponding, setIsResponding] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [isDeleted, setIsDeleted] = useState(false);

    useEffect(() => {
        const fetchData = async () => {
            if (!currentUser) {
                setError('Пользователь не аутентифицирован');
                setIsOrganizerLoading(false);
                return;
            }
            try {
                setIsOrganizerLoading(true);
                const user = await getUserById(meeting.authorId);
                setOrganizer(user);
            } catch {
                setError('Не удалось загрузить данные организатора');
            } finally {
                setIsOrganizerLoading(false);
            }
        };
        if (!isLoading) {
            fetchData();
        }
    }, [isLoading, getUserById, meeting.authorId, currentUser]);

    useEffect(() => {
        if (meeting.status === 'ACCEPTED') {
            setShowConfirmation(false);
        }
    }, [meeting.status, meeting.id]);

    const clearError = () => setError(null);

    const handleToggleDate = (date: string) => {
        setSelectedDates(prev =>
            prev.includes(date) ? prev.filter(d => d !== date) : [...prev, date]
        );
        clearError();
    };

    const handleAccept = async () => {
        if (selectedDates.length === 0 && isPending && !isPersonal) {
            setError('Пожалуйста, выберите хотя бы одну дату, которая вам подходит');
            return;
        }
        setIsResponding(true);
        try {
            const request: AcceptEventRequest = {
                status: 'ACCEPTED',
                selectedDays: selectedDates,
            };
            await respondToMeeting(meeting.id, request);
            setSelectedDates([]);
            clearError();
        } catch (err: any) {
            setError(err.message || 'Ошибка при принятии встречи');
        } finally {
            setIsResponding(false);
        }
    };

    const handleDecline = async () => {
        setIsResponding(true);
        try {
            await respondToMeeting(meeting.id, {status: 'DECLINED', selectedDays: []});
            clearError();
        } catch (err: any) {
            setError(err.message || 'Ошибка при отклонении встречи');
        } finally {
            setIsResponding(false);
        }
    };

    const handleDelete = async () => {
        if (isDeleted) return;
        setIsDeleting(true);
        try {
            await deleteMeeting(meeting.id);
            setIsDeleted(true);
            clearError();
        } catch (err: any) {
            setError(err.message || 'Ошибка при удалении встречи');
        } finally {
            setIsDeleting(false);
        }
    };

    const handleConfirmClick = useCallback(() => {
        if (meeting.status !== 'ACCEPTED') {
            setShowConfirmation(true);
        }
    }, [meeting.status]);

    const handleCloseConfirmation = useCallback(() => {
        setShowConfirmation(false);
    }, []);

    if (isLoading) return <LoadingScreen message="Загрузка карточки встречи..."/>;

    if (!currentUser) {
        return (
            <div className="relative bg-white rounded-lg shadow-md p-5">
                <div
                    className="absolute top-2 right-2 text-red-600 text-xs bg-red-100 px-2 py-1 rounded flex items-center">
                    <AlertCircle size={14} className="mr-1"/>
                    Пользователь не аутентифицирован
                </div>
                <div className="mt-10">
                    <h3 className="text-base font-bold text-gray-800 mb-2">{meeting.title}</h3>
                </div>
            </div>
        );
    }

    const isOrganizer = currentUser.id === meeting.authorId;
    const currentParticipant = meeting.participants.find(p => p.user.id === currentUser.id);
    const isPending = meeting.status === 'PENDING';
    const today = startOfDay(new Date());
    const isPersonal = meeting.isPersonal;

    const getStatusBadge = (status: string) => {
        const base = 'px-2 py-1 rounded text-xs';
        switch (status) {
            case 'ACCEPTED':
                return <span className={`${base} bg-green-100 text-green-800`}>Принято</span>;
            case 'DECLINED':
                return <span className={`${base} bg-red-100 text-red-800`}>Отклонено</span>;
            case 'INABILITY':
                return <span className={`${base} bg-red-200 text-red-900`}>Время не подходит</span>;
            default:
                return <span className={`${base} bg-yellow-100 text-yellow-800`}>Ожидание</span>;
        }
    };

    const getStatusColor = () => {
        if (meeting.status === 'ACCEPTED' && meeting.startTime) {
            const meetingDate = parseISO(meeting.startTime);
            const meetingEnd = addMinutes(meetingDate, meeting.duration ?? 0);
            const now = new Date();
            if (isBefore(now, meetingDate)) {
                return 'bg-blue-100 text-blue-800';
            } else if (isAfter(now, meetingEnd)) {
                return 'bg-gray-100 text-gray-500';
            } else {
                return 'bg-green-100 text-green-800';
            }
        }
        return 'bg-yellow-100 text-yellow-800';
    };

    const getStatusText = () => {
        if (meeting.status === 'ACCEPTED' && meeting.startTime) {
            const meetingDate = parseISO(meeting.startTime);
            const meetingEnd = addMinutes(meetingDate, meeting.duration ?? 0);
            const now = new Date();
            if (isBefore(now, meetingDate)) {
                return 'Предстоящая';
            } else if (isAfter(now, meetingEnd)) {
                return 'Прошедшая';
            } else {
                return 'В процессе';
            }
        }
        return 'Ожидание';
    };

    if (isDeleted) return null;

    return (
        <div className="relative bg-white rounded-lg shadow-md overflow-hidden mb-6">
            {currentParticipant?.status === 'INABILITY' && (
                <div
                    className="absolute top-12 right-4 z-10 bg-red-100 text-red-800 text-[10px] rounded px-2 py-[1px] flex items-center shadow-sm">
                    <AlertCircle size={12} className="mr-1"/>
                    Время не подходит
                </div>
            )}
            <div className="p-5">
                {error && (
                    <div className="mb-4 text-red-600 text-sm bg-red-100 px-3 py-2 rounded flex items-center">
                        <AlertCircle size={16} className="mr-2"/>
                        {error}
                        <button onClick={clearError} className="ml-2 text-red-800 hover:text-red-900">
                            <X size={14}/>
                        </button>
                    </div>
                )}
                <div className="flex justify-between items-start">
                    <div className="flex items-center">
                        <h3 className="text-base font-bold text-gray-800 mb-2">{meeting.title}</h3>
                        {isPersonal && (
                            <span
                                className="ml-2 bg-purple-100 text-purple-800 px-2 py-1 rounded text-xs flex items-center">
                <Tag size={12} className="mr-1"/>
                Личное
              </span>
                        )}
                    </div>
                    <span className={`px-2 py-1 rounded text-xs ${getStatusColor()}`}>
            {getStatusText()}
          </span>
                </div>
                <p className="text-gray-600 mb-4 text-sm">
                    {meeting.description || 'Описание отсутствует'}
                </p>
                <div className="flex items-center text-gray-500 mb-3">
                    <Clock size={16} className="mr-2"/>
                    <span className="text-sm">
            Создано {format(parseISO(meeting.createdAt), 'dd.MM.yyyy')}
          </span>
                </div>
                <div className="flex items-center text-gray-500 mb-3">
                    <Calendar size={16} className="mr-2"/>
                    <div className="flex flex-col sm:flex-row sm:items-center sm:space-x-2">
                        {meeting.startTime ? (
                            <span className="text-sm bg-green-100 text-green-800 px-2 py-1 rounded">
                {format(parseISO(meeting.startTime), 'dd.MM.yyyy в HH:mm')}
              </span>
                        ) : (
                            <div>
                                <span className="text-sm">Возможные даты:</span>
                                <div className="flex flex-wrap gap-1 mt-1">
                                    {meeting.possibleDays
                                        .slice(0, expanded ? undefined : 2)
                                        .map(date => (
                                            <div
                                                key={date}
                                                className={`text-xs px-2 py-1 rounded flex items-center ${
                                                    selectedDates.includes(date)
                                                        ? 'bg-blue-500 text-white'
                                                        : 'bg-blue-100 text-blue-800'
                                                } ${
                                                    isPending &&
                                                    !isOrganizer &&
                                                    !isPersonal &&
                                                    currentParticipant?.status === 'PENDING'
                                                        ? 'cursor-pointer'
                                                        : ''
                                                }`}
                                                onClick={() => {
                                                    if (
                                                        isPending &&
                                                        !isOrganizer &&
                                                        !isPersonal &&
                                                        currentParticipant?.status === 'PENDING'
                                                    ) {
                                                        handleToggleDate(date);
                                                    }
                                                }}
                                            >
                                                {format(parseISO(date), 'dd.MM.yyyy')}
                                                {selectedDates.includes(date) && (
                                                    <Check size={12} className="ml-1"/>
                                                )}
                                            </div>
                                        ))}
                                    {!expanded && meeting.possibleDays.length > 2 && (
                                        <span className="text-xs text-blue-600">
                      +{meeting.possibleDays.length - 2} ещё
                    </span>
                                    )}
                                </div>
                                {meeting.possibleDays.length > 2 && (
                                    <button
                                        className="text-xs text-blue-600 mt-1 flex items-center"
                                        onClick={() => setExpanded(!expanded)}
                                    >
                                        {expanded ? (
                                            <>
                                                <ChevronUp size={14} className="mr-1"/>
                                                Свернуть
                                            </>
                                        ) : (
                                            <>
                                                <ChevronDown size={14} className="mr-1"/>
                                                Показать все
                                            </>
                                        )}
                                    </button>
                                )}
                            </div>
                        )}
                        {meeting.duration != null && (
                            <span className="text-sm bg-blue-100 text-blue-800 px-2 py-1 rounded mt-1 sm:mt-0">
                Длительность: {meeting.duration} мин
              </span>
                        )}
                    </div>
                </div>
                {!isPersonal && (
                    <div className="flex items-start text-gray-500">
                        <Users size={16} className="mr-2 mt-1"/>
                        <div>
                            <span className="text-sm">Участники:</span>
                            <div className="flex flex-wrap gap-2 mt-1">
                                {meeting.participants
                                    .filter(p => p.user.id !== meeting.authorId)
                                    .map(participant => (
                                        <div
                                            key={participant.user.id}
                                            className="flex items-center bg-gray-100 px-2 py-1 rounded"
                                        >
                      <span className="text-sm text-gray-700">
                        {participant.user.username}
                      </span>
                                            {isOrganizer && (
                                                <div className="ml-2">
                                                    {getStatusBadge(participant.status)}
                                                </div>
                                            )}
                                        </div>
                                    ))}
                            </div>
                        </div>
                    </div>
                )}
            </div>
            <div className="bg-gray-50 px-5 py-3 border-t border-gray-200">
                <div className="flex justify-between items-center">
          <span className="text-sm text-gray-600">
            {isPersonal ? 'Личное событие' : 'Организатор:'}{' '}
              <span className="font-medium">
              {isOrganizerLoading ? 'Загрузка...' : organizer?.username || 'Неизвестно'}
            </span>
          </span>
                    <div className="flex space-x-2">
                        {isPending &&
                            !isPersonal &&
                            currentParticipant?.status === 'PENDING' &&
                            !isOrganizer && (
                                <>
                                    <button
                                        onClick={handleAccept}
                                        className="px-3 py-1 bg-green-600 text-white text-sm rounded hover:bg-green-700 flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
                                        disabled={isResponding || isDeleting}
                                    >
                                        <Check size={14} className="mr-1"/>
                                        {isResponding ? 'Обработка...' : 'Принять'}
                                    </button>
                                    <button
                                        onClick={handleDecline}
                                        className="px-3 py-1 bg-red-600 text-white text-sm rounded hover:bg-red-700 flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
                                        disabled={isResponding || isDeleting}
                                    >
                                        <X size={14} className="mr-1"/>
                                        {isResponding ? 'Обработка...' : 'Отклонить'}
                                    </button>
                                </>
                            )}
                        {isOrganizer && (
                            <>
                                {isPending && !isPersonal && (
                                    <button
                                        onClick={handleConfirmClick}
                                        className="px-3 py-1 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
                                        disabled={
                                            isResponding ||
                                            isDeleting ||
                                            showConfirmation ||
                                            meeting.status === 'ACCEPTED'
                                        }
                                    >
                                        Подтвердить
                                    </button>
                                )}
                                <button
                                    onClick={handleDelete}
                                    className="px-3 py-1 bg-red-600 text-white text-sm rounded hover:bg-red-700 flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
                                    disabled={isResponding || isDeleting || isDeleted}
                                >
                                    <Trash2 size={14} className="mr-1"/>
                                    {isDeleting ? 'Удаление...' : 'Удалить'}
                                </button>
                            </>
                        )}
                    </div>
                </div>
            </div>
            {showConfirmation && (
                <MeetingConfirmation meeting={meeting} onClose={handleCloseConfirmation}/>
            )}
        </div>
    );
};

export default MeetingCard;