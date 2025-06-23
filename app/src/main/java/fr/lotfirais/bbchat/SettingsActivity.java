// app/java/fr/lotfirais/berryai/SettingsActivity.java
package fr.lotfirais.bbchat;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import fr.lotfirais.bbchat.util.AppSettingsManager; // Import new manager

public class SettingsActivity extends Activity {

    private EditText apiKeyEditText;
    private Spinner modelSpinner;
    private CheckBox googleSearchCheckBox;
    private Button saveSettingsButton;
    private Button returnButton;

    private AppSettingsManager appSettingsManager; // Use the new manager

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_settings);

        // Initialize AppSettingsManager
        appSettingsManager = new AppSettingsManager(getApplicationContext());

        apiKeyEditText = (EditText) findViewById(R.id.apiKeyEditText);
        modelSpinner = (Spinner) findViewById(R.id.modelSpinner);
        googleSearchCheckBox = (CheckBox) findViewById(R.id.googleSearchCheckBox);
        saveSettingsButton = (Button) findViewById(R.id.saveSettingsButton);
        returnButton = (Button) findViewById(R.id.returnButton);

        List<String> models = new ArrayList<>();
        models.add("gemini-2.5-pro");
        models.add(AppSettingsManager.DEFAULT_GEMINI_MODEL);
        models.add("gemini-2.0-flash");
        models.add("gemini-2.0-flash-lite");
        models.add("gemini-1.5-pro");
        models.add("gemini-1.5-flash");

        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, models);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelAdapter);

        loadSettings();

        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void loadSettings() {
        apiKeyEditText.setText(appSettingsManager.getGeminiApiKey());

        String savedModel = appSettingsManager.getGeminiModel();
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) modelSpinner.getAdapter();
        int modelPosition = adapter.getPosition(savedModel);
        if (modelPosition >= 0) {
            modelSpinner.setSelection(modelPosition);
        } else {
            // Fallback to default if saved model isn't in the list
            modelSpinner.setSelection(adapter.getPosition(AppSettingsManager.DEFAULT_GEMINI_MODEL));
        }

        googleSearchCheckBox.setChecked(appSettingsManager.isGoogleSearchEnabled());
    }

    private void saveSettings() {
        String apiKey = apiKeyEditText.getText().toString().trim();
        String selectedModel = (String) modelSpinner.getSelectedItem();
        boolean enableGoogleSearch = googleSearchCheckBox.isChecked();

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "The API key cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedModel == null || selectedModel.isEmpty()) {
            Toast.makeText(this, "Please select a Gemini model.", Toast.LENGTH_SHORT).show();
            return;
        }

        appSettingsManager.saveGeminiApiKey(apiKey);
        appSettingsManager.saveGeminiModel(selectedModel);
        appSettingsManager.setGoogleSearchEnabled(enableGoogleSearch);

        Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}