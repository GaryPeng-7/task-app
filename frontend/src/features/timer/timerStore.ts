import { create } from "zustand";
import type { TimerStateResponse } from "../../api/timer";

interface TimerStore {
  timers: Record<string, TimerStateResponse>;
  setTimer: (t: TimerStateResponse) => void;
}

export const useTimerStore = create<TimerStore>((set) => ({
  timers: {},
  setTimer: (t) =>
    set((s) => ({
      timers: { ...s.timers, [t.blockId]: { ...t, receivedAt: Date.now() } },
    })),
}));
