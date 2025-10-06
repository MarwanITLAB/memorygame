package com.example.memorygame.web;

import com.example.memorygame.model.Card;
import com.example.memorygame.model.Player;
import com.example.memorygame.model.Room;
import com.example.memorygame.service.GameService;
import com.example.memorygame.service.RoomService;
import com.example.memorygame.ws.GameWsService;
import com.example.memorygame.ws.TurnTimerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService rooms;
    private final GameWsService ws;
    private final TurnTimerService timer;
    private final GameService game;

    public RoomController(RoomService rooms, GameWsService ws, TurnTimerService timer, GameService game) {
        this.rooms = rooms;
        this.ws = ws;
        this.timer = timer;
        this.game = game;
    }

    @PostMapping
    public Map<String, Object> createRoom() {
        Room room = rooms.createRoom();
        ws.broadcastRoomState(room);
        Map<String, Object> resp = new HashMap<>();
        resp.put("roomId", room.getId());
        resp.put("pin", room.getPin());
        resp.put("state", room.getState());
        return resp;
    }

    @GetMapping("/{pin}")
    public ResponseEntity<?> getRoom(@PathVariable String pin) {
        return rooms.findByPin(pin)
                .<ResponseEntity<?>>map(r -> {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("roomId", r.getId());
                    resp.put("pin", r.getPin());
                    resp.put("state", r.getState());
                    resp.put("timeLeft", r.getTimeLeft());
                    if (r.getCurrentPlayerId() != null) resp.put("currentPlayerId", r.getCurrentPlayerId());

                    List<Map<String, Object>> players = r.getPlayers().stream().map(p -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", p.getId());
                        m.put("name", p.getName());
                        m.put("score", p.getScore());
                        return m;
                    }).toList();
                    resp.put("players", players);

                    resp.put("boardSize", r.getBoardSize());
                    List<Map<String, Object>> board = r.getBoard().stream().map(c -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("pos", c.getPosition());
                        m.put("state", c.getState().name());
                        if (c.getState() != com.example.memorygame.model.Card.CardState.HIDDEN) {
                            String sym = r.getPairSymbols().get(c.getPairId());
                            if (sym != null) m.put("symbol", sym);
                        }
                        return m;
                    }).toList();
                    resp.put("board", board);

                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{pin}/join")
    public ResponseEntity<?> join(@PathVariable String pin, @RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "").trim();
        if (name.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Name fehlt"));
        try {
            Player p = rooms.addPlayer(pin, name);
            rooms.findByPin(pin).ifPresent(ws::broadcastRoomState);
            return ResponseEntity.ok(Map.of("playerId", p.getId(), "name", p.getName(), "pin", pin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{pin}/start")
    public ResponseEntity<?> start(@PathVariable String pin) {
        try {
            Room r = rooms.startGame(pin);
            ws.broadcastRoomState(r);
            timer.startCountdown(pin);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{pin}/reveal")
    public ResponseEntity<?> reveal(@PathVariable String pin, @RequestBody Map<String, Object> body) {
        Object pid = body.get("playerId");
        Object ppos = body.get("pos");
        if (!(pid instanceof String) || ppos == null)
            return ResponseEntity.badRequest().body(Map.of("error", "playerId/pos fehlen"));
        int pos;
        try {
            pos = (ppos instanceof Number) ? ((Number) ppos).intValue() : Integer.parseInt(ppos.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "pos ungültig"));
        }
        try {
            game.revealCard(pin, (String) pid, pos);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{pin}/restart")
    public ResponseEntity<?> restart(@PathVariable String pin, @RequestBody Map<String, Object> body) {
        boolean resetScores = false;
        if (body != null && body.containsKey("resetScores")) {
            Object v = body.get("resetScores");
            resetScores = (v instanceof Boolean) ? (Boolean) v : Boolean.parseBoolean(String.valueOf(v));
        }
        try {
            Room r = rooms.restartRound(pin, resetScores);
            ws.broadcastRoomState(r);
            timer.startCountdown(pin);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{pin}/stop")
    public ResponseEntity<?> stop(@PathVariable String pin) {
        timer.stopCountdown(pin);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
