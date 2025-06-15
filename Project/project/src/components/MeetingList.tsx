import React, {useMemo, useState} from 'react';
import {Calendar, Clock, Filter, Tag} from 'lucide-react';
import {useMeetingContext} from '../context/MeetingContext';
import MeetingCard from './MeetingCard';
import {Event, SortOption} from '../types';
import LoadingScreen from './LoadingScreen';

interface MeetingListProps {
    type: 'pending' | 'confirmed' | 'all';
    title: string;
}

const MeetingList: React.FC<MeetingListProps> = ({type, title}) => {
    const {meetings, sortMeetings, deleteMeeting, isLoading, isUserLoading} = useMeetingContext();
    const [sortOption, setSortOption] = useState<SortOption>('DATE');
    const [showPersonal, setShowPersonal] = useState<boolean>(true);

    const handleSort = (option: SortOption) => {
        setSortOption(option);
        sortMeetings(option);
    };

    const handleDelete = async (meetingId: number) => {
        try {
            await deleteMeeting(meetingId);
        } catch (err) {
            console.error('MeetingList: Failed to delete meeting:', err);
        }
    };

    const filteredMeetings = useMemo(() => {
        let result = meetings;
        if (type === 'pending') {
            result = meetings.filter((m) => m.status === 'PENDING');
        } else if (type === 'confirmed') {
            result = meetings.filter((m) => m.status === 'ACCEPTED');
        }
        if (!showPersonal) {
            result = result.filter((m) => !m.isPersonal);
        }
        return result;
    }, [meetings, type, showPersonal]);

    if (isLoading || isUserLoading) {
        return <LoadingScreen message="Загрузка списка встреч..."/>;
    }

    return (
        <div className="bg-white rounded-lg shadow-md p-5">
            <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-800">{title}</h2>
                <div className="flex items-center space-x-4">
                    <div className="flex items-center">
                        <input
                            type="checkbox"
                            id="showPersonal"
                            checked={showPersonal}
                            onChange={() => setShowPersonal(!showPersonal)}
                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                        />
                        <label htmlFor="showPersonal" className="ml-2 text-sm text-gray-700 flex items-center">
                            <Tag size={14} className="mr-1"/>
                            Показывать личные события
                        </label>
                    </div>
                    <div className="relative group">
                        <button className="flex items-center text-gray-600 hover:text-gray-800">
                            <Filter size={18} className="mr-1"/>
                            <span className="text-sm">Сортировка</span>
                        </button>
                        <div
                            className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg overflow-hidden z-20 hidden group-hover:block">
                            <div className="py-1">
                                {(['DATE', 'TITLE', 'STATUS'] as SortOption[]).map((option) => (
                                    <button
                                        key={option}
                                        onClick={() => handleSort(option)}
                                        className={`flex items-center px-4 py-2 text-sm w-full text-left ${
                                            sortOption === option ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-100'
                                        }`}
                                    >
                                        <Clock size={16} className="mr-2"/>
                                        {option === 'DATE' ? 'По дате' : option === 'TITLE' ? 'По названию' : 'По статусу'}
                                    </button>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div className="space-y-6">
                {filteredMeetings.length > 0 ? (
                    filteredMeetings.map((meeting: Event) => (
                        <MeetingCard
                            key={meeting.id}
                            meeting={meeting}
                            onDelete={handleDelete}
                        />
                    ))
                ) : (
                    <div className="text-center py-8">
                        <Calendar size={48} className="mx-auto text-gray-300 mb-2"/>
                        <p className="text-gray-500">Встречи не найдены</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default React.memo(MeetingList);