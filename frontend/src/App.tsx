import { useState } from "react";
import { useAuthStore } from "./stores/authStore";
import LoginPage from "./features/auth/LoginPage";
import RegisterPage from "./features/auth/RegisterPage";
import TasksPane from "./features/tasks/TasksPane";
import TimerPane from "./features/timer/TimerPane";
import ChatPane from "./features/chat/ChatPane";

export default function App() {
  const token = useAuthStore((s) => s.token);
  const logout = useAuthStore((s) => s.logout);
  const [showRegister, setShowRegister] = useState(false);

  if (!token) {
    return showRegister ? (
      <RegisterPage onSwitch={() => setShowRegister(false)} />
    ) : (
      <LoginPage onSwitch={() => setShowRegister(true)} />
    );
  }

  return (
    <div className="h-screen bg-slate-900 text-slate-100 flex">
      <aside className="w-96 shrink-0 border-r border-slate-700 overflow-y-auto flex flex-col">
        <div className="p-4 border-b border-slate-700 flex justify-between items-center">
          <span className="font-semibold">Tasks</span>
          <button
            onClick={logout}
            className="text-sm text-slate-400 hover:text-slate-200"
          >
            Log out
          </button>
        </div>
        <TasksPane />
      </aside>
      <main className="flex-1 overflow-y-auto">
        <TimerPane />
      </main>
      <aside className="w-96 shrink-0 border-l border-slate-700 overflow-y-auto">
        <ChatPane />
      </aside>
    </div>
  );
}
