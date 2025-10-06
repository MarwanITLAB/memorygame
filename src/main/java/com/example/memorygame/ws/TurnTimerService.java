package com.example.memorygame.ws;

import com.example.memorygame.model.Player;
import com.example.memorygame.model.Room;
import com.example.memorygame.service.RoomService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class TurnTimerService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    private final RoomService rooms;
    private final GameWsService ws;

    public TurnTimerService(RoomService rooms, GameWsService ws) {
        this.rooms = rooms; this.ws = ws;
    }

    public synchronized void startCountdown(String pin) {
        stopCountdown(pin);
        ScheduledFuture<?> f = scheduler.scheduleAtFixedRate(() -> tick(pin), 1, 1, TimeUnit.SECONDS);
        tasks.put(pin, f);
    }

    public synchronized void stopCountdown(String pin) {
        var f = tasks.remove(pin);
        if (f != null) f.cancel(true);
    }

    private void tick(String pin) {
        rooms.findByPin(pin).ifPresent(room -> {
            // Wenn Spiel vorbei, Timer stoppen
            if (room.getState() != Room.State.RUNNING) {
                stopCountdown(pin);
                return;
            }

            // runterzählen
            int t = room.getTimeLeft();
            if (t <= 1) {
                // nächster Spieler + reset 20s
                List<Player> order = room.getPlayersInJoinOrder();
                if (order.isEmpty()) return;
                String cur = room.getCurrentPlayerId();
                int idx = 0;
                if (cur != null) {
                    for (int i = 0; i < order.size(); i++) {
                        if (order.get(i).getId().equals(cur)) { idx = i; break; }
                    }
                    idx = (idx + 1) % order.size();
                }
                room.setCurrentPlayerId(order.get(idx).getId());
                room.setTimeLeft(20);
            } else {
                room.setTimeLeft(t - 1);
            }
            ws.broadcastRoomState(room);
        });
    }
}
