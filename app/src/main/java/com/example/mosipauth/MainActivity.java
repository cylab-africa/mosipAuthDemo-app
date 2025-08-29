package com.example.mosipauth;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.example.mosipauth.dataObjects.CaptureRequestDeviceDetailDto;
import com.example.mosipauth.dataObjects.CaptureRequestDto;
import com.example.mosipauth.dataObjects.DiscoverRequestDto;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_DISCOVER = 1;
    private static final int REQUEST_INFO = 2;
    private static final int REQUEST_CAPTURE = 3;
    private static final int REQUEST_CAPTURE_ALL_DISCOVER = 4;
    private static final int REQUEST_CAPTURE_ALL_CAPTURE = 5;


    private String transactionId;

    MaterialButton btnDiscover, btnInfo, btnCapture, btnShare, btnCaptureAll;
    MaterialTextView textBox;
    EditText uinInput;
    String appID = null;
    String serialNo = null;

    // Flag to track if we're in capture-all mode
    private boolean isCaptureAllMode = false;
    private String captureAllResponse = "";

    // API endpoint and executor for network calls
    private static final String API_ENDPOINT = "https://middleware.mosipcmuafrica.me/api/v2/auth/biometric";
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize executor for network calls
        executor = Executors.newSingleThreadExecutor();

        btnDiscover = findViewById(R.id.discover);
        btnInfo = findViewById(R.id.info);
        btnCapture = findViewById(R.id.capture);
        btnShare = findViewById(R.id.share);
        btnCaptureAll = findViewById(R.id.capture_all);
        textBox = findViewById(R.id.textbox);
        uinInput = findViewById(R.id.uin_input);
        textBox.setMovementMethod(new ScrollingMovementMethod());

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textBox.setText("");
                discover();
            }
        });

        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textBox.setText("");
                info();
            }
        });

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textBox.setText("");
                capture();
            }
        });

        btnCaptureAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCaptureAll();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String data = textBox.getText().toString();
                    if (!data.isEmpty()) {
                        String path = MainActivity.this.getFilesDir().getAbsolutePath();
                        File file = new File(path);
                        File txtFile = new File(file, "response.txt");

                        FileOutputStream fOut = new FileOutputStream(txtFile);
                        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                        myOutWriter.append(data);
                        myOutWriter.close();
                        fOut.flush();
                        fOut.close();

                        Uri uri = FileProvider.getUriForFile(MainActivity.this, "com.nprime.fingerauthdemo.fileprovider", txtFile);
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("plain/*");
                        share.putExtra(Intent.EXTRA_STREAM, uri);
                        startActivity(Intent.createChooser(share, "Share file"));
                    }else{
                        Toast.makeText(MainActivity.this, "perform discover/info/capture to get response", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void startCaptureAll() {
        // Validate UIN input
        String uin = uinInput.getText().toString().trim();
//        String uin = "1212232343";
        if (uin.isEmpty()) {
            Toast.makeText(this, "Please enter a UIN", Toast.LENGTH_SHORT).show();
            uinInput.requestFocus();
            return;
        }

        if (uin.length() < 10) {
            Toast.makeText(this, "UIN should be at least 10 digits", Toast.LENGTH_SHORT).show();
            uinInput.requestFocus();
            return;
        }

        // Reset previous state
        appID = null;
        serialNo = null;
        isCaptureAllMode = true;
        captureAllResponse = "";

        textBox.setText("Starting Capture All process...\n");
        textBox.append("Using UIN: " + uin + "\n\n");
        textBox.append("Step 1: Running Discover...\n");

        // Disable all buttons during capture-all process
        setButtonsEnabled(false);

        // Start with discover
        discoverForCaptureAll();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnDiscover.setEnabled(enabled);
        btnInfo.setEnabled(enabled && appID != null);
        btnCapture.setEnabled(enabled && serialNo != null);
        btnCaptureAll.setEnabled(enabled);
        btnShare.setEnabled(enabled);
        uinInput.setEnabled(enabled); // Disable UIN input during process
    }

    private boolean isFilePermissionGranted() {
        boolean permissionGranted = false;
        if(PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission
                (MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},102);
        }else {
            permissionGranted = true;
        }
        return permissionGranted;
    }

    private void initViews() {
        if (!isCaptureAllMode) {
            btnInfo.setEnabled(appID != null);
            btnCapture.setEnabled(serialNo != null);
        }
    }

    @SuppressLint("NewApi")
    private void discover(){
        Intent intent = new Intent();
        intent.setAction("io.sbi.device");

        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
        activities.sort(new ResolveInfo.DisplayNameComparator(packageManager));
        final boolean isIntentSafe = activities.size() > 0;

        if(isIntentSafe) {
            DiscoverRequestDto discoverRequestDto = new DiscoverRequestDto();
            discoverRequestDto.type = "finger";
            try {
                intent.putExtra("input", new ObjectMapper().writeValueAsBytes(discoverRequestDto));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            startActivityForResult(intent, REQUEST_DISCOVER);
        }else {
            Toast.makeText(MainActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
            if (isCaptureAllMode) {
                handleCaptureAllError("Discover failed: Supported apps not found");
            }
        }
    }

    @SuppressLint("NewApi")
    private void discoverForCaptureAll(){
        Intent intent = new Intent();
        intent.setAction("io.sbi.device");

        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
        activities.sort(new ResolveInfo.DisplayNameComparator(packageManager));
        final boolean isIntentSafe = activities.size() > 0;

        if(isIntentSafe) {
            DiscoverRequestDto discoverRequestDto = new DiscoverRequestDto();
            discoverRequestDto.type = "finger";
            try {
                intent.putExtra("input", new ObjectMapper().writeValueAsBytes(discoverRequestDto));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            startActivityForResult(intent, REQUEST_CAPTURE_ALL_DISCOVER);
        }else {
            handleCaptureAllError("Discover failed: Supported apps not found");
        }
    }

    private void info(){
        Intent intent = new Intent();
        intent.setAction(appID + ".Info");

        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
        Collections.sort(activities,new ResolveInfo.DisplayNameComparator(packageManager));
        final boolean isIntentSafe = activities.size() > 0;

        if(isIntentSafe) {
            //intent.putExtra("input", captureInput);
            startActivityForResult(intent, REQUEST_INFO);
        }else {
            Toast.makeText(MainActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void capture(){
        performCapture(REQUEST_CAPTURE);
    }

    private void captureForCaptureAll(){
        textBox.append("Step 2: Running Info to get device details...\n");

        Intent intent = new Intent();
        intent.setAction(appID + ".Info");

        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
        Collections.sort(activities,new ResolveInfo.DisplayNameComparator(packageManager));
        final boolean isIntentSafe = activities.size() > 0;

        if(isIntentSafe) {
            startActivityForResult(intent, REQUEST_INFO);
        }else {
            handleCaptureAllError("Info failed: Supported apps not found");
        }
    }

    private void performCapture(int requestCode){
        Intent intent = new Intent();
        intent.setAction(appID + ".Capture");

        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
        Collections.sort(activities,new ResolveInfo.DisplayNameComparator(packageManager));
        final boolean isIntentSafe = activities.size() > 0;
        if(isIntentSafe) {
            if(null == serialNo){
                String errorMsg = "Perform info request";
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                if (isCaptureAllMode) {
                    handleCaptureAllError("Capture failed: " + errorMsg);
                }
                return;
            }
            CaptureRequestDto captureRequestDto = new CaptureRequestDto();

            String currentTime = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentTime = Instant.now().toString();

            }

            transactionId =generateRandomTransactionId();
            captureRequestDto.captureTime = currentTime;
            captureRequestDto.env = "Staging";
            captureRequestDto.purpose = "Auth";
            captureRequestDto.specVersion = "0.9.5";
            captureRequestDto.timeout = 10000;
            captureRequestDto.domainUri = "https://api-internal.mosip.mosipcmuafrica.me";
            captureRequestDto.transactionId = transactionId;
            CaptureRequestDeviceDetailDto bio = new CaptureRequestDeviceDetailDto();
            bio.bioSubType = new String[]{"UNKNOWN"};
            bio.deviceId = serialNo;
            bio.deviceSubId = "0";
            bio.type = "Finger";
            bio.previousHash = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";
            bio.count = 1;
            bio.requestedScore = 40;
            List<CaptureRequestDeviceDetailDto> bioList = new ArrayList<>();
            bioList.add(bio);
            captureRequestDto.mosipBioRequest = bioList;
            captureRequestDto.customOpts = null;

//            String payload;
//            try {
//                 payload = getSampleBiometricDataSync();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }

            // Display the payload on main thread
            runOnUiThread(() -> {
                textBox.append("\n=== PAYLOAD TO BE SENT TO CAPTURE ===\n");
                textBox.append("Payload:\n" + captureRequestDto + "\n");
                textBox.append("TransactionId:\n" + transactionId + "\n");
                textBox.append("\nSending request...\n");
            });

            try {
                intent.putExtra("input", new ObjectMapper().writeValueAsBytes(captureRequestDto));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            startActivityForResult(intent, requestCode);
        }else {
            String errorMsg = "Supported apps not found";
            Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            if (isCaptureAllMode) {
                handleCaptureAllError("Capture failed: " + errorMsg);
            }
        }
    }


    private void getSampleBiometricData() {
        executor.execute(() -> {
            try {
                // Make request to sample bio endpoint
                URL url = new URL("https://middleware.mosipcmuafrica.me/api/v1/sample/bio");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                // Get response
                int responseCode = connection.getResponseCode();
                StringBuilder response = new StringBuilder();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream(),
                        StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                // Update UI on main thread
                runOnUiThread(() -> {
                    textBox.append("\n=== SAMPLE BIO ENDPOINT RESPONSE ===\n");
                    textBox.append("Endpoint: https://middleware.mosipcmuafrica.me/api/v1/sample/bio\n");
                    textBox.append("Response Code: " + responseCode + "\n");
                    textBox.append("Response Body:\n" + response.toString() + "\n");

                    if (responseCode >= 200 && responseCode < 300) {
                        // Success - use this response as our biometric data
                        handleCaptureAllSuccess(response.toString());
                    } else {
                        handleCaptureAllError("Sample bio endpoint failed with code: " + responseCode);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    textBox.append("\n=== SAMPLE BIO ENDPOINT ERROR ===\n");
                    textBox.append("Error: " + e.getMessage() + "\n");
                    handleCaptureAllError("Failed to get sample bio data: " + e.getMessage());
                });
            }
        });
    }

    private void handleCaptureAllError(String errorMessage) {
        isCaptureAllMode = false;
        setButtonsEnabled(true);
        textBox.append("\nERROR: " + errorMessage + "\n");
        textBox.append("Capture All process failed.\n");
        Toast.makeText(this, "Capture All failed: " + errorMessage, Toast.LENGTH_LONG).show();
    }

    private void handleCaptureAllSuccess(String finalResponse) {
        isCaptureAllMode = false;
        setButtonsEnabled(true);
        textBox.append("\n=== CAPTURE ALL COMPLETED SUCCESSFULLY ===\n");
        textBox.append("Final biometric data captured:\n");
        textBox.append(finalResponse);
        textBox.append("\n\nStep 4: Sending data to endpoint...\n");

        Toast.makeText(this, "Capture All completed! Sending to server...", Toast.LENGTH_SHORT).show();

        // Send biometric data to endpoint
        sendBiometricDataToEndpoint(finalResponse);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(REQUEST_DISCOVER == requestCode){
            handleDiscoverResponse(resultCode, data, false);
        } else if(REQUEST_CAPTURE_ALL_DISCOVER == requestCode){
            handleDiscoverResponse(resultCode, data, true);
        } else if(REQUEST_INFO == requestCode){
            handleInfoResponse(resultCode, data);
        } else if(REQUEST_CAPTURE == requestCode){
            handleCaptureResponse(resultCode, data, false);
        } else if(REQUEST_CAPTURE_ALL_CAPTURE == requestCode){
            handleCaptureResponse(resultCode, data, true);
        }
    }

    private void handleDiscoverResponse(int resultCode, Intent data, boolean isCaptureAllFlow) {
        if(Activity.RESULT_OK == resultCode){
            try{
                if(null != data) {
                    if (data.hasExtra("response")) {
                        byte[] responseBytes = data.getByteArrayExtra("response");
                        String response = new String(responseBytes);
                        JSONArray respJsonArray = new JSONArray(response);
                        JSONObject respJsonObject = respJsonArray.getJSONObject(0);
                        JSONObject errorObject = respJsonObject.getJSONObject("error");
                        if (0 == errorObject.getInt("errorCode")) {
                            appID = respJsonObject.getString("callbackId");

                            if (isCaptureAllFlow) {
                                textBox.append("✓ Discover successful! App ID: " + appID + "\n");
                                captureForCaptureAll();
                            } else {
                                textBox.setText("Discover Response :\n" + response);
                                Toast.makeText(MainActivity.this, appID, Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            String errorMsg = errorObject.toString();
                            if (isCaptureAllFlow) {
                                handleCaptureAllError("Discover error: " + errorMsg);
                            } else {
                                textBox.setText(errorMsg);
                            }
                        }
                    }else {
                        String errorMsg = "Response not found";
                        if (isCaptureAllFlow) {
                            handleCaptureAllError("Discover error: " + errorMsg);
                        } else {
                            textBox.setText(errorMsg);
                        }
                    }
                }else {
                    String errorMsg = "Response not found";
                    if (isCaptureAllFlow) {
                        handleCaptureAllError("Discover error: " + errorMsg);
                    } else {
                        textBox.setText(errorMsg);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                if (isCaptureAllFlow) {
                    handleCaptureAllError("Discover exception: " + e.getMessage());
                }
            }
        } else {
            if (isCaptureAllFlow) {
                handleCaptureAllError("Discover was cancelled or failed");
            }
        }

        if (!isCaptureAllFlow) {
            initViews();
        }
    }

    private void handleInfoResponse(int resultCode, Intent data) {
        if(Activity.RESULT_OK == resultCode){
            try {
                if(null != data) {
                    if (data.hasExtra("response")) {
                        byte[] responseBytes = data.getByteArrayExtra("response");
                        String response = new String(responseBytes);
                        JSONArray respJsonArray = new JSONArray(response);
                        JSONObject errorObject = respJsonArray.getJSONObject(0).getJSONObject("error");
                        if (0 == errorObject.getInt("errorCode")) {
                            String deviceInfo = (respJsonArray.getJSONObject(0)).getString("deviceInfo");
                            byte[] payload = getPayloadBuffer(deviceInfo);

                            JSONObject infoObject = new JSONObject(new String(payload));
                            String digitalId = infoObject.getString("digitalId");
                            byte[] digitalIdPayload = getPayloadBuffer(digitalId);

                            JSONObject digitalIdObj = new JSONObject(new String(digitalIdPayload));
                            serialNo = digitalIdObj.getString("serialNo");

                            if (isCaptureAllMode) {
                                textBox.append("✓ Info successful! Serial No: " + serialNo + "\n");
                                textBox.append("Step 3: Running Capture...\n");
                                performCapture(REQUEST_CAPTURE_ALL_CAPTURE);
                            } else {
                                textBox.setText("Info Response :\n" + response);
                            }
                        }else{
                            if (isCaptureAllMode) {
                                handleCaptureAllError("Info error: " + response);
                            } else {
                                textBox.setText(response);
                            }
                        }
                    }else {
                        String errorMsg = "Info response not found";
                        if (isCaptureAllMode) {
                            handleCaptureAllError(errorMsg);
                        } else {
                            textBox.setText(errorMsg);
                        }
                    }
                }else {
                    String errorMsg = "Info response not found";
                    if (isCaptureAllMode) {
                        handleCaptureAllError(errorMsg);
                    } else {
                        textBox.setText(errorMsg);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                if (isCaptureAllMode) {
                    handleCaptureAllError("Info exception: " + e.getMessage());
                }
            }
        } else {
            if (isCaptureAllMode) {
                handleCaptureAllError("Info was cancelled or failed");
            }
        }

        if (!isCaptureAllMode) {
            initViews();
        }
    }

    private void handleCaptureResponse(int resultCode, Intent data, boolean isCaptureAllFlow) {
        if(Activity.RESULT_OK == resultCode){
            try {
                if(null != data) {
                    Uri uri = data.getParcelableExtra("response");
                    if (null != uri) {
                        InputStream respData = getContentResolver().openInputStream(uri);
                        BufferedReader r = new BufferedReader(new InputStreamReader(respData));
                        StringBuilder total = new StringBuilder();
                        for (String line; (line = r.readLine()) != null; ) {
                            total.append(line).append('\n');
                        }
                        String response = total.toString();
                        JSONObject resposeObject = new JSONObject(response);
                        if(resposeObject.has("biometrics")) {
                            JSONArray biometricsArray = resposeObject.getJSONArray("biometrics");
                            JSONObject errObject = (biometricsArray.getJSONObject(0)).getJSONObject("error");
                            if (0 == errObject.getInt("errorCode")) {
                                if (isCaptureAllFlow) {
                                    handleCaptureAllSuccess(response);
                                } else {
                                    textBox.setText("Capture response :\n" + response);
                                }
                            } else {
                                if (isCaptureAllFlow) {
                                    handleCaptureAllError("Capture error: " + response);
                                } else {
                                    textBox.setText(response);
                                }
                            }
                        }else{
                            if (isCaptureAllFlow) {
                                handleCaptureAllError("Capture error: " + resposeObject.toString());
                            } else {
                                textBox.setText(resposeObject.toString());
                            }
                        }
                    }else {
                        String errorMsg = "Capture response not found";
                        if (isCaptureAllFlow) {
                            handleCaptureAllError(errorMsg);
                        } else {
                            textBox.setText(errorMsg);
                        }
                    }
                }else {
                    String errorMsg = "Capture response not found";
                    if (isCaptureAllFlow) {
                        handleCaptureAllError(errorMsg);
                    } else {
                        textBox.setText(errorMsg);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                if (isCaptureAllFlow) {
                    handleCaptureAllError("Capture exception: " + e.getMessage());
                }
            }
        } else {
            if (isCaptureAllFlow) {
                handleCaptureAllError("Capture was cancelled or failed");
            }
        }
    }

    @SuppressLint("NewApi")
    public byte[] getPayloadBuffer(String responseToken) {
        byte[] payLoad = null;
        try {
            String[] responseTokenArray = responseToken.split("\\.");
            payLoad = java.util.Base64.getUrlDecoder().decode(responseTokenArray[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return payLoad;
    }

    public void nprimeWebsite(View view) {
        Intent updateIntent = new Intent(Intent.ACTION_VIEW);
        updateIntent.setData(Uri.parse("https://www.nprime.in/"));
        startActivity(updateIntent);
    }

    private void sendBiometricDataToEndpoint(String biometricData) {
        executor.execute(() -> {
            try {
                // Get UIN from input field
                String uin = "";
                runOnUiThread(() -> {
                    // We need to get UIN on main thread since we're accessing UI
                });

                // Get UIN from input (we'll pass it as parameter instead)
                String uinFromInput = uinInput.getText().toString().trim();

                // Parse the biometric response to extract the data array
                JSONObject biometricResponse = new JSONObject(biometricData);
                JSONArray biometricsArray = biometricResponse.getJSONArray("biometrics");

                // Create the payload according to your API specification
                JSONObject payload = new JSONObject();
                payload.put("uid", uinFromInput); // Use dynamic UIN
                payload.put("transactionId", transactionId);

                // Use the captured biometric data
                payload.put("data", biometricsArray);

                // Add metadata
                JSONObject metadata = new JSONObject();
                metadata.put("callback", "https://ceb-api.mosipcmuafrica.me/programs/mosip/callback");
                metadata.put("userId", "68adb594694b4908f76b04a0");
                metadata.put("programId", "68adb3bdb21e6156c0935f4a");
                metadata.put("authenticator", "6888955a64bdcac82f6c1e63");
                payload.put("metadata", metadata);

                // Format the payload for display
                String formattedPayload = payload.toString(2); // Pretty print with 2-space indentation

                // Display the payload on main thread
                runOnUiThread(() -> {
                    textBox.append("\n=== PAYLOAD TO BE SENT ===\n");
                    textBox.append("Endpoint: " + API_ENDPOINT + "\n");
                    textBox.append("Method: POST\n");
                    textBox.append("Content-Type: application/json\n\n");
                    textBox.append("Payload:\n" + formattedPayload + "\n");
                    textBox.append("\nSending request...\n");
                });

                // Make HTTP request
                URL url = new URL(API_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("secrets","iZ5oiAX4t2eklaidlNxRanpWfAR5x8BmHNprFVAp4mqVK1jnhApXxwRSevrtgIdx2EmV46qDLVbX4zENxz2Za3G6VpN3ZMztiD5VeNCbf1PAVhRWGXqOkvpOfiWJXPWROSJjNE0wVnxWahETchIylulKuFi0cU5u_-MRKDVK6lOLbGNVRP0frnDgUZBKm_MwU-x3EjUkAwaIQJZxMDOTfmOFnlfsZroTpezigC8b1S3yXakHSj4yYGAY9gtWtAwumAb52PFPdsHEzS0D_Ie1Moknr4B3jIisZTkvw5DL9sdEYl0yOwqzj5t9uElaiTn7Mu0G9DRhDI19yAnU8Ebw3aICBN0yJMRZjyZYmRwquop3zpyKmIPTTadBfdrAExxbNG7nei9shnitMrXCn0pIw_I3nwYMEBGT12ckK8TG9H2pu2LsmvrcnMuJkkeHombNzeebnqPEKZZjUt2oqMFAOlu1lmB7FsbUcjvvFHVDh6BZyEouu7WcC8G8PEzqha9v");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                // Send payload
                String jsonPayload = payload.toString();
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Get response
                int responseCode = connection.getResponseCode();
                StringBuilder response = new StringBuilder();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream(),
                        StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                // Update UI on main thread with response
                runOnUiThread(() -> {
                    textBox.append("\n=== API RESPONSE ===\n");
                    textBox.append("Response Code: " + responseCode + "\n");
                    textBox.append("Response Body:\n" + response.toString() + "\n");

                    // Parse and display key information from response
                    try {
                        JSONObject responseJson = new JSONObject(response.toString());
                        if (responseJson.has("mosip")) {
                            JSONObject mosipData = responseJson.getJSONObject("mosip");
                            boolean authStatus = mosipData.optBoolean("authStatus", false);

                            textBox.append("\n=== AUTHENTICATION RESULT ===\n");
                            textBox.append("Authentication Status: " + (authStatus ? "SUCCESS" : "FAILED") + "\n");

                            if (!authStatus && mosipData.has("errors")) {
                                JSONArray errors = mosipData.getJSONArray("errors");
                                textBox.append("Errors:\n");
                                for (int i = 0; i < errors.length(); i++) {
                                    JSONObject error = errors.getJSONObject(i);
                                    textBox.append("- " + error.optString("errorCode", "") + ": " +
                                            error.optString("errorMessage", "") + "\n");
                                }
                            }
                        }
                    } catch (Exception e) {
                        // If parsing fails, just show the raw response
                        textBox.append("Note: Could not parse response for detailed info\n");
                    }

                    if (responseCode >= 200 && responseCode < 300) {
                        Toast.makeText(MainActivity.this, "Biometric data sent successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "API call failed with code: " + responseCode, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    textBox.append("\n=== API ERROR ===\n");
                    textBox.append("Error: " + e.getMessage() + "\n");
                    Toast.makeText(MainActivity.this, "Failed to send biometric data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }


    private String getSampleBiometricDataSync() throws IOException {
        try {
            // Make request to sample bio endpoint
            URL url = new URL("https://middleware.mosipcmuafrica.me/api/v1/sample/bio");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Get response
            int responseCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            final String responseBody = response.toString();
            final int finalResponseCode = responseCode;

            // Update UI on main thread
            runOnUiThread(() -> {
                textBox.append("\n=== SAMPLE BIO ENDPOINT RESPONSE ===\n");
                textBox.append("Endpoint: https://middleware.mosipcmuafrica.me/api/v1/sample/bio\n");
                textBox.append("Response Code: " + finalResponseCode + "\n");
                textBox.append("Response Body:\n" + responseBody + "\n");
            });

            if (responseCode >= 200 && responseCode < 300) {
                return responseBody;
            } else {
                runOnUiThread(() -> {
                    handleCaptureAllError("Sample bio endpoint failed with code: " + responseCode);
                });
                throw new IOException("HTTP " + responseCode + ": " + responseBody);
            }

        } catch (IOException e) {
            runOnUiThread(() -> {
                textBox.append("\n=== SAMPLE BIO ENDPOINT ERROR ===\n");
                textBox.append("Error: " + e.getMessage() + "\n");
                handleCaptureAllError("Failed to get sample bio data: " + e.getMessage());
            });
            throw e;
        }
    }


    public static String generateRandomTransactionId() {
        Random random = new Random();
        // Generate random number between 1000000000 and 9999999999 (10 digits)
        long transactionId = 1000000000L + (long)(random.nextDouble() * 9000000000L);
        return String.valueOf(transactionId);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}