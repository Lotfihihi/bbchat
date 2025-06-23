package fr.lotfirais.bbchat;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import fr.lotfirais.bbchat.adapter.ChatMessageAdapter; // Import from new package
import fr.lotfirais.bbchat.model.ChatMessage;       // Import from new package
import fr.lotfirais.bbchat.util.AppSettingsManager;  // Import new helper
import fr.lotfirais.bbchat.util.GeminiApiHelper;     // Import new helper

import java.util.ArrayList;

public class MainActivity extends Activity implements GeminiApiHelper.GeminiResponseCallback {

    private static final String TAG = "BB Chat"; // For logging

    private ListView chatListView;
    private EditText messageEditText;
    private Button sendButton;
    private ProgressBar progressBar;
    private ChatMessageAdapter chatAdapter;
    private ArrayList<ChatMessage> messages;

    private AppSettingsManager appSettingsManager;
    private GeminiApiHelper geminiApiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_main);

        // Initialize helper classes
        appSettingsManager = new AppSettingsManager(getApplicationContext());
        geminiApiHelper = new GeminiApiHelper();

        // Initialize UI components
        chatListView = (ListView) findViewById(R.id.chatListView);
        messageEditText = (EditText) findViewById(R.id.messageEditText);
        sendButton = (Button) findViewById(R.id.sendButton);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        messages = new ArrayList<>();
        chatAdapter = new ChatMessageAdapter(this, messages);
        chatListView.setAdapter(chatAdapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        Button settingsButton = (Button) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        checkAndSetChatStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndSetChatStatus();
    }

    private void checkAndSetChatStatus() {
        String savedApiKey = appSettingsManager.getGeminiApiKey();
        String savedModel = appSettingsManager.getGeminiModel();

        if (savedApiKey.isEmpty()) {
            Toast.makeText(this, "Warning: Gemini API Key not configured. Chat is disabled.", Toast.LENGTH_LONG).show();
            sendButton.setEnabled(false);
        } else {
            sendButton.setEnabled(true);
        }
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        String apiKey = appSettingsManager.getGeminiApiKey();
        String selectedModel = appSettingsManager.getGeminiModel();
        boolean googleSearchEnabled = appSettingsManager.isGoogleSearchEnabled(); // Get Google Search state

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Please configure your Gemini API key in settings first.", Toast.LENGTH_LONG).show();
            return;
        }

        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please type a message.", Toast.LENGTH_SHORT).show();
            return;
        }

        addMessage(new ChatMessage(messageText, ChatMessage.Type.USER));
        messageEditText.setText("");

        setLoadingState(true);

        // Delegate API call to GeminiApiHelper
        geminiApiHelper.generateContent(apiKey, selectedModel, googleSearchEnabled, messages, this);
    }

    private void addMessage(final ChatMessage message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messages.add(message);
                chatAdapter.notifyDataSetChanged();
                chatListView.setSelection(chatAdapter.getCount() - 1);
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        sendButton.setEnabled(!isLoading);
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    // --- Implement GeminiApiHelper.GeminiResponseCallback ---
    @Override
    public void onSuccess(final String response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addMessage(new ChatMessage(response, ChatMessage.Type.BOT));
                setLoadingState(false);
            }
        });
    }

    @Override
    public void onError(final String errorMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addMessage(new ChatMessage("Error: " + errorMessage, ChatMessage.Type.BOT));
                Toast.makeText(MainActivity.this, "API Error: " + errorMessage, Toast.LENGTH_LONG).show();
                setLoadingState(false);
            }
        });
    }
}