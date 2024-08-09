import java.awt.Color;
import java.awt.Graphics;

public class Obstacle extends GameObject {

    public Obstacle(int x, int y) {
        super(x, y, 30, 30, Color.DARK_GRAY);
    }

    @Override
    public void update(int fall_speed) {
        y += fall_speed;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }
}
