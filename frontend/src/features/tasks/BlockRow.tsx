import { Draggable } from "@hello-pangea/dnd";
import type { Block } from "../../api/blocks";
import { timerApi } from "../../api/timer";
import { useTimer, format } from "../timer/useTimer";
import { useTimerStore } from "../timer/timerStore";

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

  const anyOtherActive = useTimerStore((s) =>
    Object.values(s.timers).some(
      (t) => t.blockId !== block.id && t.state !== "IDLE" && t.state !== "DONE"
    )
  );

  const isFinished =
    block.status === "FINISHED" ||
    timer?.state === "BREAK_RUNNING" ||
    timer?.state === "BREAK_PAUSED" ||
    timer?.state === "DONE";

  const isTimerActive =
    timer != null && timer.state !== "IDLE" && timer.state !== "DONE";

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
            "group rounded px-3 py-2 flex items-center justify-between gap-2 transition-colors " +
            (isFinished
              ? "bg-purple-900/40 ring-2 ring-purple-400"
              : isTimerActive
              ? "bg-slate-800 ring-2 ring-emerald-400"
              : "bg-slate-800")
          }
        >
          <span>{block.subject}</span>

          {timer?.state === "IDLE" && !anyOtherActive && (
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
