import { Draggable } from "@hello-pangea/dnd";
import type { Block } from "../../api/blocks";
import TimerPane from "../timer/TimerPane";

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
          className="group bg-slate-800 rounded px-3 py-2 flex items-center justify-between gap-2"
        >
          <span>{block.subject}</span>
          <TimerPane blockId={block.id} />
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
