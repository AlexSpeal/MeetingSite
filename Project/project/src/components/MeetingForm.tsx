import React, {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {Calendar, Search, X} from 'lucide-react';
import {useMeetingContext} from '../context/MeetingContext';
import {CreatingEventRequest, Event, EventParticipant} from '../types';
import InteractiveCalendar from './Calendar';
import LoadingScreen from './LoadingScreen';
import MeetingConfirmation from './MeetingConfirmation';

interface MeetingFormProps {
    onClose: () => void;
}

interface FormData {
    title: string;
    description?: string;
    isPersonal: boolean;
    duration: number;
}

const MeetingForm: React.FC<MeetingFormProps> = ({onClose}) => {
    const {
        register,
        handleSubmit,
        watch,
        formState: {errors},
    } = useForm<FormData>({
        defaultValues: {
            isPersonal: false,
            duration: 60,
        },
    });
    const {addMeeting, currentUser, getUserByUsername, isLoading, isUserLoading} = useMeetingContext();

    const [selectedDates, setSelectedDates] = useState<string[]>([]);
    const [selectedParticipants, setSelectedParticipants] = useState<EventParticipant[]>([]);
    const [calendarVisible, setCalendarVisible] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const [searchError, setSearchError] = useState<string | null>(null);
    const [formError, setFormError] = useState<string | null>(null);
    const [createdMeeting, setCreatedMeeting] = useState<Event | null>(null);
    const [showConfirmation, setShowConfirmation] = useState(false);

    const isPersonal = watch('isPersonal');

    useEffect(() => {
        if (!isLoading && !isUserLoading && !currentUser) {
            setFormError('Пользователь не аутентифицирован');
        }
    }, [isLoading, isUserLoading, currentUser]);

    const handleDateSelect = (date: string) => {
        setSelectedDates((prev) =>
            prev.includes(date) ? prev.filter((d) => d !== date) : [...prev, date]
        );
    };

    const addParticipant = async () => {
        if (!searchQuery.trim()) {
            setSearchError('Введите имя пользователя');
            return;
        }

        if (!currentUser) {
            setSearchError('Текущий пользователь не определён');
            return;
        }

        try {
            const user = await getUserByUsername(searchQuery.trim());
            if (!user) {
                setSearchError('Пользователь не найден');
                return;
            }

            if (user.id === currentUser.id) {
                setSearchError('Нельзя добавить себя');
                return;
            }

            if (selectedParticipants.some((p) => p.user.id === user.id)) {
                setSearchError('Пользователь уже добавлен');
                return;
            }

            setSelectedParticipants((prev) => [
                ...prev,
                {user, status: 'PENDING'} as EventParticipant,
            ]);
            setSearchQuery('');
            setSearchError(null);
        } catch (err) {
            setSearchError('Ошибка при поиске пользователя');
        }
    };

    const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSearchQuery(e.target.value);
        setSearchError(null);
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            addParticipant();
        }
    };

    const removeParticipant = (userId: number) => {
        setSelectedParticipants((prev) => prev.filter((p) => p.user.id !== userId));
    };

    const onSubmit = async (data: FormData) => {
        if (selectedDates.length === 0) {
            setFormError('Пожалуйста, выберите хотя бы одну возможную дату');
            return;
        }

        if (!data.isPersonal && selectedParticipants.length === 0) {
            setFormError('Пожалуйста, выберите хотя бы одного участника');
            return;
        }

        try {
            const meetingData: CreatingEventRequest = {
                title: data.title,
                description: data.description || '',
                possibleDays: selectedDates,
                participants: data.isPersonal ? [] : selectedParticipants.map((p) => p.user.id),
                duration: data.duration,
            };
            const newMeeting = await addMeeting(meetingData);
            setCreatedMeeting(newMeeting);
            if (data.isPersonal) {
                setShowConfirmation(true);
            } else {
                onClose();
            }
        } catch (err) {
            setFormError('Не удалось создать встречу');
        }
    };

    if (isLoading || isUserLoading) {
        return <LoadingScreen message="Загрузка формы встречи..."/>;
    }

    if (formError) {
        return (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                <div className="bg-white rounded-lg shadow-xl p-6">
                    <h3 className="text-xl font-semibold text-red-600 mb-4">Ошибка</h3>
                    <p className="text-gray-700">{formError}</p>
                    <button
                        onClick={onClose}
                        className="mt-4 px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
                    >
                        Закрыть
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-auto">
                <div className="p-6 border-b border-gray-200 flex justify-between items-center">
                    <h2 className="text-2xl font-bold text-gray-800">
                        {isPersonal ? 'Создать личное событие' : 'Создать встречу'}
                    </h2>
                    <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
                        <X size={24}/>
                    </button>
                </div>

                <form onSubmit={handleSubmit(onSubmit)} className="p-6">
                    <div className="space-y-6">
                        <div className="flex items-center mb-4">
                            <input
                                type="checkbox"
                                id="isPersonal"
                                {...register('isPersonal')}
                                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                            />
                            <label htmlFor="isPersonal" className="ml-2 block text-sm text-gray-900">
                                Это личное событие (только для меня)
                            </label>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                {isPersonal ? 'Название события' : 'Название встречи'}
                            </label>
                            <input
                                {...register('title', {required: 'Название обязательно'})}
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder={isPersonal ? 'Введите название события' : 'Введите название встречи'}
                            />
                            {errors.title && (
                                <p className="mt-1 text-sm text-red-600">{errors.title.message}</p>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Описание</label>
                            <textarea
                                {...register('description')}
                                rows={3}
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder={isPersonal ? 'Введите описание события' : 'Введите описание встречи'}
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Длительность (в минутах)
                            </label>
                            <input
                                type="number"
                                {...register('duration', {
                                    required: 'Длительность обязательна',
                                    min: {value: 1, message: 'Длительность должна быть больше 0'},
                                    max: {value: 540, message: 'Длительность не может превышать 540 минут (1 день)'},
                                    pattern: {
                                        value: /^\d+$/,
                                        message: 'Длительность должна быть целым числом',
                                    },
                                })}
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="Введите длительность в минутах (макс. 540)"
                                min="1"
                                max="540"
                                step="1"
                            />
                            {errors.duration && (
                                <p className="mt-1 text-sm text-red-600">{errors.duration.message}</p>
                            )}
                        </div>

                        <div>
                            <div className="flex justify-between items-center mb-2">
                                <label className="block text-sm font-medium text-gray-700">Возможные даты</label>
                                <button
                                    type="button"
                                    onClick={() => setCalendarVisible(!calendarVisible)}
                                    className="flex items-center text-sm text-blue-600 hover:text-blue-800"
                                >
                                    <Calendar size={16} className="mr-1"/>
                                    {calendarVisible ? 'Скрыть календарь' : 'Показать календарь'}
                                </button>
                            </div>

                            {calendarVisible && (
                                <div className="mb-4 border border-gray-200 rounded-lg p-4">
                                    <p className="text-sm text-gray-600 mb-2">
                                        Нажмите на даты, чтобы выбрать возможные дни для{' '}
                                        {isPersonal ? 'события' : 'встречи'}
                                    </p>
                                    <div className="calendar-container">
                                        <InteractiveCalendar
                                            onDateSelect={handleDateSelect}
                                            selectedDates={selectedDates}
                                            selectionMode={true}
                                        />
                                    </div>
                                </div>
                            )}

                            <div className="flex flex-wrap gap-2 mt-2">
                                {selectedDates.length > 0 ? (
                                    selectedDates.map((date) => (
                                        <div
                                            key={date}
                                            className="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm flex items-center"
                                        >
                                            {date}
                                            <button
                                                type="button"
                                                onClick={() => handleDateSelect(date)}
                                                className="ml-2 text-blue-600 hover:text-blue-800"
                                            >
                                                <X size={14}/>
                                            </button>
                                        </div>
                                    ))
                                ) : (
                                    <p className="text-sm text-gray-500">Даты не выбраны</p>
                                )}
                            </div>
                        </div>

                        {!isPersonal && (
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Участники</label>

                                <div className="relative mb-4 flex items-center space-x-2">
                                    <div
                                        className="flex items-center border border-gray-300 rounded-md overflow-hidden flex-1">
                                        <div className="pl-3 text-gray-500">
                                            <Search size={18}/>
                                        </div>
                                        <input
                                            type="text"
                                            value={searchQuery}
                                            onChange={handleSearchChange}
                                            onKeyDown={handleKeyDown}
                                            placeholder="Введите имя пользователя..."
                                            className="w-full px-3 py-2 focus:outline-none"
                                        />
                                    </div>
                                    <button
                                        type="button"
                                        onClick={addParticipant}
                                        className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                                    >
                                        Добавить
                                    </button>
                                </div>

                                {searchError && (
                                    <p className="mt-1 text-sm text-red-600">{searchError}</p>
                                )}

                                <div className="space-y-2">
                                    {selectedParticipants.length > 0 ? (
                                        selectedParticipants.map((participant) => (
                                            <div
                                                key={participant.user.id}
                                                className="flex items-center justify-between p-3 border border-gray-200 rounded-lg"
                                            >
                                                <span>{participant.user.username}</span>
                                                <button
                                                    type="button"
                                                    onClick={() => removeParticipant(participant.user.id)}
                                                    className="text-red-500 hover:text-red-700"
                                                >
                                                    <X size={18}/>
                                                </button>
                                            </div>
                                        ))
                                    ) : (
                                        <p className="text-sm text-gray-500">Участники не выбраны</p>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="mt-8 flex justify-end space-x-3">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                        >
                            Отмена
                        </button>
                        <button
                            type="submit"
                            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                        >
                            {isPersonal ? 'Создать событие' : 'Создать встречу'}
                        </button>
                    </div>
                </form>

                {showConfirmation && createdMeeting && (
                    <MeetingConfirmation
                        meeting={createdMeeting}
                        onClose={() => {
                            setShowConfirmation(false);
                            onClose();
                        }}
                    />
                )}
            </div>
        </div>
    );
};

export default MeetingForm;