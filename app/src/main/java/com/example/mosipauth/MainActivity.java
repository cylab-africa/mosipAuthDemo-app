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
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_DISCOVER = 1;
    private static final int REQUEST_INFO = 2;
    private static final int REQUEST_CAPTURE = 3;

    MaterialButton btnDiscover, btnInfo, btnCapture, btnShare;
    MaterialTextView textBox;
    String appID = null;
    String serialNo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDiscover = findViewById(R.id.discover);
        btnInfo = findViewById(R.id.info);
        btnCapture = findViewById(R.id.capture);
        btnShare = findViewById(R.id.share);
        textBox = findViewById(R.id.textbox);
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
        btnInfo.setEnabled(appID != null);
        btnCapture.setEnabled(serialNo != null);
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
        Intent intent = new Intent();
        intent.setAction(appID + ".Capture");

        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
        Collections.sort(activities,new ResolveInfo.DisplayNameComparator(packageManager));
        final boolean isIntentSafe = activities.size() > 0;
        if(isIntentSafe) {
            if(null == serialNo){
                Toast.makeText(MainActivity.this, "Perform info request", Toast.LENGTH_SHORT).show();
                return;
            }
            CaptureRequestDto captureRequestDto = new CaptureRequestDto();
            captureRequestDto.captureTime = "2021-07-18T17:56:11Z";
            captureRequestDto.env = "Production";
            captureRequestDto.purpose = "Auth";
            captureRequestDto.specVersion = "0.9.5";
            captureRequestDto.timeout = 10000;
            captureRequestDto.domainUri = "https://extint1.mosip.net";
            captureRequestDto.transactionId = "1626630971975";
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

            try {
                intent.putExtra("input", new ObjectMapper().writeValueAsBytes(captureRequestDto));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            startActivityForResult(intent, REQUEST_CAPTURE);
        }else {
            Toast.makeText(MainActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(REQUEST_DISCOVER == requestCode){
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
                                textBox.setText("Discover Response :\n" + response);
                                Toast.makeText(MainActivity.this, appID, Toast.LENGTH_SHORT).show();
                            }else {
                                textBox.setText(errorObject.toString());
                            }
                        }else {
                            textBox.setText("Response not found");
                        }
                    }else {
                        textBox.setText("Response not found");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            initViews();
        }else if(REQUEST_INFO == requestCode){
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
                                textBox.setText("Info Response :\n" + response);
                            }else{
                                textBox.setText(response);
                            }
                        }else {
                            textBox.setText("Info response not found");
                        }
                    }else {
                        textBox.setText("Info response not found");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            initViews();
        }else if(REQUEST_CAPTURE == requestCode){
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
                                    textBox.setText("Capture response :\n" + response);
                                } else {
                                    textBox.setText(response);
                                }
                            }else{
                                textBox.setText(resposeObject.toString());
                            }
                        }else {
                            textBox.setText("Capture response not found");
                        }
                    }else {
                        textBox.setText("Capture response not found");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
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
}