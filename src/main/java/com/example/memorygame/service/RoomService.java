package com.example.memorygame.service;

import com.example.memorygame.model.Card;
import com.example.memorygame.model.Player;
import com.example.memorygame.model.Room;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet Spielräume (in-memory).
 * - Raum anlegen (mit PIN)
 * - Spieler beitreten lassen (nur in LOBBY)
 * - Spiel starten (erstellt Board, setzt ersten Spieler, Timer = 20s)
 */
@Service
public class RoomService {

    /** Räume nach PIN (z. B. "1234"). */
    private final Map<String, Room> roomsByPin = new ConcurrentHashMap<>();

    private final SecureRandom rnd = new SecureRandom();

    /** Neuen Raum mit zufälliger PIN erzeugen. */
    public Room createRoom() {
        String id = UUID.randomUUID().toString();
        String pin = generatePin(4); // 4-stellige PIN
        Room room = new Room(id, pin);
        roomsByPin.put(pin, room);
        return room;
    }

    /** Raum per PIN finden. */
    public Optional<Room> findByPin(String pin) {
        return Optional.ofNullable(roomsByPin.get(pin));
    }

    /** Spieler mit Name in LOBBY hinzufügen. */
    public Player addPlayer(String pin, String name) {
        Room room = roomsByPin.get(pin);
        if (room == null) throw new IllegalArgumentException("Raum nicht gefunden");
        if (room.getState() != Room.State.LOBBY) throw new IllegalStateException("Raum schon gestartet");

        Player p = new Player(UUID.randomUUID().toString(), name);
        room.addPlayer(p); // Reihenfolge bleibt dank LinkedHashMap im Room erhalten
        return p;
    }

    /** Spiel starten → erzeugt Board, setzt ersten Spieler, Timer auf 20s. */
    public Room startGame(String pin) {
        Room room = roomsByPin.get(pin);
        if (room == null) throw new IllegalArgumentException("Raum nicht gefunden");
        if (room.getPlayersInJoinOrder().isEmpty()) throw new IllegalStateException("Keine Spieler");

        room.setState(Room.State.RUNNING);

        // MVP: 4x4 Board erzeugen
        initBoard(room, 4);

        // Erster Spieler ist der zuerst beigetretene
        String firstId = room.getPlayersInJoinOrder().get(0).getId();
        room.setCurrentPlayerId(firstId);

        // Zugtimer starten
        room.setTimeLeft(20);

        return room;
    }

    /** Initialisiert das Memory-Board (serverseitig inkl. pairIds). */
    private void initBoard(Room room, int size) {
        room.getBoard().clear();
        room.setBoardSize(size);

        int total = size * size; // z. B. 16
        int pairs = total / 2;   // z. B. 8

        // Liste mit Paar-IDs, jede zweimal
        List<Integer> pairIds = new ArrayList<>(total);
        for (int p = 0; p < pairs; p++) {
            pairIds.add(p);
            pairIds.add(p);
        }

        // Mischen
        Collections.shuffle(pairIds, rnd);

        // Karten erzeugen
        for (int pos = 0; pos < total; pos++) {
            int pairId = pairIds.get(pos);
            room.getBoard().add(new Card(pos, pairId));
        }
    }

    // --- Helper ---

    /** Erzeugt eine numerische PIN mit gegebener Länge, kollisionsfrei innerhalb dieses Prozesses. */
    private String generatePin(int len) {
        final String digits = "0123456789";
        String pin;
        do {
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append(digits.charAt(rnd.nextInt(digits.length())));
            }
            pin = sb.toString();
        } while (roomsByPin.containsKey(pin));
        return pin;
    }

    public Room restartRound(String pin, boolean resetScores) {
        Room room = roomsByPin.get(pin);
        if (room == null) throw new IllegalArgumentException("Raum nicht gefunden");
        if (room.getPlayersInJoinOrder().isEmpty()) throw new IllegalStateException("Keine Spieler");

        // Scores zurücksetzen?
        if (resetScores) {
            room.getPlayersInJoinOrder().forEach(p -> p.setScore(0));
        }

        // Board neu, Status auf RUNNING
        room.setState(Room.State.RUNNING);
        initBoard(room, room.getBoardSize() > 0 ? room.getBoardSize() : 4);

        // erster Spieler bleibt der join-erstes
        String firstId = room.getPlayersInJoinOrder().get(0).getId();
        room.setCurrentPlayerId(firstId);
        room.setTimeLeft(20);
        return room;
    }



}
