import React, {useEffect, useState} from "react";
import {Navigate, Outlet} from "react-router-dom";
import {isAuthenticated} from "../utils/authUtils";

const ProtectedRoute: React.FC = () => {
    const [isAuth, setIsAuth] = useState<boolean | null>(null);

    useEffect(() => {
        const checkAuth = async () => {
            const auth = await isAuthenticated();
            setIsAuth(auth);
        };
        checkAuth();
    }, []);

    if (isAuth === null) {
        return <div>Loading...</div>;
    }

    return isAuth ? <Outlet/> : <Navigate to="/auth" replace/>;
};

export default ProtectedRoute;
