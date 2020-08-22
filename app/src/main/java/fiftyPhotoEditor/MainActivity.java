package fiftyPhotoEditor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.print.PrintAttributes;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Target;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.harvard.cs50.fiftygram.R;
import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import jp.wasabeef.glide.transformations.CropCircleWithBorderTransformation;
import jp.wasabeef.glide.transformations.CropSquareTransformation;
import jp.wasabeef.glide.transformations.GrayscaleTransformation;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import jp.wasabeef.glide.transformations.gpu.BrightnessFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.ContrastFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.InvertFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.KuwaharaFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.PixelationFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.SepiaFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.SketchFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.SwirlFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.ToonFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.VignetteFilterTransformation;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private ImageView imageView;
    private Bitmap original;
    private Bitmap newImage;
    private SeekBar Brightness;
    private SeekBar Contraste;

    Transformation<Bitmap> filter;
    Transformation<Bitmap> crop = null;

    private boolean iscropCircle = false;
    private boolean iscropSquare = false;

    private float BrightnessValue = 0.0f;
    private float ContrastValue = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Brightness = findViewById(R.id.seekBarBrightest);
        Contraste = findViewById(R.id.seekBarContrast);
        imageView = findViewById(R.id.image_view);

        Brightness.setProgress(10);
        Contraste.setProgress(10);

        // ContrastSeekBar
        Contraste.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                float auxValueProgress = 0.0f;

                if(progress > 10){
                    auxValueProgress = ((float) (((20 - (progress - 10)) - 10) * (0.1)));

                }else if (progress < 10) {
                    auxValueProgress = ((float) ((progress) * (-0.1)));

                }else{
                    auxValueProgress = 1.0f;

                }
                ContrastValue = auxValueProgress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                apply(filter);
            }
        });

        // BrightnessSeekBar
        Brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                float auxValueProgress = 0.0f;

                if(progress > 10){
                    auxValueProgress = ((float) ((progress - 10) * (0.1)));

                }else if (progress < 10) {
                    auxValueProgress = ((float) ((10 - progress) * (-0.1)));

                }else{
                    auxValueProgress = 0.0f;

                }
                BrightnessValue = auxValueProgress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                apply(filter);
            }
        });

        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Get file number
    private int getNumFile (){
        SharedPreferences sharedPreferences = getSharedPreferences("NumFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int num_value = sharedPreferences.getInt("NUM_FILE", 0);
        int i = 0;

        if (num_value == 0) {
            editor.putInt("NUM_FILE", 1);
            editor.apply();

        }else{
            i = num_value += 1;
            editor.putInt("NUM_FILE", i);
            editor.apply();

            return (i -1);

        }

        return 0;
    }

    // Create folder directory
    private File getFileDirectory(){
        File mediaStore = new File(Environment.getExternalStorageDirectory()
                + "/DCIM"
                + "/50PhotoEditor");

        if(! mediaStore.exists()){
            if(! mediaStore.mkdirs()){
                return null;
            }
        }

        String time = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        String mImageName = "50PhotoEditor_" + String.valueOf(getNumFile()) + ".jpg";
        File mediaFile = new File(mediaStore.getPath() + File.separator + mImageName);

        return mediaFile;
    }

    // Save image on folder 50PhotoEditor
    public void saveImage(View view){
        File fileOut = getFileDirectory();
        FileOutputStream fileOutputStream;

        if(fileOut != null) {
            try {
                fileOutputStream = new FileOutputStream(fileOut);
                newImage.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
                fileOutputStream.close();

                Toast toast = Toast.makeText(getApplicationContext(), "Image saved!", Toast.LENGTH_LONG);
                toast.show();

            } catch (FileNotFoundException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }

    // Apply filter image
    public void apply(Transformation<Bitmap> filter) {
        MultiTransformation<Bitmap> multiTransformation = null;

        MultiTransformation<Bitmap> BrigCont = new MultiTransformation<>(
                new BrightnessFilterTransformation(BrightnessValue),
                new ContrastFilterTransformation(ContrastValue));

        if (filter != null && crop != null) {
            multiTransformation = new MultiTransformation<>(BrigCont, filter, crop);

        }else if (filter == null && crop == null){
            multiTransformation = new MultiTransformation<>(BrigCont);

        }else if (filter == null){
            multiTransformation = new MultiTransformation<>(BrigCont, crop);

        }else if (crop == null){
            multiTransformation = new MultiTransformation<>(BrigCont, filter);
            
        }

        if (original != null) {
            Glide
                .with(this)
                .asBitmap()
                .load(original)
                .apply(RequestOptions.bitmapTransform(multiTransformation))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        imageView.setImageBitmap(resource);
                        newImage = resource;

                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
        }
    }

    // Get Image
    public void choosePhoto(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(intent, 1);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                ParcelFileDescriptor parcelFileDescriptor =
                        getContentResolver().openFileDescriptor(uri, "r");

                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                original = BitmapFactory.decodeFileDescriptor(fileDescriptor);

                imageView.setImageTintMode(null);

                parcelFileDescriptor.close();
                imageView.setImageBitmap(original);

            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }


    // Filter Proto (Glide transformation)
    public void applyNormal(View view) {
        filter = null;

        BrightnessValue = 0.0f;
        ContrastValue = 1.0f;

        Brightness.setProgress(10);
        Contraste.setProgress(10);

        crop = null;

        apply(filter);
    }

    public void applySepia(View view) {
        filter = new SepiaFilterTransformation();
        apply(filter);
    }

    public void applyToon(View view) {
        filter = new ToonFilterTransformation();
        apply(filter);
    }

    public void applySketch(View view) {
        filter = new SketchFilterTransformation();
        apply(filter);
    }

    public void applyPixelation(View view){
        filter = new PixelationFilterTransformation();
        apply(filter);
    }

    public void applyBlur(View view){
        filter = new BlurTransformation();
        apply(filter);
    }

    public void applyKuwahara(View view){
        filter = new KuwaharaFilterTransformation();
        apply(filter);
    }

    public void applyGrayScale(View view){
        filter = new GrayscaleTransformation();
        apply(filter);
    }

    public void applySwirl(View view){
        filter = new SwirlFilterTransformation();
        apply(filter);
    }

    public void applyVignette(View view){
        filter = new VignetteFilterTransformation();
        apply(filter);
    }


    //Crop photo (Glide transformation)
    public void applyWithBorder(View view){
        if(iscropCircle == false) {
            crop = new CircleCrop();
            iscropCircle = true;

        }else{
            crop = null;
            iscropCircle = false;

        }
        apply(filter);
    }

    public void applySquare(View view){
        if (iscropSquare == false) {
            crop = new CropSquareTransformation();
            iscropSquare = true;

        }else{
            crop = null;
            iscropSquare = false;

        }
        apply(filter);
    }
}
