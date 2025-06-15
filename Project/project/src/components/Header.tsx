import React, {useEffect, useState} from 'react';
import {Calendar as CalendarIcon, User} from 'lucide-react';
import {useMeetingContext} from '../context/MeetingContext';
import LoadingScreen from './LoadingScreen';

interface HeaderProps {
    onCreateMeeting: () => void;
    onViewProfile: () => void;
}

const Header: React.FC<HeaderProps> = ({onCreateMeeting, onViewProfile}) => {
    const {currentUser, isLoading} = useMeetingContext();
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const checkUser = () => {
            if (!currentUser) {
                setError('Пользователь не аутентифицирован');
            }
        };

        if (isLoading) {
            checkUser();
        }
    }, [isLoading, currentUser]);

    if (isLoading) {
        return <LoadingScreen message="Загрузка заголовка..."/>;
    }

    if (error) {
        return (
            <header className="bg-white shadow-sm">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center py-4">
                        <div className="flex items-center">
                            <CalendarIcon size={28} className="text-blue-600 mr-2"/>
                            <h1 className="text-2xl font-bold text-gray-900">Планировщик встреч</h1>
                        </div>
                        <div className="text-red-600">Ошибка: {error}</div>
                    </div>
                </div>
            </header>
        );
    }

    if (!currentUser) {
        return (
            <header className="bg-white shadow-sm">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center py-4">
                        <div className="flex items-center">
                            <CalendarIcon size={28} className="text-blue-600 mr-2"/>
                            <h1 className="text-2xl font-bold text-gray-900">Планировщик встреч</h1>
                        </div>
                        <div className="text-gray-600">Пользователь не определён</div>
                    </div>
                </div>
            </header>
        );
    }

    return (
        <header className="bg-white shadow-sm">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between items-center py-4">
                    <div className="flex items-center">
                        <CalendarIcon size={28} className="text-blue-600 mr-2"/>
                        <h1 className="text-2xl font-bold text-gray-900">Планировщик встреч</h1>
                    </div>

                    <div className="flex items-center space-x-4">
                        <button
                            onClick={onCreateMeeting}
                            className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md flex items-center"
                        >
                            <CalendarIcon size={18} className="mr-2"/>
                            Создать встречу
                        </button>

                        <div className="flex items-center cursor-pointer" onClick={onViewProfile}>
                            <div className="flex items-center bg-gray-100 px-3 py-2 rounded-md hover:bg-gray-200">
                                <User size={18} className="text-gray-600 mr-2"/>
                                <span className="font-medium">{currentUser.username}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </header>
    );
};

export default Header;