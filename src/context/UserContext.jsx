import { createContext, useContext, useEffect, useState } from "react";
import { api } from "@/services/api";

const UserContext = createContext(null);

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);

  const loadUser = async () => {
    const token = localStorage.getItem("token");

    if (!token) {
      setUser(null);
      return;
    }

    try {
      const res = await api.get("/auth/me");

      setUser(res.data);
      // localStorage.setItem("user", JSON.stringify(res.data));
    } catch (err) {
      console.error("Invalid session");
      localStorage.clear();
      setUser(null);
    }
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
  };

  useEffect(() => {
    loadUser();
  }, []);

  return (
    <UserContext.Provider value={{ user, loadUser, logout }}>
      {children}
    </UserContext.Provider>
  );
};

export const useUser = () => useContext(UserContext);
