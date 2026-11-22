import { useEffect, useRef, useState } from "react";
import { useChat } from "./useChat";

export default function ChatPane() {
  const { connected, messages, sendMessage } = useChat();
  const [draft, setDraft] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const send = () => {
    if (!draft.trim()) return;
    sendMessage(draft);
    setDraft("");
  };

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 border-b border-slate-700 flex items-center gap-2">
        <span
          className={`w-2 h-2 rounded-full ${
            connected ? "bg-green-400" : "bg-red-400"
          }`}
        />
        <span className="font-semibold">Lobby</span>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-2">
        {messages.map((m, i) => (
          <div key={i}>
            <span className="text-slate-400 text-xs">
              {m.sender} ·{" "}
              {new Date(m.timestamp).toLocaleDateString([], {
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
              })}
            </span>
            <div>{m.content}</div>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      <div className="p-4 border-t border-slate-700 flex gap-2">
        <input
          className="flex-1 bg-slate-800 rounded px-3 py-2"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && send()}
          placeholder="Message the lobby..."
        />
        <button onClick={send} className="bg-blue-600 rounded px-4">
          Send
        </button>
      </div>
    </div>
  );
}
