import { useState } from "react";
import { useAuthStore } from "./stores/authStore";
import LoginPage from "./features/auth/LoginPage";
import RegisterPage from "./features/auth/RegisterPage";

export default function App() {
  const token = useAuthStore((s) => s.token);
  const [showRegister, setShowRegister] = useState(false);

  if (!token) {
    return showRegister ? (
      <RegisterPage onSwitch={() => setShowRegister(false)} />
    ) : (
      <LoginPage onSwitch={() => setShowRegister(true)} />
    );
  }
  return (
    <div className="p-8">
      <p>Logged in!</p>
      <button
        onClick={() => useAuthStore.getState().logout()}
        className="bg-slate-700 px-3 py-1 rounded"
      >
        Log out
      </button>
    </div>
  );
}
