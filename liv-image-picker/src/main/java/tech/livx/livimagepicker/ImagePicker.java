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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;

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
    public static final int IMAGE_PICK_TYPE_CAMERA_ONLY = 0;
    public static final int IMAGE_PICK_TYPE_GALLERY_ONLY = 1;
    public static final int IMAGE_PICK_TYPE_CAMERA_AND_GALLERY = 2;

    //Random activity request code
    private static final int ACTIVITY_REQUEST = 53642;

    //Output uri of selected image
    private android.net.Uri outputFileUri;
    private android.net.Uri newFileUri;

    //Wrapped activity
    private Activity context;

    //Output type chosen
    private Output output;

    //Image properties
    private float width;
    private float height;
    private int rotation = 0;
    private boolean isCamera;
    private boolean exact;
    private final Object lock = new Object();
    private static boolean isRunning = false;

    //Thread instance
    private DecodeUriAsync decodeUriAsync;

    /**
     * Constructor
     *
     * @param context Activity reference
     * @param output  Output callback
     * @param width   Maximum width of the image
     * @param height  Maximum height of the image
     * @param exact   Flag to specify if the image must be exactly the width and height, if false then it will be at most the height and width but not exactly.
     */
    public ImagePicker(Activity context, Output output, float width, float height, boolean exact) {

        //Member setters
        this.context = context;
        this.width = width;
        this.height = height;
        this.output = output;
        this.exact = exact;
    }

    /**
     * Wraps onCreate of Activity and restores member variables from savedInstanceState of Activity
     *
     * @param savedInstanceState Activities savedInstanceState bundle
     */
    public void onCreate(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            if(savedInstanceState.getString("outputFileUri") == null)
                outputFileUri = null;
            else
                outputFileUri = Uri.parse(savedInstanceState.getString("outputFileUri"));
            rotation = savedInstanceState.getInt("rotation");
            isCamera = savedInstanceState.getBoolean("isCamera");

            //Decode saved Uri to provide Activity with a fresh instance of the Bitmap (after destruction)
            if(outputFileUri != null && !isRunning) {
                decodeUriAsync = new DecodeUriAsync();
                decodeUriAsync.execute(outputFileUri, width, height, isCamera);
            }
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

                if (data == null) {
                    //Some camera apps give null data
                    isCamera = true;
                } else {
                    //Others give data containing the ACTION_IMAGE_CAPTURE
                    final String action = data.getAction();
                    isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                }

                if (!isCamera)
                    outputFileUri = data.getData();
                else
                    outputFileUri = newFileUri;

                //Attempt to decode the returned Uri in a background thread.
                try {
                    if(!isRunning && outputFileUri != null) {
                        decodeUriAsync = new DecodeUriAsync();
                        decodeUriAsync.execute(outputFileUri, width, height, isCamera);
                    }
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
        if(outputFileUri == null)
            savedInstanceState.putString("outputFileUri", null);
        else
            savedInstanceState.putString("outputFileUri", outputFileUri.toString());
        savedInstanceState.putInt("rotation", rotation);
        savedInstanceState.putBoolean("isCamera", isCamera);
    }

    /**
     * Initiates pick image intents for camera and gallery apps.
     *
     * @param imagePickType Type of pick eg IMAGE_PICK_TYPE_CAMERA_ONLY
     */
    public void pickImage(int imagePickType) {
        //reset variables
        rotation = 0;

        //Insert a URI into the gallery for our new captured image and hold onto it
        newFileUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());

        //Ensure that uri was able to be created
        if(newFileUri == null)
            return;

        switch (imagePickType) {
            case IMAGE_PICK_TYPE_CAMERA_AND_GALLERY : {
                context.startActivityForResult(createGalleryAndCameraIntent(newFileUri), ACTIVITY_REQUEST);
                break;
            }
            case IMAGE_PICK_TYPE_CAMERA_ONLY : {
                context.startActivityForResult(createCameraIntent(newFileUri), ACTIVITY_REQUEST);
                break;
            }
            case IMAGE_PICK_TYPE_GALLERY_ONLY : {
                context.startActivityForResult(createGalleryIntent(), ACTIVITY_REQUEST);
                break;
            }
        }
    }

    private Intent createGalleryAndCameraIntent(Uri cameraOutputUri) {

        //List of camera intents
        final List<Intent> cameraIntents = new ArrayList<>();

        // Capture intent
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        //Query package manager for all activities that filter ACTION_IMAGE_CAPTURE intents
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);

        //Loop through all available activities, create intents for them and add them to our cameraIntents list
        for (ResolveInfo res : listCam) {
            String packageName = res.activityInfo.packageName;
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);
            cameraIntents.add(intent);
        }

        // Filesystem.
        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_PICK);

        // Chooser of filesystem options.
        Intent chooserIntent = Intent.createChooser(galleryIntent, "Choose image");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        return chooserIntent;
    }

    private Intent createGalleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_PICK);

        return intent;
    }

    private Intent createCameraIntent(Uri cameraOutputUri) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);

        return intent;
    }

    /**
     * Async class used to decode selected Uri
     */
    private class DecodeUriAsync extends AsyncTask<Object, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Object... params) {
            try {
                synchronized (lock) {
                    isRunning = true;

                    //Retrieve parameters
                    Uri uri = (Uri) params[0];
                    Log.i("URI", uri.toString());
                    float maxHeight = (float) params[1];
                    float maxWidth = (float) params[2];
                    boolean isCamera = (boolean) params[3];

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

                    //Start off with a 1:1 ratio (Full size)
                    int scaleFactor = 1;

                    while (true) {
                        if (width / 2 < maxSize || height / 2 < maxSize)
                            break;
                        width /= 2;
                        height /= 2;
                        scaleFactor *= 2;
                    }

                    //Calculate further required scaling
                    float scale = 1.0f;
                    if (exact) {
                        if (width > height) {
                            scale = maxHeight / height;
                        } else {
                            scale = maxWidth / width;
                        }
                    } else {
                        //Not exact so scale by one more factor to ensure in max bounds.
                        if (width > maxWidth || height > maxHeight) {
                            width /= 2;
                            height /= 2;
                            scaleFactor *= 2;
                        }
                    }

                    BitmapFactory.Options postLoadOptions = new BitmapFactory.Options();
                    postLoadOptions.inSampleSize = scaleFactor;

                    //Load image with calculated scale factor
                    input = context.getContentResolver().openInputStream(uri);
                    Bitmap finalBitmap = BitmapFactory.decodeStream(input, null, postLoadOptions);
                    if (input != null)
                        input.close();

                    if (!exact) {
                        maxHeight = height;
                        maxWidth = width;
                    }

                    float left = ((maxWidth - (scale * width)) / 2);
                    float top = ((maxHeight - (scale * height)) / 2);

                    //Create final bitmap
                    Matrix matrix = new Matrix();

                    //Apply scaling calculation
                    matrix.setTranslate(-(width / 2), -(height / 2));

                    //Calculate any required rotations
                    float tempRotation;
                    if (isCamera) {
                        tempRotation = rotation + ExifUtil.getExifOrientationFromCamera(context, uri);
                    } else {
                        tempRotation = rotation + ExifUtil.getExifOrientationFromGallery(context, uri);
                    }

                    matrix.postRotate(tempRotation);

                    //Crashes sometimes by settings width and height to 0
                    if (maxWidth == 0 || maxHeight == 0)
                        return null;

                    Bitmap dest;
                    if ((tempRotation / 90) % 2 != 0) {
                        dest = Bitmap.createBitmap((int) maxHeight, (int) maxWidth, Bitmap.Config.ARGB_8888);
                        matrix.postTranslate(height / 2, width / 2);
                        matrix.postScale(scale, scale);
                        matrix.postTranslate(top, left);
                    } else {
                        dest = Bitmap.createBitmap((int) maxWidth, (int) maxHeight, Bitmap.Config.ARGB_8888);
                        matrix.postTranslate(width / 2, height / 2);
                        matrix.postScale(scale, scale);
                        matrix.postTranslate(left, top);
                    }

                    Canvas canvas = new Canvas(dest);

                    //Draw final bitmap
                    canvas.drawBitmap(finalBitmap, matrix, new Paint());
                    finalBitmap.recycle();
                    return dest;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            isRunning = false;
            if(bitmap != null)
                output.process(bitmap);
            else
                output.onImageLoadFailed();
        }
    }

    /**
     * Rotates image incrementally
     */
    public void rotateImage() {

        if (outputFileUri != null) {
            rotation += 90;
            if(!isRunning) {
                decodeUriAsync = new DecodeUriAsync();
                Log.i("URI", String.format("Trying rotation with %s", outputFileUri.toString()));
                decodeUriAsync.execute(outputFileUri, width, height, isCamera);
            }
        }
    }
}
