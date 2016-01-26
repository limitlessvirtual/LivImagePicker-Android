package tech.livx.livimagepicker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * ExifUtil class used determine rotation matrix depending on image Exif values
 *
 * @author Limitless Virtual
 * @version 0.1
 */
public class ExifUtil {

    /**
     * Retrieve Exif from content provider
     *
     * @param context Application context
     * @param src     Selected image Uri
     * @return Orientation in degrees
     */
    public static int getExifOrientationFromGallery(Context context, Uri src) {
        Cursor cursor = context.getContentResolver().query(src,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

        assert cursor != null;
        if (cursor.getCount() != 1 && cursor.getColumnCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        int orientation = cursor.getInt(0);
        cursor.close();

        return orientation;
    }
}