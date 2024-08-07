import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import java.awt.Font;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener {
    static final int WIDTH = 800;
    static final int HEIGHT = 600;
    private static final int PANEL_PASS_COUNT = 5; // パネル通過のカウントをここで定義
    private static final int PANEL_FALL_SPEED = 2; // パネルの降下速度
    private static final long PANEL_COOLDOWN = 1000; // パネル取得後のクールダウンタイム（ミリ秒）
    private Thread thread;
    private boolean running;
    private Soldier soldier;
    private List<Panel> panels;
    private List<Obstacle> obstacles;
    private Boss boss;
    private boolean gameOver;
    private boolean gameWon;
    private int panelPasses;
    private boolean bossFight;
    private boolean startScreen;
    private int score;
    private int level;
    private Random random;
    private long lastPanelCollectedTime; // 最後にパネルを取得した時間

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        soldier = new Soldier(WIDTH / 4, HEIGHT - 50, Color.BLUE); // 左側から開始するように位置を変更
        panels = new ArrayList<>();
        obstacles = new ArrayList<>();
        gameOver = false;
        gameWon = false;
        panelPasses = 0;
        bossFight = false;
        startScreen = true;
        score = 1; // スコアの初期値を1に設定
        level = 1;
        random = new Random();
        lastPanelCollectedTime = 0; // 初期値
        initializeGameObjects();
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);
    }

    private void startGame() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        while (running) {
            updateGame();
            repaint();
            try {
                Thread.sleep(16); // 約60FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateGame() {
        if (gameOver || startScreen || gameWon) {
            return;
        }

        if (bossFight) {
            boss.update();
            if (boss.getY() >= soldier.getY()) {
                if (score >= boss.getRequiredSoldiers()) {
                    gameWon = true;
                    bossFight = false;
                    panelPasses = 0;
                    soldier.setY(HEIGHT - 50);
                    initializeGameObjects();
                } else {
                    gameOver = true;
                    System.out.println("You lose!");
                    resetToStartScreen();
                }
            }
            return;
        }

        for (Panel panel : panels) {
            panel.update();
        }

        for (Obstacle obstacle : obstacles) {
            obstacle.update();
        }

        if (panels.isEmpty() || panels.get(panels.size() - 1).getY() > HEIGHT / 2) {
            generatePanels();
            generateObstacles();
        }

        checkCollisions();

        if (panelPasses >= PANEL_PASS_COUNT) {
            soldier.resetXMinMax();
            bossFight = true;
            panels.clear();
            obstacles.clear();
            boss = new Boss(0, -100, (int) (calculateMaxScore(score) * 0.2), Color.RED); // ボスの横幅を広げる
        }
    }

    private void generatePanels() {
        int y = -50;
        int leftX = 0;
        int rightX = WIDTH - 400;
        String[] operations = {"+5", "+10", "*2", "*3", "-3", "-5"};
        String leftOperation = operations[(int) (Math.random() * operations.length)];
        String rightOperation = operations[(int) (Math.random() * operations.length)];

        panels.add(new Panel(leftX, y, leftOperation));
        panels.add(new Panel(rightX, y, rightOperation));
    }

    private void generateObstacles() {
        int x = random.nextInt(WIDTH - 30);
        obstacles.add(new Obstacle(x, -30));
    }

    private void checkCollisions() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPanelCollectedTime < PANEL_COOLDOWN) {
            return; // クールダウンタイム中はパネルを収集しない
        }
        int soldierSize = 20;
        int padding = 5; // 当たり判定のパディングを追加

        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            if (soldier.getX() + padding < panel.getX() + panel.getWidth() &&
                soldier.getX() + soldierSize - padding > panel.getX() &&
                soldier.getY() + padding < panel.getY() + panel.getHeight() &&
                soldier.getY() + soldierSize - padding > panel.getY()) {
                applyOperation(panel.getOperation());
                panels.remove(i);
                panelPasses++;
                lastPanelCollectedTime = currentTime; // パネルを収集した時間を更新
                soldier.setXMinMax(panel.getX(),panel.getX() + panel.getWidth());
                break; // 1フレームで1つのパネルのみ収集
            } else {
                soldier.resetXMinMax();
            }
        }

        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obstacle = obstacles.get(i);
            if (soldier.getX() < obstacle.getX() + obstacle.getSize() &&
                soldier.getX() + soldierSize > obstacle.getX() &&
                soldier.getY() < obstacle.getY() + obstacle.getSize() &&
                soldier.getY() + soldierSize > obstacle.getY()) {
                gameOver = true;
                System.out.println("Hit an obstacle!");
                resetToStartScreen();
                return;
            }
        }

        // ボスとの衝突判定
        if (bossFight && soldier.getX() < boss.getX() + boss.getWidth() &&
            soldier.getX() + soldierSize > boss.getX() &&
            soldier.getY() < boss.getY() + boss.getHeight() &&
            soldier.getY() + soldierSize > boss.getY()) {
            if (score >= boss.getRequiredSoldiers()) {
                gameWon = true;
                bossFight = false;
                panelPasses = 0;
                soldier.setY(HEIGHT - 50);
                initializeGameObjects();
            } else {
                gameOver = true;
                System.out.println("You lose!");
                resetToStartScreen();
            }
        }
    }

    private void applyOperation(String operation) {
        if (operation.startsWith("+")) {
            int value = Integer.parseInt(operation.substring(1));
            score += value;
        } else if (operation.startsWith("*")) {
            int value = Integer.parseInt(operation.substring(1));
            score *= value;
        } else if (operation.startsWith("-")) {
            int value = Integer.parseInt(operation.substring(1));
            score -= value;
        }

        if (score < 1) {
            score = 1; // スコアが1未満にならないようにする
        }
    }

    private void initializeGameObjects() {
        generatePanels();
    }

    private int calculateMaxScore(int currentScore) {
        int max = currentScore;
        for (int i = 0; i < PANEL_PASS_COUNT; i++) {
            max += 10;
        }
        return max;
    }

    private void resetToStartScreen() {
        startScreen = true;
        running = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.WHITE); // 背景色を白に設定
        if (startScreen) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("Press any key to Start", WIDTH / 2 - 150, HEIGHT / 2);
            return;
        }

        if (gameOver) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("Game Over", WIDTH / 2 - 100, HEIGHT / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Press R to Retry", WIDTH / 2 - 100, HEIGHT / 2 + 50);
            return;
        }

        if (gameWon) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("You Win!", WIDTH / 2 - 100, HEIGHT / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Press any key to continue", WIDTH / 2 - 150, HEIGHT / 2 + 50);
            return;
        }

        if (bossFight) {
            boss.draw(g);
            soldier.draw(g);
        } else {
            soldier.draw(g);
            for (Panel panel : panels) {
                panel.draw(g);
            }
            for (Obstacle obstacle : obstacles) {
                obstacle.draw(g);
            }
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Score: " + score, 10, 25); // スコアのみ表示
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_A) {
            if (soldier.getX() > 0) {
                soldier.moveLeft();
            }
        } else if (key == KeyEvent.VK_D) {
            if (soldier.getX() < WIDTH - 20) {
                soldier.moveRight();
            }
        } else if (key == KeyEvent.VK_R && gameOver) {
            resetGame();
        } else if (!gameOver && gameWon) {
            gameWon = false;
        } else if (startScreen) {
            startScreen = false;
            resetGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (startScreen) {
            startScreen = false;
            resetGame();
        } else if (gameOver && e.getButton() == MouseEvent.BUTTON1) {
            resetGame();
        } else if (!gameOver && gameWon) {
            gameWon = false;
        } else {
            soldier.moveTo(e.getX() - 10);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        soldier.moveTo(e.getX() - 10);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    private void resetGame() {
        soldier = new Soldier(WIDTH / 4, HEIGHT - 50, Color.BLUE); // 左側から開始するように位置を変更
        panels.clear();
        obstacles.clear();
        gameOver = false;
        gameWon = false;
        panelPasses = 0;
        bossFight = false;
        score = 1; // スコアの初期値を1にリセット
        level = 1;
        lastPanelCollectedTime = 0; // クールダウンタイムをリセット
        initializeGameObjects();
        startGame();
    }
}
