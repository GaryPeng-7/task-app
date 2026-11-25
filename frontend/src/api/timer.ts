import { api } from "./client";

export type TimerState =
  | "IDLE"
  | "WORK_RUNNING"
  | "WORK_PAUSED"
  | "BREAK_RUNNING"
  | "BREAK_PAUSED"
  | "DONE";

export interface TimerStateResponse {
  state: TimerState;
  endAt: string | null;
  remainingSeconds: number | null;
  serverRemainingSeconds: number;
  workSeconds: number;
  breakSeconds: number;
  blockId: string;
}

export const timerApi = {
  getState: (blockId: string) =>
    api<TimerStateResponse>(`/api/blocks/${blockId}/timer`),
  start: (blockId: string) =>
    api<TimerStateResponse>(`/api/blocks/${blockId}/timer/start`, {
      method: "POST",
    }),
  pause: (blockId: string) =>
    api<TimerStateResponse>(`/api/blocks/${blockId}/timer/pause`, {
      method: "POST",
    }),
  resume: (blockId: string) =>
    api<TimerStateResponse>(`/api/blocks/${blockId}/timer/resume`, {
      method: "POST",
    }),
  complete: (blockId: string) =>
    api<TimerStateResponse>(`/api/blocks/${blockId}/timer/complete`, {
      method: "POST",
    }),
  skipBreak: (blockId: string) =>
    api<TimerStateResponse>(`/api/blocks/${blockId}/timer/skip`, {
      method: "POST",
    }),
};
