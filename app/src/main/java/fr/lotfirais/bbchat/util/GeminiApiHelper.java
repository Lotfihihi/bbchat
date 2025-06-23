// app/java/fr/lotfirais/berryai/util/GeminiApiHelper.java
package fr.lotfirais.bbchat.util;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import fr.lotfirais.bbchat.model.ChatMessage; // Import ChatMessage from new package

public class GeminiApiHelper {

    private static final String TAG = "GeminiApiHelper";

    // Callback interface
    public interface GeminiResponseCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public void generateContent(String apiKey, String modelName, boolean googleSearchEnabled,
                                List<ChatMessage> conversationHistory, GeminiResponseCallback callback) {
        new CallGeminiApiTask(apiKey, modelName, googleSearchEnabled, conversationHistory, callback).execute();
    }

    private static class CallGeminiApiTask extends AsyncTask<Void, Void, String> {

        private final String apiKey;
        private final String modelName;
        private final boolean googleSearchEnabled;
        private final List<ChatMessage> conversationHistory;
        private final GeminiResponseCallback callback;
        private String displayErrorMessage = null;

        public CallGeminiApiTask(String apiKey, String modelName, boolean googleSearchEnabled,
                                 List<ChatMessage> conversationHistory, GeminiResponseCallback callback) {
            this.apiKey = apiKey;
            this.modelName = modelName;
            this.googleSearchEnabled = googleSearchEnabled;
            this.conversationHistory = conversationHistory;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection urlConnection = null;

            try {
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                urlConnection.setDoOutput(true);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);

                JSONObject requestBodyJson = new JSONObject();
                JSONArray contentsArray = new JSONArray();

                for (ChatMessage msg : conversationHistory) {
                    JSONObject contentItem = new JSONObject();
                    contentItem.put("role", msg.getType() == ChatMessage.Type.USER ? "user" : "model");

                    JSONObject part = new JSONObject();
                    part.put("text", msg.getText());
                    JSONArray partsArray = new JSONArray();
                    partsArray.put(part);

                    contentItem.put("parts", partsArray);
                    contentsArray.put(contentItem);
                }
                requestBodyJson.put("contents", contentsArray);

                if (googleSearchEnabled) {
                    Log.d(TAG, "Google Search is enabled. Adding tools to request.");
                    JSONObject toolsObject = new JSONObject();
                    JSONArray toolFamiliesArray = new JSONArray();
                    JSONObject googleSearchTool = new JSONObject();
                    googleSearchTool.put("googleSearch", new JSONObject());
                    toolFamiliesArray.put(googleSearchTool);
                    toolsObject.put("toolFamilies", toolFamiliesArray);
                    requestBodyJson.put("tools", toolsObject);
                } else {
                    Log.d(TAG, "Google Search is disabled. Not adding tools to request.");
                }

                String requestBodyString = requestBodyJson.toString(2);
                Log.d(TAG, "Gemini API Request URL: " + url.toString());
                Log.d(TAG, "Gemini API Request Body: " + requestBodyString);

                OutputStream os = urlConnection.getOutputStream();
                os.write(requestBodyString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = urlConnection.getResponseCode();
                Log.d(TAG, "Gemini API Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                    }
                    in.close();
                    String rawResponse = sb.toString();
                    Log.d(TAG, "Gemini API Raw Response: " + rawResponse);

                    JSONObject jsonResponse = new JSONObject(rawResponse);
                    JSONArray candidates = jsonResponse.optJSONArray("candidates");

                    if (candidates != null && candidates.length() > 0) {
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject contentPart = firstCandidate.optJSONObject("content");
                        if (contentPart != null) {
                            JSONArray partsArray = contentPart.optJSONArray("parts");
                            if (partsArray != null && partsArray.length() > 0) {
                                StringBuilder fullResponse = new StringBuilder();
                                for (int i = 0; i < partsArray.length(); i++) {
                                    JSONObject part = partsArray.getJSONObject(i);
                                    if (part.has("text")) {
                                        fullResponse.append(part.getString("text"));
                                    } else if (part.has("functionCall")) {
                                        Log.d(TAG, "Model requested a functionCall: " + part.getJSONObject("functionCall").toString());
                                        fullResponse.append("\n(AI asked to use a tool: ").append(part.getJSONObject("functionCall").optString("name", "unknown tool")).append(")");
                                    } else if (part.has("toolCode")) {
                                        Log.d(TAG, "Model generated toolCode: " + part.getJSONObject("toolCode").optString("code"));
                                        fullResponse.append("\n(AI generated tool code)");
                                    }
                                }
                                String geminiResponse = fullResponse.toString().trim();
                                if (geminiResponse.isEmpty()) {
                                    return "No direct text response from AI. Check logs for tool calls or safety issues.";
                                }
                                return geminiResponse;
                            }
                        }
                    } else {
                        JSONObject promptFeedback = jsonResponse.optJSONObject("promptFeedback");
                        if (promptFeedback != null) {
                            JSONArray safetyRatings = promptFeedback.optJSONArray("safetyRatings");
                            if (safetyRatings != null && safetyRatings.length() > 0) {
                                StringBuilder safetyMsg = new StringBuilder("Response blocked (safety reasons):\n");
                                for (int i = 0; i < safetyRatings.length(); i++) {
                                    JSONObject rating = safetyRatings.getJSONObject(i);
                                    safetyMsg.append("- Category: ").append(rating.optString("category", "Unknown")).append(", Probability: ").append(rating.optString("probability", "Unknown")).append("\n");
                                }
                                displayErrorMessage = safetyMsg.toString();
                                Log.e(TAG, "Prompt Feedback: " + displayErrorMessage);
                            } else {
                                displayErrorMessage = "No AI response (empty content or unspecified safety filters).";
                                Log.e(TAG, "Prompt Feedback: " + displayErrorMessage);
                            }
                        } else {
                            displayErrorMessage = "No valid AI response. Please rephrase your question.";
                            Log.e(TAG, "No candidates and no prompt feedback.");
                        }
                    }

                } else {
                    BufferedReader errorReader = null;
                    try {
                        if (urlConnection.getErrorStream() != null) {
                            errorReader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream(), "UTF-8"));
                            StringBuilder errorSb = new StringBuilder();
                            String errorLine;
                            while ((errorLine = errorReader.readLine()) != null) {
                                errorSb.append(errorLine);
                            }
                            errorReader.close();
                            String rawErrorResponse = errorSb.toString();
                            Log.e(TAG, "Gemini API Error Response (HTTP " + responseCode + "): " + rawErrorResponse);

                            try {
                                JSONObject errorJson = new JSONObject(rawErrorResponse);
                                JSONObject errorDetails = errorJson.optJSONObject("error");
                                if (errorDetails != null) {
                                    displayErrorMessage = errorDetails.optString("message", "Unexpected API error.");
                                    displayErrorMessage = "Error (" + responseCode + ") : " + displayErrorMessage;
                                } else {
                                    displayErrorMessage = "HTTP Error : " + responseCode + " - " + rawErrorResponse;
                                }
                            } catch (JSONException e) {
                                displayErrorMessage = "HTTP Error : " + responseCode + " - " + rawErrorResponse;
                                Log.e(TAG, "Error parsing error response JSON: " + rawErrorResponse, e);
                            }

                        } else {
                            displayErrorMessage = "HTTP Error : " + responseCode + " (no details available)";
                            Log.e(TAG, "HTTP Error with no error stream: " + responseCode);
                        }
                    } catch (Exception ex) {
                        displayErrorMessage = "Error reading HTTP error details: " + ex.getMessage();
                        Log.e(TAG, "Error reading HTTP error stream: ", ex);
                    }
                }

            } catch (JSONException e) {
                displayErrorMessage = "Error building chat request or parsing response: " + e.getMessage();
                Log.e(TAG, "JSON error: ", e);
            } catch (Exception e) {
                displayErrorMessage = "Unexpected network or internal error: " + e.getMessage();
                Log.e(TAG, "General error: ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return null; // Always return null on error to signal onPostExecute to use errorMessage
        }

        @Override
        protected void onPostExecute(String result) {
            if (callback != null) {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    // Provide a default error message if none was set
                    if (displayErrorMessage == null || displayErrorMessage.isEmpty()) {
                        displayErrorMessage = "An unknown error occurred during API communication.";
                    }
                    callback.onError(displayErrorMessage);
                }
            }
        }
    }
}