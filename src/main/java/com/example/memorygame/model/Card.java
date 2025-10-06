package com.example.memorygame.model;

public class Card {

    public enum CardState { HIDDEN, REVEALED, MATCHED }

    private final int position;   // 0..(n*n-1)
    private final int pairId;     // 0..(pairs-1) – NICHT an Clients senden
    private CardState state = CardState.HIDDEN;

    public Card(int position, int pairId) {
        this.position = position;
        this.pairId = pairId;
    }

    public int getPosition() { return position; }
    public int getPairId()   { return pairId; }
    public CardState getState() { return state; }
    public void setState(CardState state) { this.state = state; }
}
