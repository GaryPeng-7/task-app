import { useBlocks } from "./useBlocks";
import { todayLocal } from "./dateUtils";

export default function TasksPane() {
  const { blocks, error } = useBlocks(todayLocal());

  if (error) return <div className="p-4 text-red-400 text-sm">{error}</div>;
  if (blocks === null)
    return <div className="p-4 text-slate-500 text-sm">Loading...</div>;
  if (blocks.length === 0)
    return (
      <div className="p-4 text-slate-500 text-sm">No blocks for today.</div>
    );

  return (
    <ul className="p-4 flex flex-col gap-2">
      {blocks.map((b) => (
        <li key={b.id} className="bg-slate-800 rounded px-3 py-2">
          {b.subject}
        </li>
      ))}
    </ul>
  );
}
