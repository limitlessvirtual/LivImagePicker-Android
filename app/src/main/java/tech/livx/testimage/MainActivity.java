package tech.livx.testimage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import tech.livx.livimagepicker.ImagePicker;
import tech.livx.livimagepicker.OutputBitmap;

public class MainActivity extends AppCompatActivity {
    private ImagePicker imagePicker;
    private View button;
    private ImageView demo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imagePicker = new ImagePicker(this,
                new OutputBitmap() {
                    @Override
                    public void onImageLoaded(Bitmap image) {
                        demo.setImageBitmap(image);
                    }

                    @Override
                    public void onImageLoadFailed() {

                    }
                },
                600, 600, false);

        demo = (ImageView) findViewById(R.id.demo);

        imagePicker.onCreate(savedInstanceState);
        button = findViewById(R.id.image);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePicker.pickImage();
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
