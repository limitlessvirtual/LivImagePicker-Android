package tech.livx.livimagepicker;

import android.graphics.Bitmap;

/**
 * Output interface for variable Output callbacks
 * @param <T> Output Type
 */
public interface Output<T> {
    void onImageLoaded(T image);
    void onImageLoadFailed();
    void process(Bitmap bitmap);
}