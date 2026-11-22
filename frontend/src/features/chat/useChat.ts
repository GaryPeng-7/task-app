import { useEffect, useRef, useState } from "react";
import type { Client } from "@stomp/stompjs";
import { useAuthStore } from "../../stores/authStore";
import { createChatClient, sendChat, type ChatMessage } from "../../api/chat";

export function useChat() {
  const token = useAuthStore((s) => s.token);
  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);

  useEffect(() => {
    if (!token) return;

    const client = createChatClient(token, {
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
      onMessage: (m) => setMessages((prev) => [...prev, m]),
    });
    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [token]);

  const sendMessage = (content: string) => {
    if (clientRef.current) sendChat(clientRef.current, content);
  };

  return { connected, messages, sendMessage };
}
