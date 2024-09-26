package com.example.ml_thfood;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class Second_Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);        TextView resultTwo = findViewById(R.id.result);



        TextView caloriesTextView = findViewById(R.id.caloriesText);


        ImageView backButton = findViewById(R.id.backButton);
        RadioGroup meatRadioGroup = findViewById(R.id.meatRadioGroup);
        TextView selectmeat = findViewById(R.id.selectmeat);
        TextView calper = findViewById(R.id.calpercen);

        double tdee = readTdeeFromCache();

        byte[] byteArray = getIntent().getByteArrayExtra("imageBitmap");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inDensity = DisplayMetrics.DENSITY_DEFAULT;

        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        // Display the image bitmap in an ImageView
        ImageView imageView = findViewById(R.id.foodpic);
        imageView.setImageBitmap(bitmap);

        // Retrieve the predicted label from the intent
        String predictedLabel = getIntent().getStringExtra("predictedLabel");
        String predictedName = getIntent().getStringExtra("predictedName");
        int calories = getIntent().getIntExtra("calories", 0);


        if (getIntent().getBooleanExtra("showAlert", false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Can't detect food")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }else{
            resultTwo.setText(predictedName);

            caloriesTextView.setText(calories + " Kcal");

            double percentage = (calories / tdee) * 100;

            calper.setText(String.format("%.0f%%", percentage));
        }


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMainActivity();
            }
        });

        if ("6".equals(predictedLabel)||"1".equals(predictedLabel)) {
            // Show the RadioGroup for meat selection

            meatRadioGroup.setVisibility(View.VISIBLE);
            selectmeat.setVisibility(View.VISIBLE);


            meatRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    RadioButton selectedRadioButton = findViewById(checkedId);
                    String selectedMeat = selectedRadioButton.getText().toString();
                    int caloriesmeat = calculateCalories(predictedLabel, selectedMeat);
                    double percentage = (caloriesmeat / tdee) * 100;
                    // Display the total calories
                    caloriesTextView.setText(caloriesmeat+ " Kcal");
                    calper.setText(String.format("%.0f%%", percentage));
                    caloriesTextView.setVisibility(View.VISIBLE); // Show calories TextView
                }
            });

        } else {

            meatRadioGroup.setVisibility(View.GONE);
            selectmeat.setVisibility(View.GONE);
        }

    }

    private double readTdeeFromCache() {
        File cacheDir = getCacheDir();
        File formDataFile = new File(cacheDir, "FormData.txt");
        double tdee = 2516.81;

        try (BufferedReader reader = new BufferedReader(new FileReader(formDataFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("TDEE:")) {
                    tdee = Double.parseDouble(line.split(":")[1].trim());
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tdee;
    }

    private void startMainActivity() {
        Intent intent = new Intent(Second_Activity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private int calculateCalories(String predictedLabel, String selectedMeat) {
        int baseCalories = 0;
        if (predictedLabel.equals("6")) {
            if (selectedMeat.equals("Beef")) {
                baseCalories = 870;
            } else if (selectedMeat.equals("Chicken")) {
                baseCalories = 522;
            } else if (selectedMeat.equals("Pork")) {
                baseCalories = 609;
            }
        }else {
            if (selectedMeat.equals("Beef")) {
                baseCalories = 360;
            } else if (selectedMeat.equals("Chicken")) {
                baseCalories = 278;
            } else if (selectedMeat.equals("Pork")) {
                baseCalories = 408;
            }
        }


        return baseCalories;
    }


}