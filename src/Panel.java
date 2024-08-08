import java.awt.Color;
import java.awt.Graphics;

public class Panel extends GameObject {
    private String operation;

    public Panel(int x, int y, String operation) {
        super(x, y, 400, 50, operation.startsWith("-") ? new Color(255, 182, 193) : new Color(173, 216, 230));
        this.operation = operation;
    }

    @Override
    public void update() {
        y += 2; // パネルの降下速度を固定
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);
        g.drawString(operation, x + 175, y + 30);
    }

    public String getOperation() {
        return operation;
    }
}
