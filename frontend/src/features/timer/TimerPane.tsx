import { useEffect, useState } from "react";
import { getBlocks, type Block } from "../../api/blocks";
import { timerApi } from "../../api/timer";
import { useTimerStore } from "./timerStore";
import { useTimer, format } from "./useTimer";
import { todayLocal } from "../tasks/dateUtils";

export default function TimerPane() {
  const timers = useTimerStore((s) => s.timers);
  const [blocks, setBlocks] = useState<Block[]>([]);

  useEffect(() => {
    getBlocks(todayLocal())
      .then(setBlocks)
      .catch(() => {});
  }, []);

  const active =
    Object.values(timers).find(
      (t) => t.state !== "IDLE" && t.state !== "DONE"
    ) ?? null;

  const { timer, remaining, send, error } = useTimer(active?.blockId ?? null);

  const subject = blocks.find((b) => b.id === active?.blockId)?.subject ?? "";

  if (!active || !timer) {
    return (
      <div className="h-full flex items-center justify-center text-slate-500">
        No active time - press Start on a block.
      </div>
    );
  }

  const phase = timer.state.startsWith("BREAK") ? "Break" : "Work";

  return (
    <div className="h-full flex flex-col items-center justify-center gap-6">
      <div className="text-slate-400 text-lg">{subject}</div>
      <div className="text-slate-500">{phase}</div>
      <div className="font-mono text-8xl">{format(remaining ?? 0)}</div>
      <div className="flex gap-3">
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
      </div>

      {error && <div className="text-red-400 text-sm">{error}</div>}
    </div>
  );
}
