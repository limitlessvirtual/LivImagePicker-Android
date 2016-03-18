package tech.livx.testimage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import tech.livx.livimagepicker.ImagePicker;
import tech.livx.livimagepicker.OutputBitmap;

public class MainActivity extends AppCompatActivity {
    private ImagePicker imagePicker;
    private ImageView demo;

    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imagePicker = new ImagePicker(this,
                new OutputBitmap() {
                    @Override
                    public void onImageLoaded(Uri uri, Bitmap image) {
                        bitmap = image;

                        demo.setImageURI(uri);
                    }

                    @Override
                    public void onImageLoadFailed() {
                        Log.i("TEST","FAILED");
                    }
                },
                300, 300, false);

        demo = (ImageView) findViewById(R.id.demo);

        imagePicker.onCreate(savedInstanceState);

        View rotate = findViewById(R.id.rotate);
        rotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePicker.rotateImage();
            }
        });

        View button = findViewById(R.id.image);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePicker.pickImage(ImagePicker.IMAGE_PICK_TYPE_CAMERA_AND_GALLERY);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        imagePicker.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        imagePicker.onSavedInstanceState(outState);
    }
}
