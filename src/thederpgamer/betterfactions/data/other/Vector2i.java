package thederpgamer.betterfactions.data.other;

import javax.vecmath.Tuple2i;
import javax.vecmath.Vector2f;
import java.io.Serializable;

/**
 * Vector2i.java
 * <Description>
 * ==================================================
 * Created 02/09/2021
 * @author TheDerpGamer
 */
public class Vector2i extends Tuple2i implements Serializable {

    public Vector2i() {
        super();
    }

    public Vector2i(int x, int y) {
        super(x, y);
    }

    public Vector2i(int[] array) {
        super(array);
    }

    public Vector2i(Vector2i vector) {
        super(vector);
    }

    public Vector2i(float x, float y) {
        super((int) x, (int) y);
    }

    public Vector2i(float[] array) {
        this(array[0], array[1]);
    }

    public Vector2i(Vector2f vector) {
        this(vector.x, vector.y);
    }


    public Vector2f toVector2f() {
        return new Vector2f(x, y);
    }

    public int dot(Vector2i vector) {
        return this.x * vector.x + this.y * vector.y;
    }

    public int length() {
        return (int) Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public int lengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    public void normalize(Vector2i vector) {
        int i = (int) (1.0D / Math.sqrt(vector.x * vector.x + vector.y * vector.y));
        this.x = vector.x * i;
        this.y = vector.y * i;
    }

    public void normalize() {
        int i = (int) (1.0D / Math.sqrt(this.x * this.x + this.y * this.y));
        this.x *= i;
        this.y *= i;
    }

    public final float angle(Vector2i vector) {
        int i = this.dot(vector) / (this.length() * vector.length());
        if(i < -1) i = -1;
        if(i > 1) i = 1;
        return (int) Math.acos(i);
    }
}
