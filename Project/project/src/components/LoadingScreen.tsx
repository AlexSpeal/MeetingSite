import React from 'react';

interface LoadingScreenProps {
    message?: string;
}

const LoadingScreen: React.FC<LoadingScreenProps> = ({message = 'Загрузка...'}) => {
    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-xl p-6">
                <h3 className="text-xl font-semibold text-gray-700 mb-4">{message}</h3>
                <p className="text-gray-500">Пожалуйста, подождите...</p>
            </div>
        </div>
    );
};

export default LoadingScreen;