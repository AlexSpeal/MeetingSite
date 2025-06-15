import React, {useCallback, useEffect, useState} from 'react';
import FullCalendar from '@fullcalendar/react';
import {EventInput} from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import ruLocale from '@fullcalendar/core/locales/ru';
import {useMeetingContext} from '../context/MeetingContext';
import {isBefore, parseISO, startOfDay} from 'date-fns';
import LoadingScreen from './LoadingScreen';

interface CalendarProps {
    onDateSelect?: (date: string) => void;
    selectedDates?: string[];
    selectionMode?: boolean;
}

const Calendar: React.FC<CalendarProps> = ({
                                               onDateSelect,
                                               selectedDates = [],
                                               selectionMode = false,
                                           }) => {
    const {meetings, isLoading} = useMeetingContext();
    const [error, setError] = useState<string | null>(null);
    const today = startOfDay(new Date());

    const handleDateClick = useCallback(
        (arg: { dateStr: string; date: Date }) => {
            if (isBefore(arg.date, today)) return;
            if (selectionMode && onDateSelect) onDateSelect(arg.dateStr);
        },
        [selectionMode, onDateSelect, today]
    );

    const events: EventInput[] = meetings.flatMap((meeting) => {
        const color = meeting.isPersonal ? '#6B46C1' : '#007AFF';

        if (meeting.status === 'ACCEPTED' && meeting.startTime) {
            try {
                parseISO(meeting.startTime);
                return [
                    {
                        id: String(meeting.id),
                        title: meeting.title,
                        start: meeting.startTime,
                        backgroundColor: color,
                        borderColor: color,
                        extendedProps: {
                            status: 'ACCEPTED',
                            description: meeting.description,
                            isPersonal: meeting.isPersonal,
                        },
                    } as EventInput,
                ];
            } catch {
                setError(`Неверный формат времени для встречи "${meeting.title}"`);
                return [];
            }
        }

        if (meeting.possibleDays && meeting.possibleDays.length > 0) {
            return meeting.possibleDays.flatMap((date) => {
                try {
                    parseISO(date);
                    return [
                        {
                            id: `${meeting.id}-${date}`,
                            title: `${meeting.title} (Ожидание)`,
                            start: date,
                            backgroundColor: color,
                            borderColor: color,
                            extendedProps: {
                                status: 'PENDING',
                                description: meeting.description,
                                isPersonal: meeting.isPersonal,
                            },
                        } as EventInput,
                    ];
                } catch {
                    setError(`Неверный формат даты для встречи "${meeting.title}"`);
                    return [];
                }
            });
        }

        return [];
    });

    const selectedDateEvents: EventInput[] = selectionMode
        ? selectedDates.map((date) => ({
            id: `selected-${date}`,
            title: 'Выбрано',
            start: date,
            backgroundColor: '#34D399',
            borderColor: '#34D399',
            classNames: ['selected-date-event'],
        }))
        : [];

    useEffect(() => {
        return () => {
            document.querySelectorAll('.calendar-tooltip').forEach((tooltip) => tooltip.remove());
        };
    }, []);

    if (isLoading) return <LoadingScreen message="Загрузка календаря..."/>;

    return (
        <div className="bg-white rounded-lg shadow-md p-4">
            {error && (
                <div className="mb-4 text-red-600 text-sm bg-red-100 px-3 py-2 rounded flex items-center">
                    <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="2"
                            d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                        />
                    </svg>
                    {error}
                    <button onClick={() => setError(null)} className="ml-2 text-red-800 hover:text-red-900">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth="2"
                                d="M6 18L18 6M6 6l12 12"
                            />
                        </svg>
                    </button>
                </div>
            )}

            <style>
                {`
          .calendar-tooltip {
            position: absolute;
            z-index: 10000;
            pointer-events: none;
          }
          .selected-date-event {
            opacity: 0.7;
            font-weight: bold;
          }
        `}
            </style>

            <FullCalendar
                plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
                initialView="dayGridMonth"
                locale={ruLocale}
                headerToolbar={{
                    left: 'prev,next today',
                    center: 'title',
                    right: 'dayGridMonth,timeGridWeek',
                }}
                buttonText={{today: 'Сегодня', month: 'Месяц', week: 'Неделя'}}
                events={[...events, ...selectedDateEvents]}
                dateClick={handleDateClick}
                height="auto"
                selectable={selectionMode}
                eventTimeFormat={{hour: '2-digit', minute: '2-digit', meridiem: false}}
                eventDidMount={(info) => {
                    const tooltip = document.createElement('div');
                    tooltip.className = 'calendar-tooltip';
                    tooltip.innerHTML = `
            <div class="p-2 bg-gray-800 text-white rounded shadow-lg text-sm max-w-xs">
              <strong>${info.event.title}</strong>
              ${
                        info.event.extendedProps?.description
                            ? `<p class="mt-1">${info.event.extendedProps.description}</p>`
                            : ''
                    }
              <span class="inline-block mt-1 px-2 py-1 text-xs rounded ${
                        info.event.extendedProps?.status === 'ACCEPTED'
                            ? 'bg-green-700'
                            : 'bg-blue-700'
                    }">
                ${
                        info.event.extendedProps?.status === 'ACCEPTED'
                            ? 'Подтверждено'
                            : 'Ожидание'
                    }
              </span>
              ${
                        info.event.extendedProps?.isPersonal
                            ? '<span class="inline-block mt-1 ml-1 px-2 py-1 bg-purple-700 text-xs rounded">Личное</span>'
                            : ''
                    }
            </div>
          `;

                    const eventEl = info.el;
                    eventEl.style.position = 'relative';

                    const showTooltip = () => {
                        document.body.appendChild(tooltip);
                        const rect = eventEl.getBoundingClientRect();
                        tooltip.style.top = `${rect.bottom + window.scrollY + 5}px`;
                        tooltip.style.left = `${rect.left + window.scrollX}px`;
                    };

                    const hideTooltip = () => {
                        if (document.body.contains(tooltip)) {
                            document.body.removeChild(tooltip);
                        }
                    };

                    eventEl.addEventListener('mouseover', showTooltip);
                    eventEl.addEventListener('mouseout', hideTooltip);
                    info.el.addEventListener('remove', hideTooltip);
                }}
            />
        </div>
    );
};

export default Calendar;
