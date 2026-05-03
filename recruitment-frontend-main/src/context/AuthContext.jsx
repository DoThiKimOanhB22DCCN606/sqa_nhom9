import React, { createContext, useState, useContext, useEffect } from "react";
import { userServices } from "../services/userServices";
import api from "../config/axios";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {

  const [token, setToken] = useState(localStorage.getItem("token"));
  const [user, setUser] = useState(null);

  const [isLoading, setIsLoading] = useState(!!token);

  useEffect(() => {
    const loadUserFromToken = async () => {
      if (token) {
        try {
          api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
          const response = await userServices.getCurrentUser();
          setUser(response.data.data.user);
        } catch (error) {
          localStorage.removeItem("token");
          setToken(null);
          setUser(null);
          delete api.defaults.headers.common["Authorization"];
        } finally {
          setIsLoading(false);
        }
      }
    };
    
    if(token) {
      loadUserFromToken();
    }
  }, []);


  const login = async (username, password) => {
    try {

      const response = await userServices.logIn({ username, password });

      const { access_token: newToken, user: userData } = response.data.data; 

      localStorage.setItem("token", newToken);
      setToken(newToken);
      setUser(userData);
      api.defaults.headers.common["Authorization"] = `Bearer ${newToken}`;

    } catch (error) {
      localStorage.removeItem("token");
      setToken(null);
      setUser(null);
      delete api.defaults.headers.common["Authorization"];
      throw error;
    }
  };

  const logout = () => {
    localStorage.removeItem("token");
    setToken(null);
    setUser(null);
    delete api.defaults.headers.common["Authorization"];
    window.location.href = '/login';
  };

  const value = {
    user,
    token,
    isLoading,
    isAuthenticated: !!token,
    login,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {!isLoading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};