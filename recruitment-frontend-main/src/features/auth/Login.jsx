import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import Card from "../../components/ui/Card";
import TextInput from "../../components/ui/TextInput";
import Button from "../../components/ui/Button";
import { useTranslation } from "react-i18next";
import { toast } from "react-toastify";
import { useAuth } from "../../context/AuthContext";

const Login = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const { login } = useAuth();

  const loginMutation = useMutation({
    mutationFn: async (credentials) => {
      return await login(credentials.username, credentials.password);
    },
    onSuccess: () => {
      toast.success(t("toasts.loginSuccess"));
      navigate("/");
    },
    onError: (error) => {
      const errorMessage = error.response?.data?.message || t("toasts.loginFailed");
      console.error(error);
      toast.error(errorMessage);
    },
  });

  const handleLogin = async (e) => {
    e.preventDefault();
    loginMutation.mutate({ username, password });
  };

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div
        className="flex flex-col space-y-4 w-full max-w-md
        bg-white rounded-xl shadow-md p-8 border border-[#f3f3f3]"
      >
        <h2 className="text-2xl font-bold mb-6 text-center">{t("login")}</h2>
        <form onSubmit={handleLogin} className="space-y-2">
          <TextInput
            label={t("username")}
            name="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            disabled={loginMutation.isPending}
          />
          <TextInput
            label={t("password")}
            name="password"
            value={password}
            type="password"
            onChange={(e) => setPassword(e.target.value)}
            required
            disabled={loginMutation.isPending}
          />
          <div className="flex justify-center pt-2">
            <Button
              type="submit"
              onClick={handleLogin}
              disabled={loginMutation.isPending}
            >
              {loginMutation.isPending ? t("loading") : t("login")}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Login;
