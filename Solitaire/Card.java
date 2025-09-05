package Solitaire;
public class Card {
    public enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }
    public final Suit suit;
    public final int rank; // 1=Ace, 11=Jack, 12=Queen, 13=King
    public boolean faceUp = false;

    public Card(Suit suit, int rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public String toString() {
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        return ranks[rank-1] + " of " + suit;
    }
}
