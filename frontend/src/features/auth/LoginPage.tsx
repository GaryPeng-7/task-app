import { useState } from "react";
import { login } from "../../api/auth";
import { useAuthStore } from "../../stores/authStore";
import { ApiError } from "../../api/client";

export default function LoginPage({ onSwitch }: { onSwitch: () => void }) {
  const setToken = useAuthStore((s) => s.setToken);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!email || !password) {
      setError("Email and password are required");
      return;
    }
    setBusy(true);
    try {
      const res = await login({ email, password });
      setToken(res.token);
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 401
          ? "Invalid email or password"
          : "Something went wrong - is the backend up?"
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex items-center justify-center">
      <form onSubmit={handleSubmit} className="w-80 flex flex-col gap-3">
        <h1 className="text-2xl font-bold mb-2">Log in</h1>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Email"
          className="bg-slate-800 rounded px-3 py-2 outline-none focus:ring-2 ring-blue-500"
        />
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Password"
          className="bg-slate-800 rounded px-3 py-2 outline-none focus:ring-2 ring-blue-500"
        />
        {error && <p className="text-red-400 text-sm">{error}</p>}
        <button
          disabled={busy}
          className="bg-blue-600 rounded py-2 disabled:opacity-50"
        >
          {busy ? "Logging in…" : "Log in"}
        </button>
        <button
          type="button"
          onClick={onSwitch}
          className="text-sm text-slate-400"
        >
          No account? Register
        </button>
      </form>
    </div>
  );
}
