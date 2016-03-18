package tech.livx.livimagepicker;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Output interface for variable Output callbacks
 *
 * @param <T> Output Type
 */
public interface Output<T> {
    void onImageLoaded(Uri uri,T image);

    void onImageLoadFailed();

    void process(Uri uri,Bitmap bitmap);
}