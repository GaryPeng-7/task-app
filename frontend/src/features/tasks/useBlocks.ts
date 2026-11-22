import { useCallback, useEffect, useState } from "react";
import {
  getBlocks,
  type Block,
  reorderBlocks,
  createBlock as createBlockApi,
  deleteBlcok as deleteBlcokApi,
} from "../../api/blocks";
import type { DropResult } from "@hello-pangea/dnd";

export function useBlocks(date: string) {
  const [blocks, setBlocks] = useState<Block[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reorderError, setReorderError] = useState<string | null>(null);
  const [reordering, setReordering] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const refetch = useCallback(() => {
    setBlocks(null);
    setError(null);
    getBlocks(date)
      .then(setBlocks)
      .catch(() => setError("Could not load blocks. Is the backend running?"));
  }, [date]);

  useEffect(() => {
    refetch();
  }, [refetch]);

  async function onDragEnd(result: DropResult) {
    const { source, destination } = result;
    if (!destination || source.index === destination.index || !blocks) return;

    const snapshot = blocks;
    const next = [...blocks];
    const [moved] = next.splice(source.index, 1);
    next.splice(destination.index, 0, moved);

    setBlocks(next);
    setReordering(true);
    try {
      await reorderBlocks(
        date,
        next.map((b) => b.id)
      );
    } catch (e) {
      setBlocks(snapshot);
      setReorderError((e as Error).message);
      setTimeout(() => setReorderError(null), 4000);
    } finally {
      setReordering(false);
    }
  }

  async function createBlock(subject: string) {
    await createBlockApi(date, subject);
    const fresh = await getBlocks(date);
    setBlocks(fresh);
  }

  async function deleteBlcok(id: string) {
    if (!blocks) return;
    const snapshot = blocks;
    setBlocks(blocks.filter((b) => b.id !== id));
    setDeleteError(null);
    try {
      await deleteBlcokApi(id);
    } catch (e) {
      setBlocks(snapshot);
      setDeleteError((e as Error).message);
      setTimeout(() => setDeleteError(null), 4000);
    }
  }

  return {
    blocks,
    error,
    reorderError,
    reordering,
    deleteError,
    onDragEnd,
    createBlock,
    deleteBlcok,
    refetch,
  };
}
