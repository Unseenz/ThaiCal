package com.example.ml_thfood;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Form_activity extends AppCompatActivity {

    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isEditing = getIntent().getBooleanExtra("isEditing", false);
        if (!isEditing && isFormAlreadyCompleted()) {
            startMainActivity();
            finish();
            return;
        }
        setContentView(R.layout.activity_form);
        viewPager = findViewById(R.id.viewPager);
        FormPagerAdapter adapter = new FormPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
    }

    private boolean isFormAlreadyCompleted() {
        File file = new File(getCacheDir(), "formData.txt");
        return file.exists();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }


    private static void writeDataToCache(Context context, String fileName, String data, boolean append) {
        FileOutputStream fos = null;
        try {
            File file = new File(context.getCacheDir(), fileName);
            fos = new FileOutputStream(file, append); // Set append to true to append data
            fos.write(data.getBytes());
            Log.d("CacheWrite", "Data written to cache file: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("CacheError", "Failed to write data to cache: " + e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static String readDataFromCache(Context context, String fileName) {
        try {
            File file = new File(context.getCacheDir(), fileName);
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CacheReadError", "Failed to read data from cache: " + e.getMessage());
            return "";
        }
    }

    private class FormPagerAdapter extends androidx.fragment.app.FragmentPagerAdapter {
        public FormPagerAdapter(androidx.fragment.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public androidx.fragment.app.Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new FormPage1Fragment();
                case 1:
                    return new FormPage2Fragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public static class FormPage1Fragment extends androidx.fragment.app.Fragment {
        private EditText editTextName, editTextAge, editTextHeight, editTextWeight;
        private RadioGroup radioGroupGender;
        private Button btnNext;

        ImageView imageView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.form_page_1, container, false);
            editTextName = view.findViewById(R.id.editTextName);
            editTextAge = view.findViewById(R.id.editTextAge);
            editTextHeight = view.findViewById(R.id.editTextHeight);
            editTextWeight = view.findViewById(R.id.editTextWeight);
            radioGroupGender = view.findViewById(R.id.radioGroupGender);
            btnNext = view.findViewById(R.id.BtnNext);

            loadData();

            radioGroupGender.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    // Hide keyboard when any radio button is selected
                    View radioButton = radioGroupGender.findViewById(checkedId);
                    if (radioButton != null && getContext() != null) {
                        hideKeyboard(radioButton);
                    }
                }
            });


            btnNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(validateForm()) {
                        String name = editTextName.getText().toString();
                        int age = Integer.parseInt(editTextAge.getText().toString());
                        double height = Double.parseDouble(editTextHeight.getText().toString());
                        double weight = Double.parseDouble(editTextWeight.getText().toString());
                        int selectedId = radioGroupGender.getCheckedRadioButtonId();
                        RadioButton radioButton = view.findViewById(selectedId);
                        String gender = radioButton != null ? radioButton.getText().toString() : "Not specified";

                        double bmrValue = calculateBMR(age, height, weight, gender);
                        String formattedBMR = String.format("%.2f", bmrValue);
                        String data = "Name: " + name + "\nAge: " + age + "\nHeight: " + height + "\nWeight: " + weight + "\nGender: " + gender + "\nBMR: " + formattedBMR;

                        writeDataToCache(getActivity(), "formData.txt", data, false);

                        ((ViewPager)getActivity().findViewById(R.id.viewPager)).setCurrentItem(1, true);
                    } else {
                        showAlertDialog();
                    }
                }
            });

            return view;
        }

        private void loadData() {
            // Ensure the fragment is attached and views are initialized
            if (getActivity() == null || !isAdded()) {
                Log.e("FragmentError", "Fragment not attached to an activity.");
                return;
            }

            String data = readDataFromCache(getActivity(), "formData.txt");
            if (data == null || data.isEmpty()) {
                Log.e("DataLoadError", "No data found in cache.");
                return;
            }

            try {
                Pattern pattern = Pattern.compile("Name: (.*)\\nAge: (\\d+)\\nHeight: (\\d+(\\.\\d+)?)\\nWeight: (\\d+(\\.\\d+)?)\\nGender: (.*?)\\n");

                Matcher matcher = pattern.matcher(data);
                if (matcher.find()) {
                    editTextName.setText(matcher.group(1));
                    editTextAge.setText(matcher.group(2));
                    editTextHeight.setText(matcher.group(3));
                    editTextWeight.setText(matcher.group(5));
                    String gender = matcher.group(7).trim();

                    boolean genderFound = false;
                    for (int i = 0; i < radioGroupGender.getChildCount(); i++) {
                        RadioButton rb = (RadioButton) radioGroupGender.getChildAt(i);
                        if (rb.getText().toString().equalsIgnoreCase(gender)) {
                            rb.setChecked(true);
                            genderFound = true;
                            break;
                        }
                    }

                    if (!genderFound) {
                        Log.e("GenderError", "No radio button matches the gender: " + gender);
                    }
                } else {
                    Log.e("RegexError", "Data does not match the expected format.");
                }
            } catch (Exception e) {
                Log.e("LoadDataException", "Error parsing data: " + e.getMessage());
            }
        }


        private double calculateBMR(int age, double height, double weight, String gender) {
            if (gender.equalsIgnoreCase("male")) {
                return 10 * weight + 6.25 * height - 5 * age + 5;
            } else if (gender.equalsIgnoreCase("female")) {
                return 10 * weight + 6.25 * height - 5 * age - 161;
            } else {
                return -1; // Invalid BMR for unspecified gender
            }
        }
        private boolean validateForm() {

            boolean nameFilled = !editTextName.getText().toString().isEmpty();
            boolean ageFilled = !editTextAge.getText().toString().isEmpty();
            boolean heightFilled = !editTextHeight.getText().toString().isEmpty();
            boolean weightFilled = !editTextWeight.getText().toString().isEmpty();
            boolean genderSelected = radioGroupGender.getCheckedRadioButtonId() != -1;

            return nameFilled && ageFilled && heightFilled && weightFilled && genderSelected;
        }

        private void showAlertDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Incomplete Form");
            builder.setMessage("Please fill out all fields to proceed.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        private void hideKeyboard(View view) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

    }

    public static class FormPage2Fragment extends androidx.fragment.app.Fragment {
        private RadioGroup radioGroup;
        private Button btnSubmit;
        private boolean isEditing = false;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.form_page_2, container, false);

            radioGroup = view.findViewById(R.id.radioGroupTDEE);
            btnSubmit = view.findViewById(R.id.BtnSubmit);
            // Set submit button initially to GONE
            btnSubmit.setVisibility(View.GONE);

            if (getActivity() != null) {
                isEditing = getActivity().getIntent().getBooleanExtra("isEditing", false);
            }

            //  radio button selection changes
            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    // Show submit button only if a choice is made
                    if (checkedId != -1) {
                        btnSubmit.setVisibility(View.VISIBLE);
                    } else {
                        btnSubmit.setVisibility(View.GONE);
                    }
                }
            });

            btnSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    submitData();
                }
            });

            ImageView backToF1 = view.findViewById(R.id.backtoF1);
            backToF1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (getActivity() != null && getActivity().findViewById(R.id.viewPager) instanceof ViewPager) {
                        ViewPager viewPager = getActivity().findViewById(R.id.viewPager);
                        viewPager.setCurrentItem(0);  // Move back to the first page
                    }
                }
            });

            loadData();

            return view;
        }

        private void submitData() {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = getView().findViewById(selectedId);
                double activityFactor = Double.parseDouble(selectedRadioButton.getTag().toString());
                String activity = selectedRadioButton.getText().toString();

                String existingData = readDataFromCache(getActivity(), "formData.txt");
                double bmr = parseBMRFromData(existingData);
                double tdee = bmr * activityFactor;
                String formatTDEE = String.format("%.2f", tdee);
                String data = "\nActivity: " + activity + "\nActivity Level: " + activityFactor + "\nTDEE: " + formatTDEE;

                writeDataToCache(getActivity(), "formData.txt", data, true);

                if (isEditing) {
                    navigateToProfileActivity();
                } else {
                    navigateToMainActivity(getActivity());
                }
            }
        }

        private void loadData() {
            String data = readDataFromCache(getActivity(), "formData.txt");
            // Using a simple pattern that directly captures the Activity Level.
            Pattern pattern = Pattern.compile("Activity Level: (\\d+\\.\\d+)");
            Matcher matcher = pattern.matcher(data);

            if (matcher.find()) {
                String activityLevel = matcher.group(1).trim();
                boolean found = false;

                for (int i = 0; i < radioGroup.getChildCount(); i++) {
                    View child = radioGroup.getChildAt(i);
                    if (child instanceof RadioButton) {
                        RadioButton rb = (RadioButton) child;
                        if (rb.getTag() != null && rb.getTag().toString().equals(activityLevel)) {
                            rb.setChecked(true);
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    Log.e("ActivityLevel", "No matching RadioButton for activity level: " + activityLevel);
                }
            } else {
                Log.e("ActivityLevel", "Activity Level not found in data.");
            }
        }

        private double parseBMRFromData(String data) {
            // Extract BMR value from data string
            Pattern p = Pattern.compile("BMR: (\\d+(\\.\\d+)?)");
            Matcher m = p.matcher(data);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
            return -1; // Return invalid BMR if not found
        }

        private void navigateToMainActivity(Context context) {
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
            getActivity().finish();
        }

        private void navigateToProfileActivity() {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), Profile_Activity.class);
                startActivity(intent);
                getActivity().finish();
            }
        }
    }


}
