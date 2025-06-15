import React, {FormEvent, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useMeetingContext} from '../context/MeetingContext';

const AuthPage: React.FC = () => {
    const [activeTab, setActiveTab] = useState<'login' | 'register'>('login');
    const [loginUsername, setLoginUsername] = useState<string>('');
    const [loginPassword, setLoginPassword] = useState<string>('');
    const [loginError, setLoginError] = useState<string>('');
    const [registerUsername, setRegisterUsername] = useState<string>('');
    const [registerPassword, setRegisterPassword] = useState<string>('');
    const [registerError, setRegisterError] = useState<string>('');

    const navigate = useNavigate();
    const {login, currentUser} = useMeetingContext();

    useEffect(() => {
        if (currentUser) {
            navigate('/dashboard');
        }
    }, [currentUser, navigate]);

    const handleLoginSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setLoginError('');
        const data = {username: loginUsername, password: loginPassword};

        try {
            const response = await fetch('http://localhost:8189/auth', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(data),
            });
            const result = await response.json();
            if (response.status === 200) {
                await login(result.token);
                navigate('/dashboard');
            } else {
                setLoginError(result.message || 'Неверное имя пользователя или пароль.');
            }
        } catch (error) {
            setLoginError('Произошла ошибка. Попробуйте снова.');
        }
    };

    const handleRegisterSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setRegisterError('');
        const data = {username: registerUsername, password: registerPassword};

        try {
            const response = await fetch('http://localhost:8189/signup', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(data),
            });
            const result = await response.json();
            if (response.status === 200) {
                await login(result.token);
                navigate('/dashboard');

            } else {
                setRegisterError(result.message || 'Пользователь с таким именем уже существует.');
            }
        } catch (error) {
            setRegisterError('Произошла ошибка. Попробуйте снова.');
        }
    };

    return (
        <>
            <style>{`
        body {
          margin: 0;
          padding: 0;
          font-family: 'Arial', sans-serif;
          background: #f4f4f9;
          display: flex;
          justify-content: center;
          align-items: center;
          height: 100vh;
        }
        .auth-container {
          background: rgba(255, 255, 255, 0.95);
          backdrop-filter: blur(12px);
          box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
          border-radius: 20px;
          padding: 40px;
          width: 100%;
          max-width: 450px;
          color: #333;
          animation: fadeIn 0.5s ease-in-out;
          text-align: center;
        }
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(-20px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .tabs {
          display: flex;
          margin-bottom: 30px;
          border-bottom: 2px solid rgba(0, 0, 0, 0.1);
        }
        .tab-button {
          flex: 1;
          padding: 14px;
          background: transparent;
          border: none;
          cursor: pointer;
          font-size: 18px;
          transition: all 0.3s ease;
          color: #666;
        }
        .tab-button:hover {
          color: #007bff;
        }
        .tab-button.active {
          border-bottom: 3px solid #007bff;
          font-weight: bold;
          color: #007bff;
        }
        .form-group {
          margin-bottom: 20px;
          text-align: left;
        }
        .form-group label {
          display: block;
          font-size: 14px;
          margin-bottom: 5px;
          color: #444;
        }
        .form-group input {
          width: 100%;
          padding: 14px;
          border-radius: 8px;
          border: 1px solid #ccc;
          font-size: 16px;
          transition: box-shadow 0.3s ease, border-color 0.3s ease;
        }
        .form-group input:focus {
          border-color: #007bff;
          box-shadow: 0 0 12px rgba(0, 123, 255, 0.3);
        }
        .submit-button {
          width: 100%;
          padding: 16px;
          font-size: 18px;
          border: none;
          border-radius: 8px;
          background: #007bff;
          color: #fff;
          cursor: pointer;
          transition: transform 0.3s ease, box-shadow 0.3s ease;
        }
        .submit-button:hover {
          transform: scale(1.05);
          box-shadow: 0 4px 20px rgba(0, 123, 255, 0.3);
        }
        .error-message {
          background: rgba(255, 0, 0, 0.1);
          padding: 12px;
          border-radius: 8px;
          text-align: center;
          margin-bottom: 20px;
          color: red;
          animation: fadeIn 0.5s ease-in-out;
        }
      `}</style>

            <div className="auth-container">
                <div className="tabs">
                    <button
                        className={`tab-button ${activeTab === 'login' ? 'active' : ''}`}
                        onClick={() => setActiveTab('login')}
                    >
                        Вход
                    </button>
                    <button
                        className={`tab-button ${activeTab === 'register' ? 'active' : ''}`}
                        onClick={() => setActiveTab('register')}
                    >
                        Регистрация
                    </button>
                </div>

                {activeTab === 'login' && (
                    <form onSubmit={handleLoginSubmit}>
                        {loginError && <div className="error-message">{loginError}</div>}
                        <div className="form-group">
                            <label htmlFor="loginUsername">Имя пользователя</label>
                            <input
                                type="text"
                                id="loginUsername"
                                placeholder="Введите имя"
                                value={loginUsername}
                                onChange={(e) => setLoginUsername(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="loginPassword">Пароль</label>
                            <input
                                type="password"
                                id="loginPassword"
                                placeholder="Введите пароль"
                                value={loginPassword}
                                onChange={(e) => setLoginPassword(e.target.value)}
                                required
                            />
                        </div>
                        <button type="submit" className="submit-button">Войти</button>
                    </form>
                )}

                {activeTab === 'register' && (
                    <form onSubmit={handleRegisterSubmit}>
                        {registerError && <div className="error-message">{registerError}</div>}
                        <div className="form-group">
                            <label htmlFor="registerUsername">Имя пользователя</label>
                            <input
                                type="text"
                                id="registerUsername"
                                placeholder="Введите имя"
                                value={registerUsername}
                                onChange={(e) => setRegisterUsername(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="registerPassword">Пароль</label>
                            <input
                                type="password"
                                id="registerPassword"
                                placeholder="Введите пароль"
                                value={registerPassword}
                                onChange={(e) => setRegisterPassword(e.target.value)}
                                required
                            />
                        </div>
                        <button type="submit" className="submit-button">Зарегистрироваться</button>
                    </form>
                )}
            </div>
        </>
    );
};

export default AuthPage;