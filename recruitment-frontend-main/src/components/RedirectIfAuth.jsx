import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const RedirectIfAuth = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  // if (isLoading) {
  //   return <div>Loading authentication...</div>;
  // }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }
  return children;
};

export default RedirectIfAuth;