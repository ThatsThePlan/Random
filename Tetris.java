import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Tetris extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private StartMenuPanel menuPanel;
    private JPanel gameContainer;
    private TetrisPanel gamePanel;
    private SidebarPanel sidebar;
    private int highScore = 0;

    public Tetris() {
        setTitle("Tetris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        menuPanel = new StartMenuPanel();
        gamePanel = new TetrisPanel();
        sidebar = new SidebarPanel(gamePanel);
        gamePanel.setSidebar(sidebar);
        sidebar.setEndListener(() -> endGame());

        gameContainer = new JPanel(new BorderLayout());
        gameContainer.add(gamePanel, BorderLayout.CENTER);
        gameContainer.add(sidebar, BorderLayout.EAST);

        mainPanel.add(menuPanel, "menu");
        mainPanel.add(gameContainer, "game");
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startGame() {
        gamePanel.startGame();
        cardLayout.show(mainPanel, "game");
        // Ensure the game panel receives keyboard focus
        SwingUtilities.invokeLater(() -> gamePanel.requestFocusInWindow());
    }

    private void endGame() {
        int score = gamePanel.getScore();
        if (score > highScore) highScore = score;
        menuPanel.setHighScore(highScore);
        cardLayout.show(mainPanel, "menu");
    }

    class StartMenuPanel extends JPanel {
        private JLabel highScoreLabel;
        public StartMenuPanel() {
            setPreferredSize(new Dimension(440, 600));
            setBackground(Color.LIGHT_GRAY);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(Box.createVerticalGlue());
            JLabel title = new JLabel("TETRIS");
            title.setFont(new Font("Arial", Font.BOLD, 48));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(title);
            add(Box.createVerticalStrut(40));
            highScoreLabel = new JLabel("High Score: 0");
            highScoreLabel.setFont(new Font("Arial", Font.BOLD, 24));
            highScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(highScoreLabel);
            add(Box.createVerticalStrut(40));
            JButton startBtn = new JButton("Start Game");
            startBtn.setFont(new Font("Arial", Font.BOLD, 24));
            startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            startBtn.addActionListener(e -> startGame());
            add(startBtn);
            add(Box.createVerticalStrut(20));
            JButton exitBtn = new JButton("Exit");
            exitBtn.setFont(new Font("Arial", Font.BOLD, 24));
            exitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            exitBtn.addActionListener(e -> System.exit(0));
            add(exitBtn);
            add(Box.createVerticalGlue());
        }
        public void setHighScore(int score) {
            highScoreLabel.setText("High Score: " + score);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Tetris::new);
    }
}

class TetrisPanel extends JPanel implements ActionListener, KeyListener {
    private final int ROWS = 20, COLS = 10, CELL = 30;
    private javax.swing.Timer timer;
    private int[][] board = new int[ROWS][COLS];
    private Tetromino current, next;
    private int curRow, curCol, score = 0;
    private boolean gameOver = false;
    private SidebarPanel sidebar;
    private long startTime;
    public int getScore() { return score; }

    public TetrisPanel() {
        setPreferredSize(new Dimension(COLS * CELL, ROWS * CELL));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
    }

    public void setSidebar(SidebarPanel sidebar) {
        this.sidebar = sidebar;
    }

    public void startGame() {
        Arrays.stream(board).forEach(row -> Arrays.fill(row, 0));
        score = 0;
        gameOver = false;
        startTime = System.currentTimeMillis();
        next = Tetromino.random();
        spawnTetromino();
        if (timer != null) timer.stop();
        timer = new javax.swing.Timer(400, this);
        timer.start();
        requestFocusInWindow();
    }

    private void spawnTetromino() {
        current = next;
        next = Tetromino.random();
        curRow = 0;
        curCol = COLS / 2 - 2;
        if (!canMove(current.shape, curRow, curCol)) {
            timer.stop();
            gameOver = true;
        }
        if (sidebar != null) sidebar.setNext(next);
    }

    private boolean canMove(int[][] shape, int r, int c) {
        for (int i = 0; i < shape.length; i++)
            for (int j = 0; j < shape[0].length; j++)
                if (shape[i][j] != 0) {
                    int nr = r + i, nc = c + j;
                    if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS || board[nr][nc] != 0)
                        return false;
                }
        return true;
    }

    private void merge() {
        for (int i = 0; i < current.shape.length; i++)
            for (int j = 0; j < current.shape[0].length; j++)
                if (current.shape[i][j] != 0)
                    board[curRow + i][curCol + j] = current.color;
    }

    private void clearLines() {
        int lines = 0;
        for (int i = ROWS - 1; i >= 0; i--) {
            boolean full = true;
            for (int j = 0; j < COLS; j++)
                if (board[i][j] == 0) full = false;
            if (full) {
                lines++;
                for (int k = i; k > 0; k--)
                    board[k] = Arrays.copyOf(board[k - 1], COLS);
                Arrays.fill(board[0], 0);
                i++;
            }
        }
        if (lines > 0) score += (lines * lines) * 100;
        if (sidebar != null) sidebar.setScore(score);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;
        if (canMove(current.shape, curRow + 1, curCol)) {
            curRow++;
        } else {
            merge();
            clearLines();
            spawnTetromino();
        }
        if (sidebar != null) sidebar.setTime((System.currentTimeMillis() - startTime) / 1000);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw board
        for (int i = 0; i < ROWS; i++)
            for (int j = 0; j < COLS; j++)
                if (board[i][j] != 0) {
                    g.setColor(Tetromino.colors[board[i][j]]);
                    g.fillRect(j * CELL, i * CELL, CELL, CELL);
                    g.setColor(Color.DARK_GRAY);
                    g.drawRect(j * CELL, i * CELL, CELL, CELL);
                }
        // Draw current tetromino
        if (!gameOver) {
            g.setColor(Tetromino.colors[current.color]);
            for (int i = 0; i < current.shape.length; i++)
                for (int j = 0; j < current.shape[0].length; j++)
                    if (current.shape[i][j] != 0)
                        g.fillRect((curCol + j) * CELL, (curRow + i) * CELL, CELL, CELL);
        }
        // Game over
        if (gameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.drawString("Game Over", 30, getHeight() / 2);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_LEFT && canMove(current.shape, curRow, curCol - 1)) curCol--;
        else if (code == KeyEvent.VK_RIGHT && canMove(current.shape, curRow, curCol + 1)) curCol++;
        else if (code == KeyEvent.VK_DOWN && canMove(current.shape, curRow + 1, curCol)) curRow++;
        else if (code == KeyEvent.VK_UP) {
            int[][] rotated = current.rotate();
            if (canMove(rotated, curRow, curCol)) current.shape = rotated;
        } else if (code == KeyEvent.VK_SPACE) {
            while (canMove(current.shape, curRow + 1, curCol)) curRow++;
            actionPerformed(null);
        }
        repaint();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}

class SidebarPanel extends JPanel {
    private JLabel scoreLabel, timeLabel;
    private NextPiecePanel nextPanel;
    private int score = 0, time = 0;
    private Runnable endListener;

    public SidebarPanel(TetrisPanel gamePanel) {
        setPreferredSize(new Dimension(140, 600));
        setBackground(Color.LIGHT_GRAY);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(30));
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(scoreLabel);
        add(Box.createVerticalStrut(30));
        timeLabel = new JLabel("Time: 0s");
        timeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(timeLabel);
        add(Box.createVerticalStrut(30));
        nextPanel = new NextPiecePanel();
        nextPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(nextPanel);
        add(Box.createVerticalStrut(30));
        JButton endBtn = new JButton("End");
        endBtn.setFont(new Font("Arial", Font.BOLD, 18));
        endBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        endBtn.addActionListener(e -> { if (endListener != null) endListener.run(); });
        add(endBtn);
        add(Box.createVerticalGlue());
    }

    public void setScore(int score) {
        this.score = score;
        scoreLabel.setText("Score: " + score);
    }

    public void setTime(long seconds) {
        this.time = (int) seconds;
        timeLabel.setText("Time: " + time + "s");
    }

    public void setNext(Tetromino next) {
        nextPanel.setTetromino(next);
    }

    public void setEndListener(Runnable r) {
        this.endListener = r;
    }
}

class NextPiecePanel extends JPanel {
    private Tetromino tetromino;

    public NextPiecePanel() {
        setPreferredSize(new Dimension(100, 100));
        setBackground(Color.WHITE);
    }

    public void setTetromino(Tetromino t) {
        this.tetromino = t;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (tetromino == null) return;
        int[][] shape = tetromino.shape;
        g.setColor(Tetromino.colors[tetromino.color]);
        int cell = 20;
        int offsetX = (getWidth() - shape[0].length * cell) / 2;
        int offsetY = (getHeight() - shape.length * cell) / 2;
        for (int i = 0; i < shape.length; i++)
            for (int j = 0; j < shape[0].length; j++)
                if (shape[i][j] != 0)
                    g.fillRect(offsetX + j * cell, offsetY + i * cell, cell, cell);
    }
}

class Tetromino {
    public int[][] shape;
    public int color;
    private static final int[][][] SHAPES = {
        // I
        {{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}},
        // J
        {{2,0,0},{2,2,2},{0,0,0}},
        // L
        {{0,0,3},{3,3,3},{0,0,0}},
        // O
        {{4,4},{4,4}},
        // S
        {{0,5,5},{5,5,0},{0,0,0}},
        // T
        {{0,6,0},{6,6,6},{0,0,0}},
        // Z
        {{7,7,0},{0,7,7},{0,0,0}}
    };
    public static final Color[] colors = {
        Color.BLACK, Color.CYAN, Color.BLUE, Color.ORANGE,
        Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.RED
    };

    public Tetromino(int[][] shape, int color) {
        this.shape = shape;
        this.color = color;
    }

    public static Tetromino random() {
        int idx = new Random().nextInt(SHAPES.length);
        int[][] s = new int[SHAPES[idx].length][];
        for (int i = 0; i < SHAPES[idx].length; i++)
            s[i] = Arrays.copyOf(SHAPES[idx][i], SHAPES[idx][i].length);
        return new Tetromino(s, idx + 1);
    }

    public int[][] rotate() {
        int n = shape.length, m = shape[0].length;
        int[][] res = new int[m][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                res[j][n - 1 - i] = shape[i][j];
        return res;
    }
}
