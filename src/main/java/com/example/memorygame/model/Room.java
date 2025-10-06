package com.example.memorygame.model;

import java.util.*;

public class Room {

    public enum State { LOBBY, RUNNING, FINISHED }

    // --- Grunddaten ---
    private final String id;     // UUID
    private final String pin;    // Game-PIN
    private State state = State.LOBBY;

    // Spieler in Beitrittsreihenfolge
    private final Map<String, Player> players = new LinkedHashMap<>();

    // --- Spielzustand ---
    private String currentPlayerId;    // null in der Lobby
    private int timeLeft = 0;          // Sekunden

    // Aufdecken-Status (für Züge)
    private Integer firstRevealedPos = null;  // erste Karte im aktuellen Zug
    private boolean revealLock = false;       // blockt Klicks beim Vergleichen

    // --- Board ---
    private int boardSize = 4;               // z. B. 4 => 4x4
    private final List<Card> board = new ArrayList<>();

    // Zuordnung Paar-ID -> Emoji (nur serverseitig gespeichert)
    private final Map<Integer, String> pairSymbols = new HashMap<>();

    public Room(String id, String pin) {
        this.id = id;
        this.pin = pin;
    }

    // --- Getter/Setter Grunddaten ---
    public String getId() { return id; }
    public String getPin() { return pin; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    // --- Spieler ---
    public Collection<Player> getPlayers() { return players.values(); }
    public List<Player> getPlayersInJoinOrder() { return new ArrayList<>(players.values()); }
    public void addPlayer(Player p) { players.put(p.getId(), p); }

    // --- Zug & Timer ---
    public String getCurrentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(String currentPlayerId) { this.currentPlayerId = currentPlayerId; }
    public int getTimeLeft() { return timeLeft; }
    public void setTimeLeft(int timeLeft) { this.timeLeft = timeLeft; }

    // --- Aufdecken-Zwischenzustand ---
    public Integer getFirstRevealedPos() { return firstRevealedPos; }
    public void setFirstRevealedPos(Integer p) { this.firstRevealedPos = p; }
    public boolean isRevealLock() { return revealLock; }
    public void setRevealLock(boolean revealLock) { this.revealLock = revealLock; }

    // --- Board ---
    public int getBoardSize() { return boardSize; }
    public void setBoardSize(int boardSize) { this.boardSize = boardSize; }
    public List<Card> getBoard() { return board; }

    // --- Emojis ---
    public Map<Integer, String> getPairSymbols() { return pairSymbols; }
}
