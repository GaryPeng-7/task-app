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

export async function reorderBlocks(date: string, orderIds: string[]) {
  return api<void>(`/api/blocks/reorder`, {
    method: "PATCH",
    body: JSON.stringify({ blockDate: date, orderIds }),
  });
}

export function createBlock(date: string, subject: string) {
  return api<Block>(`/api/blocks`, {
    method: "POST",
    body: JSON.stringify({ blockDate: date, subject }),
  });
}

export function deleteBlcok(id: string) {
  return api<void>(`/api/blocks/${id}`, { method: "DELETE" });
}
