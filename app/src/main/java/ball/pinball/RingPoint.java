package ball.pinball;

public class RingPoint {

    private int x;
    private int y;
    private int radius;
    private int color;

    public RingPoint() {}

    public RingPoint(int x, int y, int radius, int color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
    }

    public int getX() { return x; }

    public int getY() { return y; }

    public int getRadius() { return radius; }

    public int getColor() { return color; }

}