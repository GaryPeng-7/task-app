import { useState } from "react";
import { useBlocks } from "./useBlocks";
import { todayLocal } from "./dateUtils";
import BlockRow from "./BlockRow";
import { DragDropContext, Droppable } from "@hello-pangea/dnd";

export default function TasksPane() {
  const {
    blocks,
    error,
    reorderError,
    reordering,
    deleteError,
    onDragEnd,
    createBlock,
    deleteBlcok,
  } = useBlocks(todayLocal());

  const [newSubject, setNewSubject] = useState("");
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  async function submitNewBlock() {
    const subject = newSubject.trim();
    if (!subject || creating) return;
    setCreating(true);
    setCreateError(null);
    try {
      await createBlock(subject);
      setNewSubject("");
    } catch (e) {
      setCreateError(e instanceof Error ? e.message : "Couldn't add block.");
    } finally {
      setCreating(false);
    }
  }

  if (error) return <div className="p-4 text-red-400 text-sm">{error}</div>;
  if (blocks === null)
    return <div className="p-4 text-slate-500 text-sm">Loading...</div>;

  return (
    <>
      <DragDropContext onDragEnd={onDragEnd}>
        <Droppable droppableId="tasks">
          {(provided) => (
            <ul
              ref={provided.innerRef}
              {...provided.droppableProps}
              className="p-4 flex flex-col gap-2"
            >
              {blocks.length === 0 && (
                <li className="text-slate-500 text-sm">No blocks for today.</li>
              )}
              {blocks.map((b, index) => (
                <BlockRow
                  key={b.id}
                  block={b}
                  index={index}
                  disabled={reordering}
                  onDelete={deleteBlcok}
                />
              ))}
              {provided.placeholder}
            </ul>
          )}
        </Droppable>
      </DragDropContext>

      <div className="px-4 pb-4 flex gap-2">
        <input
          className="flex-1 bg-slate-800 rounded px-3 py-2 text-sm"
          value={newSubject}
          onChange={(e) => setNewSubject(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submitNewBlock()}
          placeholder="New Block"
          disabled={creating}
        />
        <button
          className="bg-slate-700 rounded px-3 py-2 text-sm disabled:opacity-50"
          onClick={submitNewBlock}
          disabled={creating || !newSubject.trim()}
        >
          Add
        </button>
      </div>

      {createError && (
        <div className="px-4 pb-2 text-red-400 text-sm">{createError}</div>
      )}
      {deleteError && (
        <div className="p-2 text-red-400 text-sm">{deleteError}</div>
      )}
      {reorderError && (
        <div className="p-2 text-red-400 text-sm">{reorderError}</div>
      )}
      {reordering && (
        <div className="p-2 text-slate-500 text-sm">Saving order...</div>
      )}
    </>
  );
}
