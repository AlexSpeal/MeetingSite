import React, {useCallback, useEffect, useRef, useState} from 'react';
import {Info, X} from 'lucide-react';
import {AvailabilityResponse, Event, ScheduleRequest} from '../types';
import {useMeetingContext} from '../context/MeetingContext';
import {addMinutes, format, isAfter, parse} from 'date-fns';
import LoadingScreen from './LoadingScreen';

interface MeetingConfirmationProps {
    meeting: Event;
    onClose: () => void;
}

interface TimeOption {
    id: string;
    dateLabel: string;
    timeLabel: string;
}

const MeetingConfirmation: React.FC<MeetingConfirmationProps> = ({meeting, onClose}) => {
    const {getBestPossibleDates, confirmMeeting, deleteMeeting} = useMeetingContext();
    const [options, setOptions] = useState<TimeOption[]>([]);
    const [index, setIndex] = useState(0);
    const [selectedSlot, setSelectedSlot] = useState<string>('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [infoMessage, setInfoMessage] = useState<string | null>(null);
    const [shouldDelete, setShouldDelete] = useState(false);
    const [havePending, setHavePending] = useState(false);
    const [maxCount, setMaxCount] = useState(0);
    const [showPendingOptions, setShowPendingOptions] = useState(false);
    const fetchedRef = useRef(false);

    const loadOptions = useCallback(async () => {
        if (fetchedRef.current) return;
        setLoading(true);
        setError(null);
        try {
            const data: AvailabilityResponse = await getBestPossibleDates(meeting.id);
            const intervals = Array.isArray(data.possibleIntervals) ? data.possibleIntervals : [];
            const list: TimeOption[] = [];

            console.log('Meeting duration:', meeting.duration);
            intervals.forEach((interval) => {
                const startDateTime = parse(
                    `${interval.date} ${interval.start}`,
                    'yyyy-MM-dd HH:mm:ss',
                    new Date()
                );
                const endDateTime = parse(
                    `${interval.date} ${interval.end}`,
                    'yyyy-MM-dd HH:mm:ss',
                    new Date()
                );
                const meetingDuration = meeting.duration || 60;
                let current = startDateTime;

                console.log(`Processing interval: ${interval.date} ${interval.start}–${interval.end}`);

                while (!isAfter(current, endDateTime)) {
                    const endTime = addMinutes(current, meetingDuration);
                    const maxEndTime = parse(
                        `${interval.date} 18:00:00`,
                        'yyyy-MM-dd HH:mm:ss',
                        new Date()
                    );
                    if (!isAfter(endTime, maxEndTime)) {
                        const id = format(current, "yyyy-MM-dd'T'HH:mm:ss");
                        const dateLabel = format(current, 'dd.MM.yyyy');
                        const timeLabel = format(current, 'HH:mm');
                        list.push({id, dateLabel, timeLabel});
                    }
                    current = addMinutes(current, 1);
                }
            });

            console.log('Generated options:', list);
            setOptions(list);
            setMaxCount(data.maxCount);
            setHavePending(data.havePending);
            if (list.length > 0) {
                setIndex(0);
                setSelectedSlot(list[0].id);
            }

            const isPersonal = meeting.isPersonal || meeting.participants.length <= 1;
            if (isPersonal && list.length === 0) {
                setInfoMessage('Невозможно подтвердить: нет доступных слотов для личной встречи.');
                setShouldDelete(true);
            } else if (!isPersonal) {
                if (data.maxCount === 0) {
                    setInfoMessage('Невозможно подтвердить: автор встречи не может присутствовать.');
                    setShouldDelete(true);
                } else if (data.maxCount === 1 && data.havePending) {
                    setInfoMessage('Только вы доступны, но есть ожидающие участники. Ждать или удалить встречу?');
                    setShowPendingOptions(true);
                } else if (data.maxCount === 1 && !data.havePending) {
                    setInfoMessage('Невозможно подтвердить: никто из участников не может присутствовать.');
                    setShouldDelete(true);
                } else if (list.length <= 1) {
                    setInfoMessage('Невозможно подтвердить: недостаточно вариантов для групповой встречи.');
                    setShouldDelete(true);
                }
            }
        } catch (e) {
            setError('Ошибка загрузки доступных времён.');
        } finally {
            setLoading(false);
            fetchedRef.current = true;
        }
    }, [getBestPossibleDates, meeting]);

    useEffect(() => {
        loadOptions();
    }, [loadOptions]);

    const handleSliderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const i = Number(e.target.value);
        setIndex(i);
        setSelectedSlot(options[i]?.id || '');
    };

    const handleConfirm = useCallback(async () => {
        if (!selectedSlot) {
            alert('Выберите слот');
            return;
        }
        try {
            await confirmMeeting(meeting.id, {startTime: selectedSlot} as ScheduleRequest);
            onClose();
        } catch (e) {
            alert('Не удалось подтвердить встречу.');
        }
    }, [confirmMeeting, meeting.id, onClose, selectedSlot]);

    const handleCancel = useCallback(async () => {
        const isPersonal = meeting.isPersonal || meeting.participants.length <= 1;
        if (isPersonal || shouldDelete || (!isPersonal && maxCount === 0 && !havePending)) {
            try {
                await deleteMeeting(meeting.id);
            } catch (e) {
                console.error('Failed to delete meeting:', e);
            }
        }
        onClose();
    }, [deleteMeeting, meeting.id, meeting.isPersonal, meeting.participants, onClose, shouldDelete, maxCount, havePending]);

    const handleDelete = useCallback(async () => {
        try {
            await deleteMeeting(meeting.id);
        } catch (e) {
            console.error('Failed to delete meeting:', e);
            alert('Не удалось удалить встречу.');
        }
        onClose();
    }, [deleteMeeting, meeting.id, onClose]);

    useEffect(() => () => {
        fetchedRef.current = false;
    }, [meeting.id]);

    if (loading) return <LoadingScreen message="Загрузка..."/>;

    if (shouldDelete) {
        return (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
                <div className="bg-white p-6 rounded-lg shadow-lg text-center w-80">
                    <p className="text-red-600 mb-4">{infoMessage}</p>
                    <button onClick={handleCancel} className="px-4 py-2 bg-blue-600 text-white rounded">Ок</button>
                </div>
            </div>
        );
    }

    if (showPendingOptions) {
        return (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
                <div className="bg-white p-6 rounded-lg shadow-lg w-full max-w-md">
                    <div className="flex justify-between items-center p-4 border-b">
                        <h2 className="text-lg font-semibold">Подтвердить встречу</h2>
                        <button onClick={onClose}><X/></button>
                    </div>
                    <div className="p-4">
                        <div className="flex items-start bg-blue-50 p-3 rounded mb-4">
                            <Info className="text-blue-500 mr-2"/>
                            <p className="text-blue-700 text-sm">{infoMessage}</p>
                        </div>
                        <h3 className="font-medium mb-1">{meeting.title}</h3>
                        <p className="text-gray-600 mb-4">{meeting.description || 'Описание отсутствует'}</p>
                        <div className="flex justify-end mt-4 space-x-2">
                            <button onClick={onClose} className="px-3 py-1 border rounded">Ждать</button>
                            <button onClick={handleDelete} className="px-3 py-1 bg-red-600 text-white rounded">Удалить
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
            <div className="bg-white rounded-lg shadow-lg w-full max-w-md">
                <div className="flex justify-between items-center p-4 border-b">
                    <h2 className="text-lg font-semibold">Подтвердить встречу</h2>
                    <button onClick={onClose}><X/></button>
                </div>
                <div className="p-4">
                    {error && <div className="text-red-600 mb-4">{error}</div>}
                    <div className="flex items-start bg-blue-50 p-3 rounded mb-4">
                        <Info className="text-blue-500 mr-2"/>
                        <p className="text-blue-700 text-sm">Выберите время из доступных вариантов.</p>
                    </div>
                    <h3 className="font-medium mb-1">{meeting.title}</h3>
                    <p className="text-gray-600 mb-4">{meeting.description || 'Описание отсутствует'}</p>
                    {options.length > 0 ? (
                        <>
                            <label
                                className="block text-sm mb-2">{options[index].dateLabel} {options[index].timeLabel}</label>
                            <input
                                type="range"
                                min={0}
                                max={options.length - 1}
                                step={1}
                                value={index}
                                onChange={handleSliderChange}
                                className="w-full h-2 rounded-lg bg-gray-200 appearance-none cursor-pointer"
                            />
                            <div className="flex justify-between text-xs text-gray-500 mt-1">
                                <span>{options[0].timeLabel}</span>
                                <span>{options[options.length - 1].timeLabel}</span>
                            </div>
                            <div className="mt-4 p-2 bg-green-50 border border-green-200 rounded">
                                <p className="text-green-800 text-sm">Вы выбрали:</p>
                                <p className="font-bold">{options[index].timeLabel} {options[index].dateLabel}</p>
                            </div>
                        </>
                    ) : (
                        <p className="text-center text-gray-500">Нет доступных слотов</p>
                    )}
                    <div className="flex justify-end mt-4 space-x-2">
                        <button onClick={handleCancel} className="px-3 py-1 border rounded">Отмена</button>
                        <button
                            onClick={handleConfirm}
                            disabled={!selectedSlot}
                            className="px-3 py-1 bg-blue-600 text-white rounded disabled:opacity-50"
                        >Подтвердить
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default MeetingConfirmation;