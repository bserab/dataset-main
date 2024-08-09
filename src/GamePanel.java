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
import java.awt.FontMetrics; // FontMetrics クラスのインポートを追加
import java.util.Random;

public class GamePanel extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener {
    static final int WIDTH = 800;
    static final int HEIGHT = 600;
    private static final int PANEL_PASS_COUNT = 5; // パネル通過のカウントをここで定義
    private static final int PANEL_FALL_SPEED = 2; // パネルの降下速度
    private static final int OBSTACLE_FALL_SPEED = 3; //障害物の効果速度
    private static final long PANEL_COOLDOWN = 1000; // パネル取得後のクールダウンタイム（ミリ秒）
    
    private Thread thread;
    private boolean running;
    private boolean first;
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
    private Random random;
    private long lastPanelCollectedTime; // 最後にパネルを取得した時間

    private double sleepAddTime;
    private int fps = 60;

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
        first = true;
        score = 1; // スコアの初期値を1に設定
        random = new Random();
        lastPanelCollectedTime = 0; // 初期値
        generatePanels();
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);
        setFps(fps);
    }

    private void startGame() {
        System.out.println("START");
        running = true;
        if (first) {
            thread = new Thread(this);
            thread.start();
            first = false;
        }
    }

    @Override
    public void run() {
        double nextTime = System.currentTimeMillis() + sleepAddTime;
        while (running) {
            updateGame();
            //repaint();
            try {
                long res = (long) nextTime - System.currentTimeMillis();
                if (res < 0) res = 0;
                Thread.sleep(res);
                //Thread.sleep(16); // 約60FPS
                repaint();
                nextTime += sleepAddTime;
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
            boss.update(PANEL_FALL_SPEED);
            if (boss.getY() >= soldier.getY()) {
                if (score >= boss.getRequiredSoldiers()) {
                    gameWon = true;
                    bossFight = false;
                    panelPasses = 0;
                    soldier.setY(HEIGHT - 50);
                    generatePanels();
                } else {
                    gameOver = true;
                    System.out.println("You lose!");
                }
            }
            return;
        }

        for (Panel panel : panels) {
            panel.update(PANEL_FALL_SPEED);
        }

        for (Obstacle obstacle : obstacles) {
            obstacle.update(OBSTACLE_FALL_SPEED);
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
            boss = new Boss((WIDTH - 40) / 2, -100, (int) (calculateMaxScore(score) * 0.2), Color.RED); // ボスの位置を中央に設定
            soldier.setX((WIDTH - 20) / 2); // プレイヤーを道の内側に強制移動
            soldier.setXMinMax(boss.getX(), boss.getX() + boss.getWidth());
        }
    }

    public void setFps(int fps) {
        if (fps < 10 || fps > 60) {
            throw new IllegalArgumentException("fpsの設定は10～60の間で指定してください。");
        }
        this.fps = fps;
        sleepAddTime = 1000.0 / fps;
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
                soldier.setXMinMax(panel.getX(), panel.getX() + panel.getWidth());
                break; // 1フレームで1つのパネルのみ収集
            } else {
                soldier.resetXMinMax();
            }
        }

        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obstacle = obstacles.get(i);
            if (soldier.getX() < obstacle.getX() + obstacle.getWidth() &&
                soldier.getX() + soldierSize > obstacle.getX() &&
                soldier.getY() < obstacle.getY() + obstacle.getHeight() &&
                soldier.getY() + soldierSize > obstacle.getY()) {
                gameOver = true;
                System.out.println("Hit an obstacle!");
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
                generatePanels();
            } else {
                gameOver = true;
                System.out.println("You lose!");
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

    private int calculateMaxScore(int currentScore) {
        int max = currentScore;
        for (int i = 0; i < PANEL_PASS_COUNT; i++) {
            max += 10;
        }
        return max;
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
            g.drawString("Press T to Return to Title", WIDTH / 2 - 130, HEIGHT / 2 + 50); // タイトルへ戻るメッセージ
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
    
        // ボス戦が始まっているときの処理
        if (bossFight) {
            boss.draw(g);
    
            // ボスのスコアを表示
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            String scoreText = "Boss Score: " + boss.getRequiredSoldiers();
            int textWidth = fm.stringWidth(scoreText);
            g.drawString(scoreText, boss.getX() + (boss.getWidth() - textWidth) / 2, boss.getY() - 50);
    
            // ボスの画面での壁を描画
            g.drawLine(boss.getX(), 0, boss.getX(), HEIGHT);
            g.drawLine(boss.getX() + boss.getWidth(), 0, boss.getX() + boss.getWidth(), HEIGHT);
        } else {
            // パネルと障害物の描画
            for (Panel panel : panels) {
                panel.draw(g);
            }
            for (Obstacle obstacle : obstacles) {
                obstacle.draw(g);
            }
        }
    
        // 兵士の描画
        soldier.draw(g);
    
        // スコアの表示
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Score: " + score, 10, 25); // スコアのみ表示
    }
    

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_A) {
            if (soldier.getX() > 0 && (!bossFight || soldier.getX() > boss.getX())) {
                soldier.moveLeft();
            }
        } else if (key == KeyEvent.VK_D) {
            if (soldier.getX() < WIDTH - 20 && (!bossFight || soldier.getX() < boss.getX() + boss.getWidth() - 20)) {
                soldier.moveRight();
            }
        } else if (key == KeyEvent.VK_T && gameOver) {
            startScreen = true; // タイトルに戻る
            gameOver = false;
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
            startScreen = true; // タイトルに戻る
            gameOver = false;
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
        lastPanelCollectedTime = 0; // クールダウンタイムをリセット
        generatePanels();
        startGame();
    }
}
