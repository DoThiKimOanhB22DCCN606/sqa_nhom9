import React, { createContext, useContext, useEffect, useState, useRef } from "react";
import { io } from "socket.io-client";
import { useAuth } from "./AuthContext";
import { toast } from "react-toastify";
import { useQueryClient } from "@tanstack/react-query";

const SocketContext = createContext();

export const SocketProvider = ({ children }) => {
  const { token, user } = useAuth();
  const [socket, setSocket] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const queryClient = useQueryClient();
  const socketRef = useRef(null);

  useEffect(() => {
    if (!token || !user) {
      if (socketRef.current) {
        socketRef.current.disconnect();
        socketRef.current = null;
        setSocket(null);
        setIsConnected(false);
      }
      return;
    }

    const socketInstance = io("https://4jw1qjvd-8081.asse.devtunnels.ms", {
      query: {
        token: token,
      },
      transports: ["websocket", "polling"],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionAttempts: 5,
    });

    socketRef.current = socketInstance;
    setSocket(socketInstance);

    socketInstance.on("connect", () => {
      setIsConnected(true);
    });

    socketInstance.on("disconnect", () => {
      setIsConnected(false);
    });

    socketInstance.on("connect_error", (error) => {
      console.error("Socket connection error:", error);
      setIsConnected(false);
    });

    socketInstance.on("notification", (data) => {
      console.log("🔔 [Socket] Received NEW notification from server:", data);
      
      setNotifications((prev) => [data, ...prev]);

      console.log("🔔 [Socket] Showing toast for notification:", data.title);
      toast.info(
        <div>
          <div className="font-semibold">{data.title}</div>
          {data.message && <div className="text-sm mt-1">{data.message}</div>}
        </div>,
        {
          position: "top-right",
          autoClose: 5000,
          onClick: () => {
            console.log("Notification clicked:", data);
          },
        }
      );

      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    });

    return () => {
      if (socketInstance) {
        socketInstance.off("connect");
        socketInstance.off("disconnect");
        socketInstance.off("connect_error");
        socketInstance.off("notification");
        socketInstance.disconnect();
      }
    };
  }, [token, user, queryClient]);

  const value = {
    socket,
    isConnected,
    notifications,
  };

  return (
    <SocketContext.Provider value={value}>
      {children}
    </SocketContext.Provider>
  );
};

export const useSocket = () => {
  const context = useContext(SocketContext);
  if (context === undefined) {
    throw new Error("useSocket must be used within a SocketProvider");
  }
  return context;
};
