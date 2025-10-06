package com.example.memorygame.service;

import com.example.memorygame.model.Card;
import com.example.memorygame.model.Player;
import com.example.memorygame.model.Room;
import com.example.memorygame.ws.GameWsService;
import com.example.memorygame.ws.TurnTimerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

@Service
public class GameService {

    private final RoomService rooms;
    private final GameWsService ws;
    private final TurnTimerService timer;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public GameService(RoomService rooms, GameWsService ws, TurnTimerService timer) {
        this.rooms = rooms; this.ws = ws; this.timer = timer;
    }

    public void revealCard(String pin, String playerId, int pos) {
        Room room = rooms.findByPin(pin).orElseThrow(() -> new IllegalArgumentException("Raum nicht gefunden"));
        synchronized (room) {
            if (room.getState() != Room.State.RUNNING) throw new IllegalStateException("Spiel nicht gestartet");
            if (!playerId.equals(room.getCurrentPlayerId())) throw new IllegalStateException("Nicht dein Zug");
            if (room.isRevealLock()) return;

            Card card = cardAt(room, pos);
            if (card == null) throw new IllegalArgumentException("Ungültige Position");
            if (card.getState() != Card.CardState.HIDDEN) return;

            if (room.getFirstRevealedPos() == null) {
                card.setState(Card.CardState.REVEALED);
                room.setFirstRevealedPos(pos);
                ws.broadcastRoomState(room);
                return;
            }

            if (room.getFirstRevealedPos() == pos) return;

            Card first = cardAt(room, room.getFirstRevealedPos());
            card.setState(Card.CardState.REVEALED);

            boolean match = first.getPairId() == card.getPairId();

            if (match) {
                first.setState(Card.CardState.MATCHED);
                card.setState(Card.CardState.MATCHED);
                room.setFirstRevealedPos(null);

                Player cur = room.getPlayersInJoinOrder().stream()
                        .filter(p -> p.getId().equals(playerId)).findFirst().orElse(null);
                if (cur != null) cur.setScore(cur.getScore() + 1);

                // Alle gefunden?
                if (allMatched(room)) {
                    room.setState(Room.State.FINISHED);
                    room.setTimeLeft(0);
                    ws.broadcastRoomState(room);
                    timer.stopCountdown(pin);
                    return;
                }

                // Gleicher Spieler bleibt, Timer reset
                room.setTimeLeft(20);
                ws.broadcastRoomState(room);
            } else {
                room.setRevealLock(true);
                ws.broadcastRoomState(room);

                scheduler.schedule(() -> {
                    synchronized (room) {
                        first.setState(Card.CardState.HIDDEN);
                        card.setState(Card.CardState.HIDDEN);
                        room.setFirstRevealedPos(null);

                        // Nächster Spieler + Timer reset
                        String next = nextPlayerId(room);
                        room.setCurrentPlayerId(next);
                        room.setTimeLeft(20);
                        room.setRevealLock(false);
                        ws.broadcastRoomState(room);
                    }
                }, 900, TimeUnit.MILLISECONDS);
            }
        }
    }

    private Card cardAt(Room room, int pos) {
        return room.getBoard().stream().filter(c -> c.getPosition() == pos).findFirst().orElse(null);
    }
    private boolean allMatched(Room room) {
        return room.getBoard().stream().allMatch(c -> c.getState() == Card.CardState.MATCHED);
    }
    private String nextPlayerId(Room room) {
        List<Player> order = room.getPlayersInJoinOrder();
        if (order.isEmpty()) return null;
        String cur = room.getCurrentPlayerId();
        int idx = 0;
        for (int i = 0; i < order.size(); i++) if (order.get(i).getId().equals(cur)) { idx = i; break; }
        return order.get((idx + 1) % order.size()).getId();
    }
}
