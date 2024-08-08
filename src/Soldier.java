import java.awt.Color;
import java.awt.Graphics;

public class Soldier extends GameObject {
    int xMin = 0;
    int xMax = GamePanel.WIDTH - 20;

    public Soldier(int x, int y, Color color) {
        super(x, y, 20, 30, color);
    }

    public void setXMinMax(int min0, int max0) {
        xMin = min0;
        xMax = max0 - 20;
    }

    public void resetXMinMax() {
        xMin = 0;
        xMax = GamePanel.WIDTH - 20;
    }

    public void moveLeft() {
        if (x > xMin) {
            x -= 10; // 左に移動
        }
    }

    public void moveRight() {
        if (x < xMax) { // 画面右端の制限
            x += 10; // 右に移動
        }
    }

    public void moveTo(int newX) {
        if (newX > xMin && newX < xMax) { // 画面外に出ないように制限
            this.x = newX;
        }
    }

    @Override
    public void update() {
        // Soldierの更新処理（必要なら追加）
    }

    @Override
    public void draw(Graphics g) {
        // 兵士の体
        g.setColor(color);
        g.fillRect(x, y - 20, width, height);

        // 兵士の頭
        g.setColor(Color.BLACK);
        g.fillRect(x + 5, y - 30, 10, 10);

        // 兵士の足
        g.setColor(Color.GRAY);
        g.fillRect(x + 2, y + 10, 6, 10);
        g.fillRect(x + 12, y + 10, 6, 10);

        // 兵士の腕
        g.setColor(Color.GRAY);
        g.fillRect(x - 5, y - 15, 5, 20);
        g.fillRect(x + 20, y - 15, 5, 20);
    }
}
