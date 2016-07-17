package camera.plam.com.plamcamera.Utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;

/**
 * Created by PunK _|_ RuLz on 16/07/16.
 */
public class BitmapFilters {

    private static final String LOG_TAG = BitmapFilters.class.getSimpleName();

    public enum Filters {
        TINT_RED, NONE, GREY;

        private static Filters[] value = values();

        public Filters next() {
            return value[(this.ordinal() + 1) % value.length];
        }

        public Filters previous() {
            if (this.ordinal() == 0) {
                return value[value.length - 1];
            } else {
                return value[((this.ordinal() - 1) % value.length)];
            }
        }
    }

    public static Bitmap getGreyScaledBitmap(Bitmap source) {
        float[] GrayArray = {
                0.213f, 0.715f, 0.072f, 0.0f, 0.0f,
                0.213f, 0.715f, 0.072f, 0.0f, 0.0f,
                0.213f, 0.715f, 0.072f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f,
        };
        Paint paint = new Paint();
        ColorMatrix colorMatrixGray = new ColorMatrix(GrayArray);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrixGray);
        paint.setAntiAlias(true);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(filter);
        Canvas canvas = new Canvas(source);
        canvas.drawBitmap(source, 0, 0, paint);
        return source;
    }

    public static Bitmap getTintedBitmap(Bitmap source, int color) {

        Paint p = new Paint(Color.RED);
        ColorFilter filter = new LightingColorFilter(color, 1);
        p.setColorFilter(filter);

        Canvas c = new Canvas(source);
        c.setBitmap(source);
        c.drawBitmap(source, 0, 0, p);

        return source;
    }
}
