package ch.hepia.lovino.modifiedballdroid.models;


import android.graphics.RectF;

public abstract class GameObject implements Drawable {
    protected float x;
    protected float y;

    public GameObject(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    abstract public RectF getBoundingRect();
}
