package plam.com.camera.Utils;

/**
 * Created by PunK _|_ RuLz on 17/07/16.
 */

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class BitmapCache {
    private LruCache<String, Bitmap> bitmapLruCache;

    private static BitmapCache cache;

    public static BitmapCache getInstance() {
        if (cache == null) {
            cache = new BitmapCache();
        }
        return cache;
    }

    public void initializeCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        final int cacheSize = maxMemory / 4;

        bitmapLruCache = new LruCache<String, Bitmap>(cacheSize) {
            protected int sizeOf(String key, Bitmap value) {
                // The cache size will be measured in kilobytes rather than number of items.

                int bitmapByteCount = value.getRowBytes() * value.getHeight();

                return bitmapByteCount / 1024;
            }
        };
    }

    public void addBitmapCache(String key, Bitmap value) {
        if (bitmapLruCache != null && bitmapLruCache.get(key) == null) {
            bitmapLruCache.put(key, value);
        }
    }

    public Bitmap getBitmapCache(String key) {
        if (key != null) {
            return bitmapLruCache.get(key);
        } else {
            return null;
        }
    }

    public void removeBitmapCache(String key) {
        bitmapLruCache.remove(key);
    }

    public void clearCache() {
        if (bitmapLruCache != null) {
            bitmapLruCache.evictAll();
        }
    }

}
