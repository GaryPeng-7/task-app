import { create } from "zustand";
import type { TimerStateResponse } from "../../api/timer";

// What the server sent + when it arrived (client clock, for countdown anchoring)
export type StoredTimer = TimerStateResponse & { receivedAt: number };

interface TimerStore {
  timers: Record<string, StoredTimer>; // ← was Record<string, TimerStateResponse>
  setTimer: (t: TimerStateResponse) => void;
}

export const useTimerStore = create<TimerStore>((set) => ({
  timers: {},
  setTimer: (t) =>
    set((s) => ({
      timers: { ...s.timers, [t.blockId]: { ...t, receivedAt: Date.now() } },
    })),
}));
