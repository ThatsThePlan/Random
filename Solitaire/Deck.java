package Solitaire;
import java.util.*;

public class Deck {
    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        for (Card.Suit suit : Card.Suit.values())
            for (int r = 1; r <= 13; r++)
                cards.add(new Card(suit, r));
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card draw() {
        if (cards.isEmpty()) return null;
        return cards.remove(cards.size() - 1);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
