package tech.livx.livimagepicker;

import android.graphics.Bitmap;

/**
 * Output image as Bitmap
 */
public abstract class OutputBitmap implements Output<Bitmap> {
    @Override
    public void process(Bitmap bitmap) {
        onImageLoaded(bitmap);
    }
}
