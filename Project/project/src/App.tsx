import React, {useState} from 'react';
import {MeetingProvider} from './context/MeetingContext';
import Header from './components/Header';
import Calendar from './components/Calendar';
import MeetingForm from './components/MeetingForm';
import MeetingList from './components/MeetingList';
import UserProfile from './components/UserProfile';
import AuthPage from './components/AuthPage';
import {BrowserRouter as Router, Navigate, Route, Routes} from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import NotFound from './components/NotFound';

const Dashboard: React.FC = () => {
    const [showMeetingForm, setShowMeetingForm] = useState(false);
    const [showUserProfile, setShowUserProfile] = useState(false);

    return (
        <div className="min-h-screen bg-gray-100">
            <Header
                onCreateMeeting={() => setShowMeetingForm(true)}
                onViewProfile={() => setShowUserProfile(true)}
            />
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="space-y-8">
                    <div>
                        <h2 className="text-xl font-semibold text-gray-800 mb-4">Календарь</h2>
                        <Calendar/>
                    </div>
                    <div>
                        <MeetingList type="all" title="Ваши встречи"/>
                    </div>
                </div>
            </main>
            {showMeetingForm && <MeetingForm onClose={() => setShowMeetingForm(false)}/>}
            {showUserProfile && <UserProfile onClose={() => setShowUserProfile(false)}/>}
        </div>
    );
};

function App() {
    return (
        <MeetingProvider>
            <Router>
                <Routes>
                    <Route path="/auth" element={<AuthPage/>}/>
                    <Route element={<ProtectedRoute/>}>
                        <Route path="/dashboard" element={<Dashboard/>}/>
                        <Route path="/" element={<Navigate to="/dashboard" replace/>}/>
                    </Route>
                    <Route path="*" element={<NotFound/>}/>
                </Routes>
            </Router>
        </MeetingProvider>
    );
}

export default App;
