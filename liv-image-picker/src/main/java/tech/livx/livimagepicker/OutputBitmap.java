package tech.livx.livimagepicker;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Output image as Bitmap
 */
public abstract class OutputBitmap implements Output<Bitmap> {
    @Override
    public void process(Uri uri, Bitmap bitmap) {
        onImageLoaded(uri,bitmap);
    }
}
