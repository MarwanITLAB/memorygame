package com.example.memorygame.model;

public class Player {
    private final String id;
    private final String name;
    private int score = 0;
    private boolean connected = true;

    public Player(String id, String name) { this.id = id; this.name = name; }
    public String getId() { return id; }
    public String getName() { return name; }
    public int getScore() { return score; }
    public void setScore(int s) { score = s; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean c) { connected = c; }
}
