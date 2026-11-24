import { useCallback, useEffect, useRef, useState } from "react";
import { timerApi, type TimerStateResponse } from "../../api/timer";
import { useTimerStore } from "./timerStore";

const RUNNING = new Set(["WORK_RUNNING", "BREAK_RUNNING"]);

function format(totalSeconds: number): string {
  const minute = Math.floor(totalSeconds / 60);
  const second = totalSeconds % 60;
  return `${String(minute).padStart(2, "0")}: ${String(second).padStart(
    2,
    "0"
  )}`;
}

export default function TimerPane({ blockId }: { blockId: string }) {
  const timer = useTimerStore((s) => s.timers[blockId]);
  const setTimer = useTimerStore((s) => s.setTimer);
  const [error, setError] = useState<string | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const expiryRequested = useRef(false);

  const refetch = useCallback(async () => {
    try {
      setTimer(await timerApi.getState(blockId));
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Timer fetch failed");
    }
  }, [blockId, setTimer]);

  useEffect(() => {
    if (!timer) void refetch();
  }, [timer, refetch]);

  const running = timer !== undefined && RUNNING.has(timer.state);

  useEffect(() => {
    if (!running) return;
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, [running]);

  const remaining = !timer
    ? null
    : running && timer.endAt
    ? Math.max(0, Math.floor((Date.parse(timer.endAt) - now) / 1000))
    : timer.remainingSeconds ?? 0;

  useEffect(() => {
    expiryRequested.current = false;
  }, [timer?.state, timer?.endAt]);

  useEffect(() => {
    if (running && remaining === 0 && !expiryRequested.current) {
      expiryRequested.current = true;
      void refetch();
    }
  }, [running, remaining, refetch]);

  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState === "visible") void refetch();
    };
    document.addEventListener("visibilitychange", onVisible);
    return () => document.removeEventListener("visibilitychange", onVisible);
  }, [refetch]);

  async function send(intent: (id: string) => Promise<TimerStateResponse>) {
    try {
      setTimer(await intent(blockId));
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Action failed");
    }
  }

  if (!timer) return <span className="text-slate-500 text-xs">...</span>;
  if (timer.state === "DONE") return null;

  const phase = timer.state.startsWith("BREAK") ? "Break" : "Work";

  return (
    <span className="flex items-center gap-2 text-sm">
      {timer.state !== "IDLE" && (
        <span className="font-mono">
          {phase} {format(remaining ?? 0)}
        </span>
      )}

      {timer.state === "IDLE" && (
        <button onClick={() => send(timerApi.start)}>Sttart</button>
      )}
      {timer.state === "WORK_RUNNING" && (
        <>
          <button onClick={() => send(timerApi.pause)}>Pause</button>
          <button onClick={() => send(timerApi.complete)}>Complete</button>
        </>
      )}
      {timer.state === "WORK_PAUSED" && (
        <>
          <button onClick={() => send(timerApi.resume)}>Resume</button>
          <button onClick={() => send(timerApi.complete)}>Complete</button>
        </>
      )}
      {timer.state === "BREAK_RUNNING" && (
        <>
          <button onClick={() => send(timerApi.pause)}>Pause</button>
          <button onClick={() => send(timerApi.skipBreak)}>Skip break</button>
        </>
      )}
      {timer.state === "BREAK_PAUSED" && (
        <>
          <button onClick={() => send(timerApi.resume)}>Resume</button>
          <button onClick={() => send(timerApi.skipBreak)}>Skip break</button>
        </>
      )}

      {error && <span className="text-red-400 text-xs">{error}</span>}
    </span>
  );
}
