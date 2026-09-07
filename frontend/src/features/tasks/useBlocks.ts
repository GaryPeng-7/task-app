import { useCallback, useEffect, useState } from "react";
import { getBlocks, type Block } from "../../api/blocks";

export function useBlocks(date: string) {
  const [blocks, setBlocks] = useState<Block[] | null>(null);
  const [error, setError] = useState<string | null>(null);

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

  return { blocks, error, refetch };
}
