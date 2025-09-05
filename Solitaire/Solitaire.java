package Solitaire;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Stack;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Solitaire extends JFrame {
    public Solitaire() {
        setTitle("Solitaire");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        add(new SolitairePanel());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Solitaire::new);
    }
}

class SolitairePanel extends JPanel {
    private final int CARD_W = 60, CARD_H = 90;
    private final int PILE_SPACING = 80, TABLEAU_Y = 150, FOUNDATION_Y = 20, STOCK_X = 20;
    private Deck deck = new Deck();
    private List<Stack<Card>> tableau = new ArrayList<>();
    private List<Stack<Card>> foundations = new ArrayList<>();
    private Stack<Card> stock = new Stack<>();
    private Stack<Card> waste = new Stack<>();

    // Drag state
    private int dragPile = -1, dragIndex = -1, dragOffsetX = 0, dragOffsetY = 0;
    private boolean draggingWaste = false;
    private List<Card> draggingCards = null;
    private Point dragPoint = null;

    public SolitairePanel() {
        setPreferredSize(new Dimension(7 * PILE_SPACING + 100, 600));
        setBackground(new Color(0, 120, 0));
        // Setup tableau
        for (int i = 0; i < 7; i++) tableau.add(new Stack<>());
        for (int i = 0; i < 4; i++) foundations.add(new Stack<>());
        // Deal cards
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j <= i; j++) {
                Card c = deck.draw();
                if (j == i) c.faceUp = true;
                tableau.get(i).push(c);
            }
        }
        while (!deck.isEmpty()) stock.push(deck.draw());

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragPile = dragIndex = -1;
                draggingWaste = false;
                draggingCards = null;
                dragPoint = e.getPoint();
                // Stock click: flip to waste or recycle
                if (new Rectangle(STOCK_X, FOUNDATION_Y, CARD_W, CARD_H).contains(e.getPoint())) {
                    if (!stock.isEmpty()) {
                        Card c = stock.pop();
                        c.faceUp = true;
                        waste.push(c);
                        repaint();
                        return;
                    } else if (!waste.isEmpty()) {
                        while (!waste.isEmpty()) {
                            Card c = waste.pop();
                            c.faceUp = false;
                            stock.push(c);
                        }
                        repaint();
                        return;
                    }
                }
                // Waste click: drag top card
                if (!waste.isEmpty()) {
                    int wx = STOCK_X + CARD_W + 20, wy = FOUNDATION_Y;
                    if (dragPoint.x >= wx && dragPoint.x <= wx + CARD_W && dragPoint.y >= wy && dragPoint.y <= wy + CARD_H) {
                        draggingWaste = true;
                        draggingCards = Collections.singletonList(waste.peek());
                        dragOffsetX = dragPoint.x - wx;
                        dragOffsetY = dragPoint.y - wy;
                        return;
                    }
                }
                // Tableau drag
                for (int i = 0; i < 7; i++) {
                    Stack<Card> pile = tableau.get(i);
                    int x = STOCK_X + i * PILE_SPACING;
                    for (int j = pile.size() - 1; j >= 0; j--) {
                        Card c = pile.get(j);
                        int y = TABLEAU_Y + j * 35;
                        if (c.faceUp && dragPoint.x >= x && dragPoint.x <= x + CARD_W && dragPoint.y >= y && dragPoint.y <= y + CARD_H) {
                            dragPile = i;
                            dragIndex = j;
                            draggingCards = new ArrayList<>(pile.subList(j, pile.size()));
                            dragOffsetX = dragPoint.x - x;
                            dragOffsetY = dragPoint.y - y;
                            return;
                        }
                    }
                }
                // Foundation drag (top card only)
                for (int i = 0; i < 4; i++) {
                    Stack<Card> pile = foundations.get(i);
                    int x = STOCK_X + (i + 3) * (CARD_W + 20);
                    int y = FOUNDATION_Y;
                    if (!pile.isEmpty() && dragPoint.x >= x && dragPoint.x <= x + CARD_W && dragPoint.y >= y && dragPoint.y <= y + CARD_H) {
                        draggingCards = Collections.singletonList(pile.peek());
                        dragPile = 100 + i; // 100+index for foundation
                        dragIndex = pile.size() - 1;
                        dragOffsetX = dragPoint.x - x;
                        dragOffsetY = dragPoint.y - y;
                        return;
                    }
                }
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingCards != null) {
                    dragPoint = e.getPoint();
                    repaint();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingCards != null) {
                    // Try to drop on foundations
                    for (int i = 0; i < 4; i++) {
                        int x = STOCK_X + (i + 3) * (CARD_W + 20);
                        int y = FOUNDATION_Y;
                        Rectangle pileRect = new Rectangle(x, y, CARD_W, CARD_H);
                        if (pileRect.contains(e.getPoint())) {
                            Card moving = draggingCards.get(0);
                            Stack<Card> pile = foundations.get(i);
                            Card top = pile.isEmpty() ? null : pile.peek();
                            if (draggingCards.size() == 1 && isValidFoundationMove(top, moving)) {
                                // Remove from source
                                if (draggingWaste) waste.pop();
                                else if (dragPile >= 0 && dragPile < 7) {
                                    Stack<Card> src = tableau.get(dragPile);
                                    src.remove(src.size() - 1);
                                    if (!src.isEmpty() && !src.peek().faceUp) src.peek().faceUp = true;
                                } else if (dragPile >= 100) {
                                    foundations.get(dragPile - 100).pop();
                                }
                                pile.push(moving);
                                draggingCards = null;
                                dragPile = dragIndex = -1;
                                repaint();
                                return;
                            }
                        }
                    }
                    // Try to drop on tableau
                    for (int i = 0; i < 7; i++) {
                        int x = STOCK_X + i * PILE_SPACING;
                        int y = TABLEAU_Y + tableau.get(i).size() * 20;
                        Rectangle pileRect = new Rectangle(x, y, CARD_W, CARD_H);
                        Card moving = draggingCards.get(0);
                        Card top = tableau.get(i).isEmpty() ? null : tableau.get(i).peek();
                        if (pileRect.contains(e.getPoint()) && isValidTableauMove(top, moving)) {
                            // Remove from source
                            if (draggingWaste) waste.pop();
                            else if (dragPile >= 0 && dragPile < 7) {
                                List<Card> movingCards = new ArrayList<>(tableau.get(dragPile).subList(dragIndex, tableau.get(dragPile).size()));
                                tableau.get(i).addAll(movingCards);
                                for (int k = tableau.get(dragPile).size() - 1; k >= dragIndex; k--)
                                    tableau.get(dragPile).remove(k);
                                if (!tableau.get(dragPile).isEmpty() && !tableau.get(dragPile).peek().faceUp)
                                    tableau.get(dragPile).peek().faceUp = true;
                            } else if (dragPile >= 100) {
                                foundations.get(dragPile - 100).pop();
                            }
                            if (draggingWaste) tableau.get(i).add(moving);
                            draggingCards = null;
                            dragPile = dragIndex = -1;
                            repaint();
                            return;
                        }
                    }
                    // Snap back if not dropped on valid pile
                    draggingCards = null;
                    dragPile = dragIndex = -1;
                    repaint();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }
    // ...existing code...

    private boolean isValidFoundationMove(Card top, Card moving) {
        if (top == null) return moving.rank == 1; // Ace
        return (top.suit == moving.suit) && (moving.rank == top.rank + 1);
    }

    private boolean isValidTableauMove(Card top, Card moving) {
        if (top == null) return moving.rank == 13; // Only King on empty
        boolean altColor = ((top.suit == Card.Suit.HEARTS || top.suit == Card.Suit.DIAMONDS) != (moving.suit == Card.Suit.HEARTS || moving.suit == Card.Suit.DIAMONDS));
        return altColor && (top.rank == moving.rank + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw stock
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(STOCK_X, FOUNDATION_Y, CARD_W, CARD_H);
        if (!stock.isEmpty()) drawCardBack(g, STOCK_X, FOUNDATION_Y);
        // Draw waste
        if (!waste.isEmpty()) drawCard(g, waste.peek(), STOCK_X + CARD_W + 20, FOUNDATION_Y);
        // Draw foundations
        for (int i = 0; i < 4; i++) {
            int x = STOCK_X + (i + 3) * (CARD_W + 20);
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(x, FOUNDATION_Y, CARD_W, CARD_H);
            if (!foundations.get(i).isEmpty()) drawCard(g, foundations.get(i).peek(), x, FOUNDATION_Y);
        }
        // Draw tableau
        for (int i = 0; i < 7; i++) {
            Stack<Card> pile = tableau.get(i);
            int x = STOCK_X + i * PILE_SPACING;
            for (int j = 0; j < pile.size(); j++) {
                Card c = pile.get(j);
                int y = TABLEAU_Y + j * 35;
                if (draggingCards != null && i == dragPile && j >= dragIndex) continue;
                if (c.faceUp) drawCard(g, c, x, y);
                else drawCardBack(g, x, y);
            }
        }
        // Draw dragging cards
        if (draggingCards != null && dragPoint != null) {
            int x = dragPoint.x - dragOffsetX;
            int y = dragPoint.y - dragOffsetY;
            for (int k = 0; k < draggingCards.size(); k++) {
                drawCard(g, draggingCards.get(k), x, y + k * 35);
            }
        }
    }

    private void drawCard(Graphics g, Card c, int x, int y) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, CARD_W, CARD_H, 10, 10);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, CARD_W, CARD_H, 10, 10);
        // ASCII art representation
        String rank = c.toString().split(" ")[0];
        String suitSymbol = getAsciiSuit(c.suit);
        String[] art = new String[] {
            "+------+",
            String.format("|%-2s   |", rank),
            "|      |",
            String.format("|  %s   |", suitSymbol),
            "|      |",
            String.format("|   %-2s|", rank),
            "+------+"
        };
        // Ensure all lines are exactly 8 chars
        for (int i = 0; i < art.length; i++) {
            if (art[i].length() < 8) art[i] = art[i] + " ".repeat(8 - art[i].length());
            else if (art[i].length() > 8) art[i] = art[i].substring(0, 8);
        }
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor((c.suit == Card.Suit.HEARTS || c.suit == Card.Suit.DIAMONDS) ? Color.RED : Color.BLACK);
        int artHeight = art.length * 12; // 12px per line
        int yOffset = (CARD_H - artHeight) / 2 + 12; // Center vertically, start a bit lower
        for (int i = 0; i < art.length; i++) {
            g.drawString(art[i], x + 4, y + yOffset + i * 12);
        }
    }

    private String getAsciiSuit(Card.Suit suit) {
        switch (suit) {
            case HEARTS: return "♥";
            case DIAMONDS: return "♦";
            case CLUBS: return "♣";
            case SPADES: return "♠";
            default: return "?";
        }
    }

    private void drawCardBack(Graphics g, int x, int y) {
        g.setColor(Color.BLUE.darker());
        g.fillRoundRect(x, y, CARD_W, CARD_H, 10, 10);
        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y, CARD_W, CARD_H, 10, 10);
    }
}
