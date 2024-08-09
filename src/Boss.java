import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class Boss extends GameObject {
    private int requiredSoldiers;

    public Boss(int x, int y, int requiredSoldiers, Color color) {
        super(x, y, 40, 60, color); // サイズをキャラクターと同じ形式に変更
        this.requiredSoldiers = requiredSoldiers;
    }

    @Override
    public void update(int fall_speed) {
        y += fall_speed; // ボスの降下速度
    }

    @Override
    public void draw(Graphics g) {
        // ボスの体
        g.setColor(color);
        g.fillRect(x, y - 20, width, height);

        // ボスの頭
        g.setColor(Color.BLACK);
        g.fillRect(x + 10, y - 30, 20, 20);

        // ボスの足
        g.setColor(Color.GRAY);
        g.fillRect(x + 5, y + 40, 10, 20);
        g.fillRect(x + 25, y + 40, 10, 20);

        // ボスの腕
        g.setColor(Color.GRAY);
        g.fillRect(x - 10, y - 10, 10, 30);
        g.fillRect(x + 40, y - 10, 10, 30);
    }

    public int getRequiredSoldiers() {
        return requiredSoldiers;
    }
}
