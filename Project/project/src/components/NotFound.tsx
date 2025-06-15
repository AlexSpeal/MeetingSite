import React from 'react';

const NotFound: React.FC = () => {
    return (
        <div className="min-h-screen bg-white flex items-center justify-center px-4">
            <div className="max-w-xl text-center">
                <img
                    src="src/data/page-not-found.gif"
                    alt="Not Found Illustration"
                    className="w-64 mx-auto mb-6"
                />
                <h2 className="text-2xl font-semibold text-gray-800 mb-2">Упс! Страница не найдена</h2>
                <p className="text-gray-600 mb-6">
                    Возможно, страница была удалена или вы ввели неверный адрес.
                </p>
                <a
                    href="/"
                    className="inline-block bg-black hover:bg-gray-800 text-white font-medium py-2 px-6 rounded-lg transition-all duration-200"
                >
                    Вернуться на главную
                </a>
            </div>
        </div>
    );
};

export default NotFound;