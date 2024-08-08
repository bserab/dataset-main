import java.awt.Color;
import java.awt.Graphics;

public class Obstacle extends GameObject {
    private static final int FALL_SPEED = 3;

    public Obstacle(int x, int y) {
        super(x, y, 30, 30, Color.DARK_GRAY);
    }

    @Override
    public void update() {
        y += FALL_SPEED;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }
}
