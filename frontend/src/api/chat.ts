import { Client } from "@stomp/stompjs";

export interface ChatMessage {
  sender: string;
  content: string;
  timestamp: string;
}

export function createChatClient(
  token: string,
  handlers: {
    onConnect: () => void;
    onDisconnect: () => void;
    onMessage: (m: ChatMessage) => void;
  }
): Client {
  const client = new Client({
    brokerURL: "ws://localhost:8080/ws",
    connectHeaders: { Authorization: `Bearer ${token}` },
    onConnect: () => {
      handlers.onConnect();
      client.subscribe("/topic/lobby", (frame) => {
        handlers.onMessage(JSON.parse(frame.body));
      });
    },
    onDisconnect: handlers.onDisconnect,
    onStompError: (f) => console.error("STOMP error", f),
    onWebSocketError: (e) => console.error("WS error", e),
    onWebSocketClose: () => handlers.onDisconnect(),
  });
  return client;
}

export function sendChat(client: Client, content: string) {
  client.publish({
    destination: "/app/chat",
    body: JSON.stringify({ content: content }),
  });
}
