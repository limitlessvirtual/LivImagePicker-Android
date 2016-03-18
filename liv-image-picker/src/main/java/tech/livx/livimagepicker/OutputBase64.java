package tech.livx.livimagepicker;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/**
 * Output image as Base64 String
 */
public abstract class OutputBase64 implements Output<String> {
    @Override
    public void process(final Uri uri,Bitmap bitmap) {
        new AsyncTask<Bitmap, Void, String>() {
            @Override
            protected String doInBackground(Bitmap... params) {

                //Create Byte Array Stream from image
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                params[0].compress(Bitmap.CompressFormat.PNG, 100, stream);

                //Encode Stream as Base64 String
                return Base64.encodeToString(stream.toByteArray(),
                        Base64.NO_WRAP);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                onImageLoaded(uri,s);
            }
        }.execute(bitmap);
    }
}