import React, {useEffect, useState} from 'react';
import {Calendar, Clock, Gauge, LogOut, User, Users, X} from 'lucide-react';
import {useMeetingContext} from '../context/MeetingContext';
import {format, parseISO} from 'date-fns';
import LoadingScreen from './LoadingScreen';
import {useNavigate} from 'react-router-dom';
import VkBindingModal from './VkBindingModal';

interface UserProfileProps {
    onClose: () => void;
}

interface LogoutModalProps {
    onConfirm: () => void;
    onCancel: () => void;
}

const LogoutModal: React.FC<LogoutModalProps> = ({onConfirm, onCancel}) => {
    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
                <h3 className="text-xl font-semibold mb-4">Вы уверены, что хотите выйти?</h3>
                <div className="flex justify-end space-x-3">
                    <button
                        onClick={onCancel}
                        className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                    >
                        Отмена
                    </button>
                    <button
                        onClick={onConfirm}
                        className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
                    >
                        Да
                    </button>
                </div>
            </div>
        </div>
    );
};

const UserProfile: React.FC<UserProfileProps> = ({onClose}) => {
    const {currentUser, meetings, isLoading, logout, disableVkBinding, updateDailyLoad} = useMeetingContext();
    const [error, setError] = useState<string | null>(null);
    const [showLogoutModal, setShowLogoutModal] = useState(false);
    const navigate = useNavigate();
    const [showVkModal, setShowVkModal] = useState(false);
    const [vkMessage, setVkMessage] = useState<string | null>(null);
    const [vkError, setVkError] = useState<string | null>(null);
    const [dailyLoadInput, setDailyLoadInput] = useState<string>(
        currentUser?.dailyLoadMinutes != null ? String(currentUser.dailyLoadMinutes / 60) : ''
    );
    const [loadMessage, setLoadMessage] = useState<string | null>(null);
    const [loadError, setLoadError] = useState<string | null>(null);

    useEffect(() => {
        setDailyLoadInput(
            currentUser?.dailyLoadMinutes != null ? String(currentUser.dailyLoadMinutes / 60) : ''
        );
    }, [currentUser?.dailyLoadMinutes]);

    const handleSaveDailyLoad = async () => {
        setLoadMessage(null);
        setLoadError(null);

        const trimmed = dailyLoadInput.trim();
        let dailyLoadMinutes: number | null;
        if (trimmed === '') {
            dailyLoadMinutes = null;
        } else {
            const hours = Number(trimmed);
            if (!Number.isFinite(hours) || hours <= 0 || hours > 24) {
                setLoadError('Введите число от 1 до 24, или оставьте поле пустым для снятия ограничения');
                return;
            }
            dailyLoadMinutes = Math.round(hours * 60);
        }

        try {
            await updateDailyLoad({dailyLoadMinutes});
            setLoadMessage('Ежедневная нагрузка обновлена');
        } catch (err: any) {
            setLoadError(err.message || 'Не удалось обновить ежедневную нагрузку');
        }
    };
    const handleLogout = () => {
        logout();
        setShowLogoutModal(false);
        onClose();
        navigate('/auth');
    };
    const handleDisableVk = async () => {
        setVkError(null);
        setVkMessage(null);

        try {
            const result = await disableVkBinding();
            setVkMessage(typeof result === 'string' ? result : 'VK уведомления отключены');
        } catch (err: any) {
            setVkError(err.message || 'Не удалось отключить VK');
        }
    };

    useEffect(() => {
        meetings.forEach((meeting) => {
            if (meeting.status === 'ACCEPTED' && meeting.startTime) {
                try {
                    parseISO(meeting.startTime);
                } catch (err) {
                    setError(`Неверный формат времени для встречи "${meeting.title}"`);
                }
            }
            if (meeting.possibleDays && meeting.possibleDays.length > 0) {
                meeting.possibleDays.forEach((date) => {
                    try {
                        parseISO(date);
                    } catch (err) {
                        setError(`Неверный формат даты для встречи "${meeting.title}"`);
                    }
                });
            }
        });
    }, [meetings]);

    if (isLoading) {
        return <LoadingScreen message="Загрузка данных профиля..."/>;
    }

    if (!currentUser) {
        return (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                <div className="bg-white rounded-lg shadow-xl p-6">
                    <h3 className="text-xl font-semibold text-red-600 mb-4">Ошибка</h3>
                    <p className="text-gray-700">Пользователь не авторизован</p>
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

    if (error) {
        return (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                <div className="bg-white rounded-lg shadow-xl p-6">
                    <h3 className="text-xl font-semibold text-red-600 mb-4">Ошибка</h3>
                    <p className="text-gray-700">{error}</p>
                    <div className="flex justify-end space-x-3 mt-4">
                        <button
                            onClick={() => setError(null)}
                            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                        >
                            Сбросить ошибку
                        </button>
                        <button
                            onClick={onClose}
                            className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
                        >
                            Закрыть
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    const confirmedMeetings = meetings.filter((meeting) => meeting.status === 'ACCEPTED');
    const pendingMeetings = meetings.filter((meeting) => meeting.status === 'PENDING');

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-auto">
                <div className="p-6 border-b border-gray-200 flex justify-between items-center">
                    <h2 className="text-2xl font-bold text-gray-800">Ваш профиль</h2>
                    <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
                        <X size={24}/>
                    </button>
                </div>

                <div className="p-6">
                    <div className="flex items-center mb-6">
                        <div className="bg-gray-100 p-4 rounded-lg">
                            <User size={32} className="text-gray-600"/>
                        </div>
                        <div className="ml-4">
                            <h3 className="text-xl font-semibold">{currentUser.username}</h3>
                            <p className="text-gray-500">ID пользователя: {currentUser.id}</p>
                        </div>
                    </div>

                    <div className="mb-6">
                        <h4 className="text-lg font-semibold mb-3 flex items-center">
                            <Calendar size={20} className="mr-2 text-blue-600"/>
                            Предстоящие встречи
                        </h4>
                        {confirmedMeetings.length > 0 ? (
                            <div className="space-y-3">
                                {confirmedMeetings.map((meeting) => (
                                    <div key={meeting.id} className="border border-gray-200 rounded-lg p-4">
                                        <div className="flex justify-between items-start">
                                            <h5 className="font-medium">{meeting.title}</h5>
                                            <span className="bg-green-100 text-green-800 px-2 py-1 rounded text-xs">
                        Подтверждено
                      </span>
                                        </div>
                                        <p className="text-gray-600 text-sm mt-1">{meeting.description}</p>
                                        <div className="flex items-center mt-2 text-sm text-gray-500">
                                            <Clock size={16} className="mr-1"/>
                                            {meeting.startTime && (
                                                <span>
                          {(() => {
                              try {
                                  return format(parseISO(meeting.startTime), 'dd.MM.yyyy в HH:mm');
                              } catch {
                                  return 'Неверная дата';
                              }
                          })()}
                        </span>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="text-gray-500">Нет предстоящих подтвержденных встреч</p>
                        )}
                    </div>

                    <div>
                        <h4 className="text-lg font-semibold mb-3 flex items-center">
                            <Users size={20} className="mr-2 text-blue-600"/>
                            Ожидающие приглашения
                        </h4>
                        {pendingMeetings.length > 0 ? (
                            <div className="space-y-3">
                                {pendingMeetings.map((meeting) => (
                                    <div key={meeting.id} className="border border-gray-200 rounded-lg p-4">
                                        <div className="flex justify-between items-start">
                                            <h5 className="font-medium">{meeting.title}</h5>
                                            <span className="bg-yellow-100 text-yellow-800 px-2 py-1 rounded text-xs">
                        Ожидание
                      </span>
                                        </div>
                                        <p className="text-gray-600 text-sm mt-1">{meeting.description}</p>
                                        <div className="flex flex-wrap gap-1 mt-2">
                                            {meeting.possibleDays.slice(0, 3).map((date) => (
                                                <span
                                                    key={date}
                                                    className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded"
                                                >
                          {(() => {
                              try {
                                  return format(parseISO(date), 'dd.MM');
                              } catch {
                                  return 'Неверная дата';
                              }
                          })()}
                        </span>
                                            ))}
                                            {meeting.possibleDays.length > 3 && (
                                                <span className="text-xs text-blue-600">
                          +{meeting.possibleDays.length - 3} ещё
                        </span>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="text-gray-500">Нет ожидающих приглашений на встречи</p>
                        )}
                    </div>
                    <div className="mt-8">
                        <h4 className="text-lg font-semibold mb-3 flex items-center">
                            <User size={20} className="mr-2 text-blue-600"/>
                            VK-уведомления
                        </h4>

                        {vkError && (
                            <div className="mb-3 text-red-600 text-sm bg-red-100 px-3 py-2 rounded">
                                {vkError}
                            </div>
                        )}

                        {vkMessage && (
                            <div className="mb-3 text-green-700 text-sm bg-green-100 px-3 py-2 rounded">
                                {vkMessage}
                            </div>
                        )}

                        {currentUser.vkUserId ? (
                            <div className="flex items-center justify-between border border-gray-200 rounded-lg p-4">
                                <div>
                                    <p className="font-medium text-gray-800">VK подключен</p>
                                    <p className="text-sm text-gray-500">VK ID: {currentUser.vkUserId}</p>
                                </div>
                                <button
                                    onClick={handleDisableVk}
                                    className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
                                >
                                    Отключить
                                </button>
                            </div>
                        ) : (
                            <div className="flex items-center justify-between border border-gray-200 rounded-lg p-4">
                                <div>
                                    <p className="font-medium text-gray-800">VK не подключен</p>
                                    <p className="text-sm text-gray-500">
                                        Подключите VK, чтобы получать напоминания о встречах
                                    </p>
                                </div>
                                <button
                                    onClick={() => setShowVkModal(true)}
                                    className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                                >
                                    Подключить
                                </button>
                            </div>
                        )}
                    </div>

                    <div className="mt-8">
                        <h4 className="text-lg font-semibold mb-3 flex items-center">
                            <Gauge size={20} className="mr-2 text-blue-600"/>
                            Ежедневная нагрузка
                        </h4>

                        {loadError && (
                            <div className="mb-3 text-red-600 text-sm bg-red-100 px-3 py-2 rounded">
                                {loadError}
                            </div>
                        )}

                        {loadMessage && (
                            <div className="mb-3 text-green-700 text-sm bg-green-100 px-3 py-2 rounded">
                                {loadMessage}
                            </div>
                        )}

                        <div className="border border-gray-200 rounded-lg p-4">
                            <p className="text-sm text-gray-500 mb-3">
                                Максимальное количество часов встреч в день. Слоты, в которые ваша нагрузка
                                превысит этот лимит, не будут предлагаться. Оставьте пустым, чтобы снять ограничение.
                            </p>
                            <div className="flex items-center space-x-2">
                                <input
                                    type="number"
                                    min="1"
                                    max="24"
                                    step="0.5"
                                    value={dailyLoadInput}
                                    onChange={(e) => setDailyLoadInput(e.target.value)}
                                    placeholder="часов"
                                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                                <button
                                    onClick={handleSaveDailyLoad}
                                    className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                                >
                                    Сохранить
                                </button>
                            </div>
                        </div>
                    </div>

                    <div className="mt-8">
                        <button
                            onClick={() => setShowLogoutModal(true)}
                            className="flex items-center px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
                        >
                            <LogOut size={18} className="mr-2"/>
                            Выйти из аккаунта
                        </button>
                    </div>
                </div>
                {showVkModal && (
                    <VkBindingModal onClose={() => setShowVkModal(false)} />
                )}

                {showLogoutModal && (
                    <LogoutModal
                        onConfirm={handleLogout}
                        onCancel={() => setShowLogoutModal(false)}
                    />
                )}
            </div>
        </div>
    );
};

export default UserProfile;