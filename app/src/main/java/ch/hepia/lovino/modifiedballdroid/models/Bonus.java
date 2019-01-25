package ch.hepia.lovino.modifiedballdroid.models;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Bonus extends StaticGameObject {
    private final int seconds;
    private static final int COLOR_BONUS = Color.GREEN;
    private static final int COLOR_MALUS = Color.RED;

    public Bonus(float x, float y, float width, float height, int seconds) {
        super(x, y, width, height);
        this.seconds = seconds;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(seconds < 0 ? COLOR_MALUS : COLOR_BONUS);
        canvas.drawRect(this.getBoundingRect(), paint);
    }

    public int getSeconds() {
        return seconds;
    }
}
