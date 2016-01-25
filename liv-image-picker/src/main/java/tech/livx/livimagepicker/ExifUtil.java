package tech.livx.livimagepicker;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ExifUtil class used determine rotation matrix depending on image Exif values
 *
 * @author Limitless Virtual
 * @version 0.1
 */
public class ExifUtil {

    /**
     * Rotates bitmap retrieved from Gallery content provider Api
     *
     * @param context Application context
     * @param uri     Gallery content provider Uri of selected image
     * @param cx      Center X of image
     * @param cy      Center Y of image
     * @return Rotation matrix for correct Bitmap orientation
     */
    public static Matrix rotateBitmapFromGallery(Context context, Uri uri, float cx, float cy) {
        Matrix matrix = new Matrix();
        int orientation = getExifOrientationFromGallery(context, uri);

        matrix.setRotate(orientation, cx, cy);

        return matrix;
    }

    /**
     * Retrieve Exif from content provider
     *
     * @param context Application context
     * @param src     Selected image Uri
     * @return Orientation in degrees
     */
    private static int getExifOrientationFromGallery(Context context, Uri src) {
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

    /**
     * Rotate bitmap retrieved from File system
     *
     * @param src Uri of selected image from file system
     * @param cx  Center X of image
     * @param cy  Center Y of image
     * @return Rotation matrix for correct Bitmap orientation
     */
    public static Matrix rotateBitmapFromFile(String src, float cx, float cy) {
        try {
            int orientation = getExifOrientationFromFile(src);
            Matrix matrix = new Matrix();

            if (orientation == 1) {
                return matrix;
            }

            switch (orientation) {
                case 2:
                    matrix.setScale(-1, 1);
                    break;
                case 3:
                    matrix.setRotate(180, cy, cx);
                    break;
                case 4:
                    matrix.setRotate(180, cy, cx);
                    matrix.postScale(-1, 1);
                    break;
                case 5:
                    matrix.setRotate(90, cy, cx);
                    matrix.postScale(-1, 1);
                    break;
                case 6:
                    matrix.setRotate(90, cy, cx);
                    break;
                case 7:
                    matrix.setRotate(-90, cy, cx);
                    matrix.postScale(-1, 1);
                    break;
                case 8:
                    matrix.setRotate(-90, cy, cx);
                    break;
                default:
                    return matrix;
            }

            return matrix;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Matrix();
    }

    /**
     * Retrieve Exif from file system
     *
     * @param src Selected File Uri
     * @return Orientation code
     * @throws IOException
     */
    private static int getExifOrientationFromFile(String src) throws IOException {
        int orientation = 1;

        try {
            /**
             * if your are targeting only api level >= 5
             * ExifInterface exif = new ExifInterface(src);
             * orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
             */
            if (Build.VERSION.SDK_INT >= 5) {
                Class<?> exifClass = Class.forName("android.media.ExifInterface");
                Constructor<?> exifConstructor = exifClass.getConstructor(String.class);
                Object exifInstance = exifConstructor.newInstance(src);
                Method getAttributeInt = exifClass.getMethod("getAttributeInt", String.class, int.class);
                Field tagOrientationField = exifClass.getField("TAG_ORIENTATION");
                String tagOrientation = (String) tagOrientationField.get(null);
                orientation = (Integer) getAttributeInt.invoke(exifInstance, tagOrientation, 1);
            }
        } catch (ClassNotFoundException | SecurityException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException | InstantiationException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return orientation;
    }
}