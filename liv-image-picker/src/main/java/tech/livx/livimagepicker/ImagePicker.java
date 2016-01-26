package tech.livx.livimagepicker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ImagePicker class used to wrap Activity Events and provide boilerplate code for image picker
 *
 * @author Limitless Virtual
 * @version 0.1.2
 */

public class ImagePicker {
    //Random activity request code
    private static final int ACTIVITY_REQUEST = 53642;

    //Output uri of selected image
    private android.net.Uri outputFileUri;

    //Wrapped activity
    private Activity context;

    //Output type chosen
    private Output output;

    //Image properties
    private float width;
    private float height;
    private int rotation = 0;

    /**
     * Constructor
     *
     * @param context    Activity reference
     * @param output     Output callback
     * @param width      Maximum width of the image
     * @param height     Maximum height of the image
     */
    public ImagePicker(Activity context, Output output, float width, float height) {

        //Member setters
        this.context = context;
        this.width = width;
        this.height = height;
        this.output = output;
    }

    /**
     * Wraps onCreate of Activity and restores member variables from savedInstanceState of Activity
     *
     * @param savedInstanceState Activities savedInstanceState bundle
     */
    public void onCreate(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            outputFileUri = Uri.parse(savedInstanceState.getString("outputFileUri"));
            rotation = savedInstanceState.getInt("rotation");

            //Decode saved Uri to provide Activity with a fresh instance of the Bitmap (after destruction)
            new DecodeUriAsync().execute(outputFileUri, width, height);
        }
    }

    /**
     * Wraps onActivityResult to retrieve Uri information from the camera or gallery
     *
     * @param requestCode Intent requestCode
     * @param resultCode  Activity resultCode
     * @param data        Result intent data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ACTIVITY_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {

                boolean isCamera;
                //Check if a camera was used or gallery
                if (data == null) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();
                    isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                }

                if (!isCamera)
                    outputFileUri = data.getData();

                //Attempt to decode the returned Uri in a background thread.
                try {
                    new DecodeUriAsync().execute(outputFileUri, width, height);
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    output.onImageLoadFailed();
                }
            }
        }
    }

    /**
     * Wraps activity event onSavedInstanceState to save stat variables.
     *
     * @param savedInstanceState Activity savedInstanceState bundle
     */
    public void onSavedInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("outputFileUri", outputFileUri.toString());
        savedInstanceState.putInt("rotation", rotation);
    }

    /**
     * Initiates pick image intents for camera and gallery apps.
     */
    public void pickImage() {
        //reset variables
        rotation = 0;

        outputFileUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new ContentValues());
        // Camera.
        final List<Intent> cameraIntents = new ArrayList<>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

            //Grant app permissions
            context.grantUriPermission(packageName, outputFileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraIntents.add(intent);
        }

        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_PICK);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Choose image");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        context.startActivityForResult(chooserIntent, ACTIVITY_REQUEST);
    }

    /**
     * Async class used to decode selected Uri
     */
    private class DecodeUriAsync extends AsyncTask<Object, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Object... params) {
            try {

                //Retrieve parameters
                Uri uri = (Uri) params[0];
                float maxHeight = (float) params[1];
                float maxWidth = (float) params[2];

                float maxSize = Math.max(maxHeight, maxWidth);

                //Load actual bounds of image
                BitmapFactory.Options preLoadOptions = new BitmapFactory.Options();
                preLoadOptions.inJustDecodeBounds = true;

                InputStream input = context.getContentResolver().openInputStream(uri);
                BitmapFactory.decodeStream(input, null, preLoadOptions);
                if (input != null)
                    input.close();

                float height = preLoadOptions.outHeight;
                float width = preLoadOptions.outWidth;

                //Calculate scale factor to improve memory by sampling image as close to required size as possible
                int scaleFactor = 1;
                while (true) {
                    if (width / 2 < maxSize || height / 2 < maxSize)
                        break;
                    width /= 2;
                    height /= 2;
                    scaleFactor *= 2;
                }

                BitmapFactory.Options postLoadOptions = new BitmapFactory.Options();
                postLoadOptions.inSampleSize = scaleFactor;

                //Load image with calculated scale factor
                input = context.getContentResolver().openInputStream(uri);
                Bitmap finalBitmap = BitmapFactory.decodeStream(input, null, postLoadOptions);
                if (input != null)
                    input.close();

                //Calculate further required scaling
                float scale;
                if (width > height) {
                    scale = maxHeight / height;
                } else {
                    scale = maxWidth / width;
                }

                float left = ((maxWidth - (scale*width)) / 2);
                float top = ((maxHeight - (scale*height)) / 2);

                //Create final bitmap

                Matrix matrix = new Matrix();

                //Apply scaling calculation

                matrix.setTranslate(-(width / 2), -(height / 2));

                //Calculate any required rotations
                float tempRotation = rotation + ExifUtil.getExifOrientationFromGallery(context, uri);

                matrix.postRotate(tempRotation);

                Bitmap dest;
                if((tempRotation/ 90) % 2 != 0) {
                    dest = Bitmap.createBitmap((int) maxHeight, (int) maxWidth, Bitmap.Config.ARGB_8888);
                    matrix.postTranslate(height/2, width/2);
                    matrix.postScale(scale, scale);
                    matrix.postTranslate(top,left);
                } else {
                    dest = Bitmap.createBitmap((int) maxWidth, (int) maxHeight, Bitmap.Config.ARGB_8888);
                    matrix.postTranslate(width/2, height/2);
                    matrix.postScale(scale, scale);
                    matrix.postTranslate(left, top);
                }

                Canvas canvas = new Canvas(dest);

                //Draw final bitmap
                canvas.drawARGB(255, 255, 0, 0);
                Paint circle = new Paint();
                circle.setColor(Color.argb(255, 255, 0, 0));
                canvas.drawBitmap(finalBitmap, matrix, null);
                finalBitmap.recycle();
                return dest;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            output.process(bitmap);
        }
    }

    /**
     * Rotates image incrementally
     */
    public void rotateImage() {
        if(outputFileUri != null) {
            rotation+= 90;
            new DecodeUriAsync().execute(outputFileUri, width, height);
        }
    }
}
