package com.example.memorygame.ws;

import com.example.memorygame.model.Card;
import com.example.memorygame.model.Room;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameWsService {

    private final SimpMessagingTemplate messaging;

    public GameWsService(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void broadcastRoomState(Room room) {
        // Players
        List<Map<String, Object>> players = room.getPlayers().stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("score", p.getScore());
            return m;
        }).toList();

        // Board – pos/state immer, symbol NUR wenn nicht hidden
        List<Map<String, Object>> board = room.getBoard().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("pos", c.getPosition());
            m.put("state", c.getState().name());
            if (c.getState() != Card.CardState.HIDDEN) {
                String sym = room.getPairSymbols().get(c.getPairId());
                if (sym != null) m.put("symbol", sym);
            }
            return m;
        }).toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ROOM_STATE");
        payload.put("pin", room.getPin());
        payload.put("state", room.getState().name());
        payload.put("timeLeft", room.getTimeLeft());
        payload.put("players", players);
        payload.put("boardSize", room.getBoardSize());
        payload.put("board", board);
        if (room.getCurrentPlayerId() != null) {
            payload.put("currentPlayerId", room.getCurrentPlayerId());
        }

        messaging.convertAndSend("/topic/room." + room.getPin(), payload);
    }
}
