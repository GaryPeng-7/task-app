import { api } from "./client";

export interface Block {
  id: string;
  subject: string;
  description: string;
  blockDate: string;
  position: number;
  remainingSeconds: number;
  status: string;
}

export function getBlocks(date: string) {
  return api<Block[]>(`/api/blocks?date=${date}`);
}
