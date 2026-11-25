import { Draggable } from "@hello-pangea/dnd";
import type { Block } from "../../api/blocks";
import { timerApi } from "../../api/timer";
import { useTimer, format } from "../timer/useTimer";

interface BlockRowProps {
  block: Block;
  index: number;
  disabled: boolean;
  onDelete: (id: string) => void;
}

export default function BlockRow({
  block,
  index,
  disabled,
  onDelete,
}: BlockRowProps) {
  const { timer, remaining, send } = useTimer(block.id);
  const active = !!timer && timer.state !== "IDLE" && timer.state !== "DONE";

  return (
    <Draggable
      draggableId={String(block.id)}
      index={index}
      isDragDisabled={disabled}
    >
      {(provided) => (
        <li
          ref={provided.innerRef}
          {...provided.draggableProps}
          {...provided.dragHandleProps}
          className={
            "group bg-slate-800 rounded px-3 py-2 flex items-center justify-between gap-2" +
            (active ? " ring-2 ring-emerald-400" : "")
          }
        >
          <span>{block.subject}</span>

          {timer?.state === "IDLE" && (
            <button className="text-sm" onClick={() => send(timerApi.start)}>
              Start
            </button>
          )}
          {active && (
            <span className="text-emerald-300 text-xs font-mono">
              {timer.state.startsWith("BREAK") ? "Break" : "Work"}{" "}
              {format(remaining ?? 0)}
            </span>
          )}

          <button
            onClick={() => onDelete(block.id)}
            className="text-red-400 hover:text-red-300 text-sm opacity-0 group-hover:opacity-100"
            aria-label={`Delete ${block.subject}`}
          >
            delete
          </button>
        </li>
      )}
    </Draggable>
  );
}
