package com.example.ml_thfood;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;

public class Profile_Activity extends AppCompatActivity {


    TextView result, textViewName, textViewAge, textViewHeight, textViewWeight, bmr, tdee, activity;

    Button editinfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        textViewName = findViewById(R.id.uName);
        textViewAge = findViewById(R.id.uAge);
        textViewHeight = findViewById(R.id.uHeight);
        textViewWeight = findViewById(R.id.uWeight);
        editinfo = findViewById(R.id.editinfo);
        bmr = findViewById(R.id.bmr);
        tdee = findViewById(R.id.tdee);
        activity = findViewById(R.id.activity);

        try {
            // Get the cache directory
            File cacheDir = getCacheDir();

            // Get a list of files in the cache directory
            File[] files = cacheDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    // Filter files to get only those matching the pattern "form_data[NUMBER].txt"
                    return name.matches("formData.txt");
                }
            });

            if (files != null && files.length > 0) {
                // Sort files by suffix number in ascending order
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        int num1 = extractNumber(f1.getName());
                        int num2 = extractNumber(f2.getName());
                        return Integer.compare(num1, num2);
                    }

                    private int extractNumber(String name) {
                        // Extract the number from the file name
                        int lastIndex = name.lastIndexOf('.');
                        int lastIndexBeforeNumber = name.lastIndexOf('[');
                        if (lastIndexBeforeNumber < 0 || lastIndex < 0) return 0;
                        String numberStr = name.substring(lastIndexBeforeNumber + 1, lastIndex);
                        return Integer.parseInt(numberStr);
                    }
                });

                // Read from the latest file
                File latestFile = files[files.length - 1];
                FileInputStream fis = new FileInputStream(latestFile);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

                // Initialize variables to store age, weight, and height
                int age = 0;
                float weight = 0;
                float height = 0;

                // Read each line of the file
                String line;
                String gender = "";
                while ((line = reader.readLine()) != null) {
                    // Split the line by ": " to separate the label and the value
                    String[] parts = line.split(": ");
                    if (parts.length == 2) {
                        // Set the value to the corresponding TextView based on the label
                        switch (parts[0]) {
                            case "Name":
                                textViewName.setText(parts[1]);
                                break;
                            case "Age":
                                textViewAge.setText(parts[1] + " ปี");
                                break;
                            case "Height":
                                textViewHeight.setText(parts[1] + " ซม.");
                                break;
                            case "Weight":
                                textViewWeight.setText(parts[1] + " กก.");
                                break;
                            case "BMR":
                                bmr.setText(parts[1]);
                                break;
                            case "TDEE":
                                tdee.setText(parts[1]);
                                break;
                            case "Activity":
                                activity.setText(parts[1]);
                                break;
                        }
                    }
                }

                // Close the reader
                reader.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Button changeInfoButton = findViewById(R.id.editinfo);
        ImageView back = findViewById(R.id.backtoMain);
        changeInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Profile_Activity.this, Form_activity.class);
                intent.putExtra("isEditing", true); // Indicate editing mode
                startActivity(intent);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Profile_Activity.this, MainActivity.class);
                intent.putExtra("isEditing", true); // Indicate editing mode
                startActivity(intent);
            }
        });


    }
}
