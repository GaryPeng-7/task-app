import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import { getMe } from "../../api/auth";
import type { TimerStateResponse } from "../../api/timer";
import { useAuthStore } from "../../stores/authStore";
import { useTimerStore } from "./timerStore";

export function useTimerSocket() {
  const token = useAuthStore((state) => state.token);
  const setTimer = useTimerStore((state) => state.setTimer);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!token) return;

    let disposed = false;

    getMe()
      .then((user) => {
        if (disposed) return;

        const stomp = new Client({
          brokerURL: "ws://localhost:8080/ws",
          connectHeaders: {
            Authorization: `Bearer ${token}`,
          },
          onConnect: () => {
            stomp.subscribe(`/topic/users/${user.id}/timer`, (message) => {
              const timer = JSON.parse(message.body) as TimerStateResponse;

              setTimer(timer);
            });
          },
          onStompError: (frame) => {
            console.error("Timer STOMP error", frame);
          },
          onWebSocketError: (event) => {
            console.error("Timer WebSocket error", event);
          },
        });

        clientRef.current = stomp;
        stomp.activate();
      })
      .catch((error) => {
        if (!disposed) {
          console.error("Could not initialize timer socket", error);
        }
      });

    return () => {
      disposed = true;
      void clientRef.current?.deactivate();
      clientRef.current = null;
    };
  }, [token, setTimer]);
}
