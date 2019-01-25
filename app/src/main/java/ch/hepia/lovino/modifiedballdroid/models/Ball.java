package ch.hepia.lovino.modifiedballdroid.models;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class Ball extends GameObject {
    private float speedX, speedY;
    private float maxSpeed;
    private int gravity;
    private final float radius = 8f;
    private static final int BALL_COLOR = Color.BLACK;
    private static final float DRAG_FORCE = 10.0f;
    private static final int DRAG_FORCE_REBOUND = 2;
    private static final float INIT_X = 100;
    private static final float INIT_Y = 100;
    private static final float MAX_SPEED_SLOW = 12.0f;
    private static final float MAX_SPEED_MEDIUM = 18.0f;
    private static final float MAX_SPEED_FAST = 30.0f;
    private static final int GRAVITY_EASY = 6;
    private static final int GRAVITY_MEDIUM = 9;
    private static final int GRAVITY_HARD = 15;


    public Ball(DifficultyLevel difficulty) {
        super(INIT_X, INIT_Y);
        switch (difficulty) {
            case EASY:
                this.maxSpeed = MAX_SPEED_SLOW;
                this.gravity = GRAVITY_EASY;
                break;
            case MEDIUM:
                this.maxSpeed = MAX_SPEED_MEDIUM;
                this.gravity = GRAVITY_MEDIUM;
                break;
            case HARD:
                this.maxSpeed = MAX_SPEED_FAST;
                this.gravity = GRAVITY_HARD;
                break;
        }
        this.speedY = 0;
        this.speedX = 0;

    }

    public float getRadius() {
        return radius;
    }

    public void setX(float newX) {
        this.x = newX;
    }

    public void setY(float newY) {
        this.y = newY;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(BALL_COLOR);
        canvas.drawCircle(x, y, radius, paint);
    }

    public void reboundX() {
        this.speedX *= -1;
        this.speedX /= DRAG_FORCE_REBOUND;
    }

    public void reboundY() {
        this.speedY *= -1;
        this.speedY /= DRAG_FORCE_REBOUND;
    }

    public void putToStart() {
        this.x = INIT_X;
        this.y = INIT_Y;
        this.speedX = 0;
        this.speedY = 0;
    }

    public void incrementSpeedX(float toAdd) {
        this.speedX = Math.min(this.maxSpeed, this.speedX + toAdd / DRAG_FORCE);
    }

    public void incrementSpeedY() {
        this.speedY = Math.min(this.maxSpeed, this.speedY + this.gravity / DRAG_FORCE);
    }

    public void updatePosition() {
        this.x += speedX;
        this.y += speedY;
    }

    public RectF getBoundingRect() {
        return new RectF(x - radius, y - radius, x + radius, y + radius);
    }

    public BallDirection getDirection() {
        if (this.speedX > 0) {
            if (this.speedY > 0) {
                return BallDirection.SE;
            } else if (this.speedY < 0) {
                return BallDirection.NE;
            } else {
                return BallDirection.E;
            }
        } else if (this.speedX < 0) {
            if (this.speedY > 0) {
                return BallDirection.SW;
            } else if (this.speedY < 0) {
                return BallDirection.NW;
            } else {
                return BallDirection.W;
            }
        } else {
            if (this.speedY > 0) {
                return BallDirection.S;
            } else if (this.speedY < 0) {
                return BallDirection.N;
            } else {
                return BallDirection.STILL;
            }
        }
    }

}
