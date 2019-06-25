package ball.pinball;

public class Barrier {

    private int x;
    private int y;
    private int x2;
    private int y2;
    private int color;

    public Barrier() {}

    public Barrier(int x, int y, int x2, int y2, int color) {
        this.x = x;
        this.y = y;
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
    }

    public int getX() { return x; }

    public int getY() { return y; }

    public int getX2() { return x2; }

    public int getY2() { return y2; }

    public int getColor() { return color; }

}