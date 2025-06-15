export const isAuthenticated = async (): Promise<boolean> => {
    const token = localStorage.getItem("token");
    if (!token) return false;

    try {
        const response = await fetch("http://localhost:8189/secured/checkToken", {
            headers: {Authorization: `Bearer ${token}`},
        });

        if (response.status === 401) {
            console.warn("Токен недействителен (401 Unauthorized).");
            localStorage.removeItem("token");
            return false;
        }

        return response.ok;
    } catch (e) {
        console.error("Ошибка при проверке токена:", e);
        return false;
    }
};
  