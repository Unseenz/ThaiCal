package com.example.ml_thfood;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.ml_thfood.ml.IncModelTensor;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    Button selectBtn, captureBtn;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //permission
        getPermission();

        selectBtn = findViewById(R.id.selectBtn);

        captureBtn = findViewById(R.id.captureBtn);


        // Capture button click listener
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 12);
            }
        });


        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 12);
            }
        });

        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 10);
            }
        });

        ImageView profile = findViewById(R.id.profile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Profile_Activity.class);
                startActivity(intent);
            }
        });


    }

    int getMax(float[] arr) {
        int max = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > arr[max]) max = i;
        }
        return max;
    }

    void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 11);
            }
        }
    }

    private void performPrediction(Bitmap bitmap) {
        try {
            // Load the TensorFlow Lite model
            IncModelTensor model = IncModelTensor.newInstance(MainActivity.this);

            // Resize the bitmap to (224x224)
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, false);

            // Normalize pixel values to [0, 1]
            resizedBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true);
            int[] pixels = new int[224 * 224];
            resizedBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224);
            float[] normalizedPixels = new float[pixels.length * 3];
            for (int i = 0; i < pixels.length; ++i) {
                final int val = pixels[i];
                normalizedPixels[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
                normalizedPixels[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
                normalizedPixels[i * 3 + 2] = (val & 0xFF) / 255.0f;
            }

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(normalizedPixels, new int[]{1, 224, 224, 3});

            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);

            // Load the pixel values from the TensorImage into the TensorBuffer
            inputFeature0.loadBuffer(tensorImage.getBuffer());

            // Run model inference and get the result
            IncModelTensor.Outputs outputs = model.process(inputFeature0);

            // Get the output tensor buffer
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] probabilities = outputFeature0.getFloatArray();
            boolean found = false;

            for (float probability : probabilities) {
                if (probability > 0.5) {
                    found = true;
                    break;
                }
            }

            float maxProbability = -1;

            for (int i = 0; i < probabilities.length; i++) {
                Log.d("ModelOutput", "Class " + i + ": " + outputFeature0.getFloatArray()[i]);
                if (probabilities[i] > maxProbability) {
                    maxProbability = probabilities[i];
                }
            }
            Log.d("ModelOutput", "Highest Probability is " + maxProbability);

            // Get the index of the maximum value in the output tensor buffer
            int predictedClassIndex = getMax(outputFeature0.getFloatArray());//getmax
            String predictedLabel = String.valueOf(predictedClassIndex);
            InputStream inputStream = getAssets().open("calories.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String jsonString = new String(buffer, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject labelsObject = jsonObject.getJSONObject("labels");


            JSONObject predictlabelObj = labelsObject.getJSONObject(predictedLabel);
            String predictedName = predictlabelObj.getString("name");
            int calories = predictlabelObj.getInt("cal");


            // Pass the prediction result to the next activity
            Intent intent = new Intent(MainActivity.this, Second_Activity.class);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            intent.putExtra("imageBitmap", byteArray);
            intent.putExtra("predictedLabel", predictedLabel);
            intent.putExtra("predictedName", predictedName);
            intent.putExtra("calories", calories);
            intent.putExtra("showAlert", !found);
            startActivity(intent);

            model.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 11) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 10) {
            if (data != null) {
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    performPrediction(bitmap);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        } else if (requestCode == 12) {
            bitmap = (Bitmap) data.getExtras().get("data");
            performPrediction(bitmap);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}