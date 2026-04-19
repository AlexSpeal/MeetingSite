import React, {useState} from 'react';
import {X} from 'lucide-react';
import {useMeetingContext} from '../context/MeetingContext';

interface VkBindingModalProps {
    onClose: () => void;
}

const VkBindingModal: React.FC<VkBindingModalProps> = ({onClose}) => {
    const {startVkBinding, confirmVkBinding, isLoading} = useMeetingContext();

    const [step, setStep] = useState<'screenName' | 'code'>('screenName');
    const [screenName, setScreenName] = useState('');
    const [code, setCode] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [message, setMessage] = useState<string | null>(null);

    const handleStart = async () => {
        setError(null);
        setMessage(null);

        if (!screenName.trim()) {
            setError('Введите логин VK');
            return;
        }

        try {
            const result = await startVkBinding({screenName: screenName.trim()});
            setMessage(typeof result === 'string' ? result : 'Код подтверждения отправлен в VK');
            setStep('code');
        } catch (err: any) {
            setError(err.message || 'Не удалось отправить код');
        }
    };

    const handleConfirm = async () => {
        setError(null);
        setMessage(null);

        if (!code.trim()) {
            setError('Введите код подтверждения');
            return;
        }

        try {
            const result = await confirmVkBinding({code: code.trim()});
            setMessage(typeof result === 'string' ? result : 'VK уведомления успешно подключены');
            setTimeout(() => onClose(), 1000);
        } catch (err: any) {
            setError(err.message || 'Не удалось подтвердить привязку');
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
                <div className="p-6 border-b border-gray-200 flex justify-between items-center">
                    <h2 className="text-xl font-bold text-gray-800">Подключение VK</h2>
                    <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
                        <X size={24}/>
                    </button>
                </div>

                <div className="p-6">
                    {error && (
                        <div className="mb-4 text-red-600 text-sm bg-red-100 px-3 py-2 rounded">
                            {error}
                        </div>
                    )}

                    {message && (
                        <div className="mb-4 text-green-700 text-sm bg-green-100 px-3 py-2 rounded">
                            {message}
                        </div>
                    )}

                    {step === 'screenName' && (
                        <>
                            <p className="text-sm text-gray-600 mb-4">
                                Введите логин VK. Мы отправим код подтверждения в личные сообщения.
                            </p>

                            <input
                                type="text"
                                value={screenName}
                                onChange={(e) => setScreenName(e.target.value)}
                                placeholder="Введите логин VK"
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />

                            <div className="mt-6 flex justify-end space-x-3">
                                <button
                                    onClick={onClose}
                                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                                >
                                    Отмена
                                </button>
                                <button
                                    onClick={handleStart}
                                    disabled={isLoading}
                                    className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                                >
                                    {isLoading ? 'Отправка...' : 'Отправить код'}
                                </button>
                            </div>
                        </>
                    )}

                    {step === 'code' && (
                        <>
                            <p className="text-sm text-gray-600 mb-4">
                                Введите код, который пришел вам в VK.
                            </p>

                            <input
                                type="text"
                                value={code}
                                onChange={(e) => setCode(e.target.value)}
                                placeholder="Введите код"
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                            />

                            <div className="mt-6 flex justify-end space-x-3">
                                <button
                                    onClick={onClose}
                                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                                >
                                    Закрыть
                                </button>
                                <button
                                    onClick={handleConfirm}
                                    disabled={isLoading}
                                    className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50"
                                >
                                    {isLoading ? 'Проверка...' : 'Подтвердить'}
                                </button>
                            </div>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default VkBindingModal;