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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.example.mosipauth.dataObjects.CaptureRequestDeviceDetailDto;
import com.example.mosipauth.dataObjects.CaptureRequestDto;
import com.example.mosipauth.dataObjects.DiscoverRequestDto;

// QR Scanner imports
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

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
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private String transactionId;

    MaterialButton btnDiscover, btnInfo, btnCapture, btnShare, btnCaptureAll;
    MaterialButton btnQRScanner, btnToggleFlash, btnCloseScanner;
    MaterialTextView textBox;
    EditText uinInput;
    LinearLayout qrScannerSection;
    BarcodeView barcodeScanner;
    
    // New result display components
    MaterialCardView resultCard;
    LinearLayout loadingLayout, successLayout, errorLayout;
    TextView errorMessage;

    String appID = null;
    String serialNo = null;

    // QR Scanner related variables
    private boolean isQRScannerOpen = false;
    private boolean isFlashOn = false;
    private String uinFetched;

    // Flag to track if we're in capture-all mode
    private boolean isCaptureAllMode = false;
    private String captureAllResponse = "";

    // API endpoint and executor for network calls
    private static final String API_ENDPOINT = "https://middleware.mosipcmuafrica.me/api/v2/auth/biometric";
    private static final String ORDER_API_ENDPOINT = "https://mojashop-api.mosipcmuafrica.me/api/business-service/orders/";
    private ExecutorService executor;

    // Order details related variables
    private JSONObject orderDetails = null;
    private String authToken = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize executor for network calls
        executor = Executors.newSingleThreadExecutor();

        // Initialize existing views
//        btnDiscover = findViewById(R.id.discover);
//        btnInfo = findViewById(R.id.info);
//        btnCapture = findViewById(R.id.capture);
//        btnShare = findViewById(R.id.share);
        btnCaptureAll = findViewById(R.id.capture_all);
//        textBox = findViewById(R.id.textbox);
        uinInput = findViewById(R.id.uin_input);
//        textBox.setMovementMethod(new ScrollingMovementMethod());

        // Initialize QR scanner views
        btnQRScanner = findViewById(R.id.qr_scanner_button);
        btnToggleFlash = findViewById(R.id.toggle_flash);
        btnCloseScanner = findViewById(R.id.close_scanner);
        qrScannerSection = findViewById(R.id.qr_scanner_section);
        barcodeScanner = findViewById(R.id.barcode_scanner);
        
        // Initialize new result display components
        resultCard = findViewById(R.id.result_card);
        loadingLayout = findViewById(R.id.loading_layout);
        successLayout = findViewById(R.id.success_layout);
        errorLayout = findViewById(R.id.error_layout);
        errorMessage = findViewById(R.id.error_message);

        // Setup QR scanner
        setupQRScanner();

        // Existing button click listeners
//        btnDiscover.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                textBox.setText("");
//                discover();
//            }
//        });

//        btnInfo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                textBox.setText("");
//                info();
//            }
//        });

//        btnCapture.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                textBox.setText("");
//                capture();
//            }
//        });

        btnCaptureAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCaptureAll();
            }
        });

//        btnShare.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                try {
//                    String data = textBox.getText().toString();
//                    if (!data.isEmpty()) {
//                        String path = MainActivity.this.getFilesDir().getAbsolutePath();
//                        File file = new File(path);
//                        File txtFile = new File(file, "response.txt");
//
//                        FileOutputStream fOut = new FileOutputStream(txtFile);
//                        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
//                        myOutWriter.append(data);
//                        myOutWriter.close();
//                        fOut.flush();
//                        fOut.close();
//
//                        Uri uri = FileProvider.getUriForFile(MainActivity.this, "com.nprime.fingerauthdemo.fileprovider", txtFile);
//                        Intent share = new Intent(Intent.ACTION_SEND);
//                        share.setType("plain/*");
//                        share.putExtra(Intent.EXTRA_STREAM, uri);
//                        startActivity(Intent.createChooser(share, "Share file"));
//                    }else{
//                        Toast.makeText(MainActivity.this, "perform discover/info/capture to get response", Toast.LENGTH_SHORT).show();
//                    }
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        });
    }

    private void setupQRScanner() {
        // QR Scanner button click listener
        btnQRScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkCameraPermission()) {
                    toggleQRScanner();
                } else {
                    requestCameraPermission();
                }
            }
        });

        // Flash toggle button
//        btnToggleFlash.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                toggleFlash();
//            }
//        });

        // Close scanner button
        btnCloseScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeQRScanner();
            }
        });

        // Configure barcode scanner
        CameraSettings settings = new CameraSettings();
        settings.setRequestedCameraId(1); // Use front camera (0 = back, 1 = front)
        barcodeScanner.setCameraSettings(settings);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleQRScanner();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleQRScanner() {
        if (isQRScannerOpen) {
            closeQRScanner();
        } else {
            openQRScanner();
        }
    }

    private void openQRScanner() {
        qrScannerSection.setVisibility(View.VISIBLE);
        isQRScannerOpen = true;

        // Start scanning
        barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                String scannedText = result.getText();
                handleQRScanResult(scannedText);
            }

            @Override
            public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {
                // Optional: Handle possible result points for UI feedback
            }
        });

        barcodeScanner.resume();
        btnQRScanner.setText("Close QR");
    }

    private void closeQRScanner() {
        qrScannerSection.setVisibility(View.GONE);
        isQRScannerOpen = false;
        barcodeScanner.pause();
        btnQRScanner.setText("📱");

        // Turn off flash if it was on
//        if (isFlashOn) {
//            toggleFlash();
//        }
    }

//    private void toggleFlash() {
//        if (barcodeScanner.isFlashOn()) {
//            barcodeScanner.setFlash(false);
//            isFlashOn = false;
//            btnToggleFlash.setText("💡 Flash");
//        } else {
//            barcodeScanner.setFlash(true);
//            isFlashOn = true;
//            btnToggleFlash.setText("🔆 Flash On");
//        }
//    }

    private boolean isValidUIN(String uin) {
        // Basic UIN validation
        if (uin == null || uin.trim().isEmpty()) {
            return false;
        }

        // Remove any non-numeric characters
        String cleanedUIN = uin.replaceAll("[^0-9]", "");

        // Check if it's between 10-15 digits (adjust based on your requirements)
        return cleanedUIN.length() >= 10 && cleanedUIN.length() <= 15;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isQRScannerOpen && barcodeScanner != null) {
            barcodeScanner.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScanner != null) {
            barcodeScanner.pause();
        }
    }

    // Rest of your existing methods remain the same...

    private void startCaptureAll() {
        // Validate UIN input
        String uin = uinInput.getText().toString().trim();
        if (uin.isEmpty()) {
            Toast.makeText(this, "Please enter a UIN or scan a QR code", Toast.LENGTH_SHORT).show();
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

//        textBox.setText("Starting Capture All process...\n");
//        textBox.append("Using UIN: " + uin + "\n\n");
//        textBox.append("Step 1: Running Discover...\n");

        // Show loading state
        showLoadingResult();
        
        // Disable all buttons during capture-all process
        setButtonsEnabled(false);

        // Start with discover
        discoverForCaptureAll();
    }

    private void setButtonsEnabled(boolean enabled) {
//        btnDiscover.setEnabled(enabled);
//        btnInfo.setEnabled(enabled && appID != null);
//        btnCapture.setEnabled(enabled && serialNo != null);
        btnCaptureAll.setEnabled(enabled);
//        btnShare.setEnabled(enabled);
        btnQRScanner.setEnabled(enabled); // Also disable QR scanner during process
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
//            btnInfo.setEnabled(appID != null);
//            btnCapture.setEnabled(serialNo != null);
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
//        textBox.append("Step 2: Running Info to get device details...\n");

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

            // Display the payload on main thread
            runOnUiThread(() -> {
//                textBox.append("\n=== PAYLOAD TO BE SENT TO CAPTURE ===\n");
//                textBox.append("Payload:\n" + captureRequestDto + "\n");
//                textBox.append("TransactionId:\n" + transactionId + "\n");
//                textBox.append("\nSending request...\n");
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
//                    textBox.append("\n=== SAMPLE BIO ENDPOINT RESPONSE ===\n");
//                    textBox.append("Endpoint: https://middleware.mosipcmuafrica.me/api/v1/sample/bio\n");
//                    textBox.append("Response Code: " + responseCode + "\n");
//                    textBox.append("Response Body:\n" + response.toString() + "\n");

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
//                    textBox.append("\n=== SAMPLE BIO ENDPOINT ERROR ===\n");
//                    textBox.append("Error: " + e.getMessage() + "\n");
                    handleCaptureAllError("Failed to get sample bio data: " + e.getMessage());
                });
            }
        });
    }

    private void handleCaptureAllError(String errorMessage) {
        isCaptureAllMode = false;
        setButtonsEnabled(true);
//        textBox.append("\nERROR: " + errorMessage + "\n");
//        textBox.append("Capture All process failed.\n");
        showErrorResult("Authentication failed: " + errorMessage);
        Toast.makeText(this, "Capture All failed: " + errorMessage, Toast.LENGTH_LONG).show();
    }

    private void handleCaptureAllSuccess(String finalResponse) {
        isCaptureAllMode = false;
//        setButtonsEnabled(true); // Don't enable yet, wait for final API response
//        textBox.append("\n=== CAPTURE ALL COMPLETED SUCCESSFULLY ===\n");
//        textBox.append("Final biometric data captured:\n");
//        textBox.append(finalResponse);
//        textBox.append("\n\nStep 4: Sending data to endpoint...\n");

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
//                                textBox.append("✓ Discover successful! App ID: " + appID + "\n");
                                captureForCaptureAll();
                            } else {
//                                textBox.setText("Discover Response :\n" + response);
                                Toast.makeText(MainActivity.this, appID, Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            String errorMsg = errorObject.toString();
                            if (isCaptureAllFlow) {
                                handleCaptureAllError("Discover error: " + errorMsg);
                            } else {
//                                textBox.setText(errorMsg);
                            }
                        }
                    }else {
                        String errorMsg = "Response not found";
                        if (isCaptureAllFlow) {
                            handleCaptureAllError("Discover error: " + errorMsg);
                        } else {
//                            textBox.setText(errorMsg);
                        }
                    }
                }else {
                    String errorMsg = "Response not found";
                    if (isCaptureAllFlow) {
                        handleCaptureAllError("Discover error: " + errorMsg);
                    } else {
//                        textBox.setText(errorMsg);
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
//                                textBox.append("✓ Info successful! Serial No: " + serialNo + "\n");
//                                textBox.append("Step 3: Running Capture...\n");
                                performCapture(REQUEST_CAPTURE_ALL_CAPTURE);
                            } else {
//                                textBox.setText("Info Response :\n" + response);
                            }
                        }else{
                            if (isCaptureAllMode) {
                                handleCaptureAllError("Info error: " + response);
                            } else {
//                                textBox.setText(response);
                            }
                        }
                    }else {
                        String errorMsg = "Info response not found";
                        if (isCaptureAllMode) {
                            handleCaptureAllError(errorMsg);
                        } else {
//                            textBox.setText(errorMsg);
                        }
                    }
                }else {
                    String errorMsg = "Info response not found";
                    if (isCaptureAllMode) {
                        handleCaptureAllError(errorMsg);
                    } else {
//                        textBox.setText(errorMsg);
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
//                                    textBox.setText("Capture response :\n" + response);
                                }
                            } else {
                                if (isCaptureAllFlow) {
                                    handleCaptureAllError("Capture error: " + response);
                                } else {
//                                    textBox.setText(response);
                                }
                            }
                        }else{
                            if (isCaptureAllFlow) {
                                handleCaptureAllError("Capture error: " + resposeObject.toString());
                            } else {
//                                textBox.setText(resposeObject.toString());
                            }
                        }
                    }else {
                        String errorMsg = "Capture response not found";
                        if (isCaptureAllFlow) {
                            handleCaptureAllError(errorMsg);
                        } else {
//                            textBox.setText(errorMsg);
                        }
                    }
                }else {
                    String errorMsg = "Capture response not found";
                    if (isCaptureAllFlow) {
                        handleCaptureAllError(errorMsg);
                    } else {
//                        textBox.setText(errorMsg);
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
                String uinFromInput = uinInput.getText().toString().trim();

                // Parse the biometric response to extract the data array
                JSONObject biometricResponse = new JSONObject(biometricData);
                JSONArray biometricsArray = biometricResponse.getJSONArray("biometrics");

                // Create the payload according to your API specification
                JSONObject payload = new JSONObject();
                payload.put("uid", orderDetails.getString("uin")); // Use dynamic UIN
                payload.put("transactionId", transactionId);

                // Use the captured biometric data
                payload.put("data", biometricsArray);

                // Add metadata
                JSONObject metadata = new JSONObject();
                metadata.put("callback", "https://mojashop-api.mosipcmuafrica.me/api/business-service/webhooks/auth-middleware-callback");
                metadata.put("userId", orderDetails.getString("_id"));
                metadata.put("programId", orderDetails.getString("programId"));
                metadata.put("orderNumber", orderDetails.getString("orderNumber"));
                metadata.put("orderId", orderDetails.getString("_id"));
                metadata.put("authenticator", "Mojashop");
                payload.put("metadata", metadata);

                // Format the payload for display
                String formattedPayload = payload.toString(2); // Pretty print with 2-space indentation

                // Display the payload on main thread
//                runOnUiThread(() -> {
//                    textBox.append("\n=== PAYLOAD TO BE SENT ===\n");
//                    textBox.append("Endpoint: " + API_ENDPOINT + "\n");
//                    textBox.append("Method: POST\n");
//                    textBox.append("Content-Type: application/json\n\n");
//                    textBox.append("Payload:\n" + formattedPayload + "\n");
//                    textBox.append("\nSending request...\n");
//                });

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
//                    textBox.append("\n=== API RESPONSE ===\n");
//                    textBox.append("Response Code: " + responseCode + "\n");
//                    textBox.append("Response Body:\n" + response.toString() + "\n");

                    // Parse and display key information from response
                    try {
                        JSONObject responseJson = new JSONObject(response.toString());
                        if (responseJson.has("mosip")) {
                            JSONObject mosipData = responseJson.getJSONObject("mosip");
                            boolean authStatus = mosipData.optBoolean("authStatus", false);

//                            textBox.append("\n=== AUTHENTICATION RESULT ===\n");
//                            textBox.append("Authentication Status: " + (authStatus ? "SUCCESS" : "FAILED") + "\n");

                            if (authStatus) {
                                showSuccessResult();
                            } else {
                                String errorDetails = "";
                                if (mosipData.has("errors")) {
                                    JSONArray errors = mosipData.getJSONArray("errors");
                                    StringBuilder errorBuilder = new StringBuilder();
                                    for (int i = 0; i < errors.length(); i++) {
                                        JSONObject error = errors.getJSONObject(i);
                                        errorBuilder.append(error.optString("errorCode", "")).append(": ")
                                                .append(error.optString("errorMessage", ""));
                                        if (i < errors.length() - 1) {
                                            errorBuilder.append("\n");
                                        }
                                    }
                                    errorDetails = errorBuilder.toString();
                                }
                                showErrorResult(errorDetails.isEmpty() ? "Authentication failed" : errorDetails);
                            }
                        } else {
                            showErrorResult("Invalid response format");
                        }
                    } catch (Exception e) {
                        // If parsing fails, show error
                        showErrorResult("Failed to parse authentication response");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
//                    textBox.append("\n=== API ERROR ===\n");
//                    textBox.append("Error: " + e.getMessage() + "\n");
                    showErrorResult("Network error: " + e.getMessage());
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
//                textBox.append("\n=== SAMPLE BIO ENDPOINT RESPONSE ===\n");
//                textBox.append("Endpoint: https://middleware.mosipcmuafrica.me/api/v1/sample/bio\n");
//                textBox.append("Response Code: " + finalResponseCode + "\n");
//                textBox.append("Response Body:\n" + responseBody + "\n");
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
//                textBox.append("\n=== SAMPLE BIO ENDPOINT ERROR ===\n");
//                textBox.append("Error: " + e.getMessage() + "\n");
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

        // Clean up QR scanner resources
        if (barcodeScanner != null) {
            barcodeScanner.pause();
        }
    }

    /**
     * Fetches order details from the API using the provided order ID
     * @param orderId The ID of the order to fetch
     */
    private void fetchOrderDetails(String orderId) {
        executor.execute(() -> {
            try {
                // Read token from SharedPreferences (equivalent to FlutterSecureStorage)
                String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiI2NTEyYzNiYjM5ZmQ3ZmY5NzEzOTY0ZTciLCJlbWFpbCI6ImRuZ2Fib0BhbmRyZXcuY211LmVkdSIsImZ1bGxOYW1lIjoiRGlkaWVyIE5nYWJvIiwicm9sZSI6ImFkbWluIiwiaWF0IjoxNzU2NzEwMTQwLCJleHAiOjE3NTY3OTY1NDB9.bP01lA3zgDhc_XtWcapCqX8R3eU7Bh31jGd1foKYVok";

                if (token == null || token.isEmpty()) {
                    runOnUiThread(() -> {
//                        textBox.append("\nERROR: No authentication token found. Please login first.\n");
                        Toast.makeText(MainActivity.this, "Authentication required", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Create the API URL
                URL url = new URL(ORDER_API_ENDPOINT + orderId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + token);
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
//                    textBox.append("\n=== ORDER DETAILS API RESPONSE ===\n");
//                    textBox.append("Endpoint: " + ORDER_API_ENDPOINT + orderId + "\n");
//                    textBox.append("Response Code: " + finalResponseCode + "\n");

                    if (finalResponseCode == 200) {
                        try {
                            orderDetails = new JSONObject(responseBody);
//                            textBox.append("✓ Order details fetched successfully!\n");
//                            textBox.append("Response Body:\n" + orderDetails.toString(2) + "\n");

                            // Display key order information if available
//                            displayOrderSummary(orderDetails);

                            uinInput.setText(orderDetails.getString("uin"));

                            Toast.makeText(MainActivity.this, "Order details loaded successfully!", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
//                            textBox.append("Error parsing order details: " + e.getMessage() + "\n");
                            Toast.makeText(MainActivity.this, "Error parsing order data", Toast.LENGTH_SHORT).show();
                        }
                    } else if (finalResponseCode == 404) {
//                        textBox.append("❌ Order not found. Please check the QR code.\n");
                        Toast.makeText(MainActivity.this, "Order not found. Please check the QR code.", Toast.LENGTH_LONG).show();
                        // Trigger vibration for error (you may need to implement vibration)
//                        vibrateError();
                    } else {
//                        textBox.append("❌ Failed to fetch order details. Status: " + finalResponseCode + "\n");
//                        textBox.append("Response: " + responseBody + "\n");
                        Toast.makeText(MainActivity.this, "Failed to fetch order details. Status: " + finalResponseCode, Toast.LENGTH_LONG).show();
//                        vibrateError();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
//                    textBox.append("\n=== ORDER API ERROR ===\n");
//                    textBox.append("Network error: " + e.getMessage() + "\n");
                    Toast.makeText(MainActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                    vibrateError();
                });
            }
        });
    }

    /**
     * Displays a summary of the order details in a readable format
     */
//    private void displayOrderSummary(JSONObject orderDetails) {
//        try {
////            textBox.append("\n=== ORDER SUMMARY ===\n");
//
//            // Extract common order fields (adjust based on your API response structure)
//            if (orderDetails.has("_id")) {
//                textBox.append("Order ID: " + orderDetails.getString("_id") + "\n");
//            }
//            if (orderDetails.has("uin")) {
//                textBox.append("Order ID: " + orderDetails.getString("uin") + "\n");
//            }
//            if (orderDetails.has("status")) {
//                textBox.append("Status: " + orderDetails.getString("status") + "\n");
//            }
//            if (orderDetails.has("totalAmount")) {
//                textBox.append("Total Amount: " + orderDetails.getString("totalAmount") + "\n");
//            }
//            if (orderDetails.has("customerName")) {
//                textBox.append("Customer: " + orderDetails.getString("customerName") + "\n");
//            }
//            if (orderDetails.has("items")) {
//                JSONArray items = orderDetails.getJSONArray("items");
//                textBox.append("Items Count: " + items.length() + "\n");
//            }
//            if (orderDetails.has("createdAt")) {
//                textBox.append("Created: " + orderDetails.getString("createdAt") + "\n");
//            }
//
//            textBox.append("========================\n");
//        } catch (Exception e) {
//            textBox.append("Error displaying order summary: " + e.getMessage() + "\n");
//        }
//    }

    /**
     * Gets the authentication token from SharedPreferences
     * In a real app, you should use EncryptedSharedPreferences for security
     */
    private String getAuthToken() {
        if (authToken != null) {
            return authToken;
        }

        // Try to read from SharedPreferences (equivalent to FlutterSecureStorage)
        android.content.SharedPreferences prefs = getSharedPreferences("secure_storage", MODE_PRIVATE);
        authToken = prefs.getString("token", null);

        return authToken;
    }

    /**
     * Sets the authentication token in SharedPreferences
     */
    public void setAuthToken(String token) {
        this.authToken = token;

        // Save to SharedPreferences for persistence
        android.content.SharedPreferences prefs = getSharedPreferences("secure_storage", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString("token", token);
        editor.apply();
    }

    /**
     * Triggers error vibration (simple implementation)
     */
//    private void vibrateError() {
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                android.os.VibrationEffect effect = android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
//                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
//                if (vibrator != null) {
//                    vibrator.vibrate(effect);
//                }
//            } else {
//                // For older versions
//                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
//                if (vibrator != null) {
//                    vibrator.vibrate(500);
//                }
//            }
//        } catch (Exception e) {
//            // Vibration failed, ignore
//        }
//    }

    /**
     * Enhanced QR scanner callback that can handle order IDs
     */
    private void handleQRScanResult(String scannedText) {
        // Check if the scanned text looks like a UIN (numeric, 10-15 digits)
//        if (isValidUIN(scannedText)) {
//            uinInput.setText(scannedText);
//            closeQRScanner();
//            Toast.makeText(this, "UIN scanned successfully!", Toast.LENGTH_SHORT).show();
//        } else {
            // Check if it might be an order ID or URL containing order ID
            String orderId = extractOrderId(scannedText);
            if (orderId != null) {
                closeQRScanner();
//                textBox.setText("Order ID scanned: " + orderId + "\n");
//                textBox.append("Fetching order details...\n");
                fetchOrderDetails(orderId);
                Toast.makeText(this, "Order ID scanned! Fetching details...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Scanned content does not appear to be a valid UIN or Order ID", Toast.LENGTH_LONG).show();
//            }
        }
    }

    /**
     * Extracts order ID from scanned text (handles URLs and plain IDs)
     */
    private String extractOrderId(String scannedText) {
        if (scannedText == null || scannedText.trim().isEmpty()) {
            return null;
        }

        String text = scannedText.trim();

        // If it's a URL, try to extract order ID from it
        if (text.startsWith("http://") || text.startsWith("https://")) {
            try {
                Uri uri = Uri.parse(text);
                // Check for order ID in path or query parameters
                String orderIdFromPath = uri.getLastPathSegment();
                String orderIdFromQuery = uri.getQueryParameter("orderId");

                if (orderIdFromQuery != null && !orderIdFromQuery.isEmpty()) {
                    return orderIdFromQuery;
                } else if (orderIdFromPath != null && !orderIdFromPath.isEmpty()) {
                    return orderIdFromPath;
                }
            } catch (Exception e) {
                // URL parsing failed
            }
        }

        // If it's not a UIN but looks like an order ID (alphanumeric, reasonable length)
        if (text.length() > 5 && text.length() < 50 && text.matches("[a-zA-Z0-9-_]+")) {
            return text;
        }

        return null;
    }
    
    // Methods to handle the new result display UI
    private void showLoadingResult() {
        resultCard.setVisibility(View.VISIBLE);
        loadingLayout.setVisibility(View.VISIBLE);
        successLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
    }
    
    private void showSuccessResult() {
        resultCard.setVisibility(View.VISIBLE);
        loadingLayout.setVisibility(View.GONE);
        successLayout.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        setButtonsEnabled(true);
    }
    
    private void showErrorResult(String errorMsg) {
        resultCard.setVisibility(View.VISIBLE);
        loadingLayout.setVisibility(View.GONE);
        successLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        errorMessage.setText(errorMsg);
        setButtonsEnabled(true);
    }
    
    private void hideResult() {
        resultCard.setVisibility(View.GONE);
    }
}