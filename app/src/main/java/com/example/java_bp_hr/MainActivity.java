package com.example.java_bp_hr;

import static com.google.android.gms.fitness.data.DataPoint.*;
import static com.google.android.gms.fitness.data.DataType.*;
import static com.google.android.gms.fitness.data.Field.*;
import static com.google.android.gms.fitness.data.HealthDataTypes.TYPE_BLOOD_PRESSURE;
import static com.google.android.gms.fitness.data.HealthDataTypes.TYPE_BODY_TEMPERATURE;
import static com.google.android.gms.fitness.data.HealthDataTypes.TYPE_OXYGEN_SATURATION;
import static com.google.android.gms.fitness.data.HealthFields.FIELD_BLOOD_PRESSURE_DIASTOLIC;
import static com.google.android.gms.fitness.data.HealthFields.FIELD_BLOOD_PRESSURE_SYSTOLIC;
import static com.google.android.gms.fitness.data.HealthFields.FIELD_BODY_TEMPERATURE;
import static com.google.android.gms.fitness.data.HealthFields.FIELD_OXYGEN_SATURATION;
import static com.google.android.gms.fitness.data.HealthFields.FIELD_OXYGEN_SATURATION_MEASUREMENT_METHOD;
import static com.google.android.gms.fitness.data.HealthFields.FIELD_OXYGEN_SATURATION_SYSTEM;
import static com.google.android.gms.fitness.data.HealthFields.FIELD_OXYGEN_THERAPY_ADMINISTRATION_MODE;
import static com.google.android.gms.fitness.data.HealthFields.FIELD_SUPPLEMENTAL_OXYGEN_FLOW_RATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import android.graphics.Color;
import com.github.mikephil.charting.components.YAxis;

import android.content.Context;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.request.DataUpdateRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.TimeUnit;



public class MainActivity extends Activity {
    private static final int RC_SIGN_IN = 9001; // Request code for the sign-in intent
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 3;

    private GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private static final UUID BLOOD_PRESSURE_SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    private static final UUID BLOOD_PRESSURE_MEASUREMENT_UUID = UUID.fromString("00002A35-0000-1000-8000-00805f9b34fb");
    private static final int REQUEST_ENABLE_LOCATION = 123;


    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothLeScanner mBluetoothLeScanner;
    private List<Float> systolicHistory = new ArrayList<>(Collections.nCopies(10, 0.0f));
    private List<Float> diastolicHistory = new ArrayList<>(Collections.nCopies(10, 0.0f));
    private List<Float> heartRateHistory = new ArrayList<>(Collections.nCopies(10, 0.0f));

    private List<Float> temperatureHistory = new ArrayList<>(Collections.nCopies(10, 0.0f));
    private List<Float> spo2History = new ArrayList<>(Collections.nCopies(10, 0.0f));

    private float latestSystolic = 0, latestDiastolic = 0, latestHeartRate = 0, latestTemperature = 0, latestSpO2 = 0;


    private TextView tvBleStatus, tvHighestValue, tvLowestValue, tvLastTimeReceived, tvLastValues;

    private LineChart chartHeartRate, chartBloodPressure, chartTemperature, chartSpO2;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestBluetoothPermissions();
        checkAndRequestLocationPermission();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Initialize Google API client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        // Initialize TextViews
        tvBleStatus = findViewById(R.id.tv_ble_status);
        tvLastTimeReceived = findViewById(R.id.tv_last_time_received);
        tvHighestValue = findViewById(R.id.tv_highest_values);
        tvLowestValue = findViewById(R.id.tv_lowest_values);
        tvLastValues = findViewById(R.id.tv_latest_values);

        // Initialize Charts
        chartHeartRate = findViewById(R.id.chart_heart_rate);
        chartBloodPressure = findViewById(R.id.chart_blood_pressure);
        chartTemperature = findViewById(R.id.chart_temperature);
        chartSpO2 = findViewById(R.id.chart_spo2);

        initializeChart(chartHeartRate, 200f); // Assuming max heart rate of 200 for yAxis
        initializeChart(chartBloodPressure, 200f); // Assuming max BP of 200 for yAxis
        initializeChart(chartTemperature, 120f); // Assuming max temperature of 120 for yAxis
        initializeChart(chartSpO2, 105f); // Assuming max SpO2 of 105 for yAxis

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            // User is not signed in, initiate Google Sign-In
            startGoogleSignIn();
        } else {
            // User is already signed in, you can use 'account' to access user information
        }
        requestFitnessPermissions();


    }

    private void startGoogleSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

    }

    private void requestFitnessPermissions() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            FitnessOptions fitnessOptions = FitnessOptions.builder()
                    .addDataType(TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_WRITE)
                    .addDataType(TYPE_OXYGEN_SATURATION, FitnessOptions.ACCESS_WRITE)
                    .addDataType(TYPE_BODY_TEMPERATURE, FitnessOptions.ACCESS_WRITE)
                    .addDataType(TYPE_BLOOD_PRESSURE, FitnessOptions.ACCESS_WRITE)
                    .build();

            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                        this,
                        GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                        account,
                        fitnessOptions
                );
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // User signed in successfully
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                if (account != null) {
                    // You can use 'account' to access user information
                }
            } else {
                // Sign-in was canceled or failed
                // Handle the failure or cancellation here
            }
        }
    }





    private void checkAndRequestBluetoothPermissions() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            startScanning();

        } else {
            checkAndRequestLocationPermission();
        }
    }
    private void checkAndRequestLocationPermission() {
        // Check if GPS is enabled
        if (isGPSEnabled()) {
            // GPS is enabled, start scanning
            startScanning();
        } else {
            // GPS is not enabled, show the dialog
            showEnableGPSDialog();
        }

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            startScanning();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            startScanning();

        } else {
            // Location permission is granted, start scanning
            startScanning();
        }
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

    }

    private void showEnableGPSDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable GPS")
                .setMessage("To use this app, please enable GPS in your device settings.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    // Open device settings to enable GPS
                    Intent enableLocationIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(enableLocationIntent, REQUEST_ENABLE_LOCATION);


                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Handle if the user cancels enabling GPS
                    // You can show another message or take appropriate action here.
                })
                .show();

    }



    @SuppressLint("MissingPermission")
    private void startScanning() {
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(scanCallback);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            BluetoothDevice device = result.getDevice();
            if ("BP_HR_IP".equals(device.getName()) && !isConnected) {
                mBluetoothGatt = device.connectGatt(MainActivity.this, true, gattCallback);
                mBluetoothLeScanner.stopScan(scanCallback);

            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                runOnUiThread(() -> tvBleStatus.setText("BLE Status: Connected"));
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                isConnected = false; // Update the connection state flag
                runOnUiThread(() -> tvBleStatus.setText("BLE Status: Disconnected"));
                // Optionally, you can close the GATT client
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
                // If you want to automatically start scanning again after a disconnect, uncomment the line below
                startScanning();
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(BLOOD_PRESSURE_SERVICE_UUID);
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(BLOOD_PRESSURE_MEASUREMENT_UUID);
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (BLOOD_PRESSURE_MEASUREMENT_UUID.equals(characteristic.getUuid())) {
                byte[] value = characteristic.getValue();
                float systolic = floatToFloat(ByteBuffer.wrap(value, 1, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
                float diastolic = floatToFloat(ByteBuffer.wrap(value, 3, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
                float heartRate = floatToFloat(ByteBuffer.wrap(value, 7, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
                float temp = floatToFloat(ByteBuffer.wrap(value, 13, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
                float spo2 = floatToFloat(ByteBuffer.wrap(value, 15, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());

                latestSystolic = systolic;
                latestDiastolic = diastolic;
                latestHeartRate = heartRate;
                latestTemperature = temp;
                latestSpO2 = spo2;

                // Dummy data for the new measurements

                updateHistory(systolicHistory, systolic);
                updateHistory(diastolicHistory, diastolic);
                updateHistory(heartRateHistory, heartRate);
                updateHistory(temperatureHistory, temp);
                updateHistory(spo2History, spo2);

                sendDataToGoogleFit(heartRate,99,temp,systolic,diastolic);

            }




            runOnUiThread(() -> {
                updateHeartRateChart(heartRateHistory);
                updateBloodPressureChart(systolicHistory, diastolicHistory);
                updateTemperatureChart(temperatureHistory);
                updateSpO2Chart(spo2History);

                tvHighestValue.setText("Highest Values:\nSystolic: " + Collections.max(systolicHistory) + " mmHg\nDiastolic: " + Collections.max(diastolicHistory) + " mmHg\nHeart Rate: " + Collections.max(heartRateHistory) + " BPM\nTemperature: " + Collections.max(temperatureHistory) + " °C\n" + Collections.max(spo2History) + " %");
                tvLowestValue.setText("Lowest Values:\nSystolic: " + Collections.min(systolicHistory) + " mmHg\nDiastolic: " + Collections.min(diastolicHistory) + " mmHg\nHeart Rate: " + Collections.min(heartRateHistory) + " BPM\nTemperature: " + Collections.min(temperatureHistory) + " °C\n" + Collections.min(spo2History) + " %");
                tvLastValues.setText("Latest Values:\nSystolic: " + latestSystolic + " mmHg\nDiastolic: " + latestDiastolic + " mmHg\nHeart Rate: " + latestHeartRate + " BPM\nTemperature: " + latestTemperature + " °C\nSpO2: " + latestSpO2 + " %");
                tvLastTimeReceived.setText("Last Time Received: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            });

        }


    };


    private float floatToFloat(short floatValue) {
        int exponent = (floatValue & 0xF000) >> 12;
        int mantissa = floatValue & 0x0FFF;
        if (exponent == 0x07) {
            if (mantissa == 0x00) return Float.NaN;
            if (mantissa == 0x01) return Float.NEGATIVE_INFINITY;
            if (mantissa == 0x02) return Float.POSITIVE_INFINITY;
        }
        return (float) (mantissa * Math.pow(10, exponent - 3));
    }

    private synchronized void updateHistory(List<Float> historyList, float newValue) {
        if (historyList.size() >= 10) {
            historyList.remove(0);
        }
        historyList.add(newValue);
    }

    private void updateChart(LineChart chart, float systolic, float diastolic, float heartRate) {
        List<Entry> systolicEntries = new ArrayList<>();
        List<Entry> diastolicEntries = new ArrayList<>();

        systolicEntries.add(new Entry(0, systolic));
        diastolicEntries.add(new Entry(0, diastolic));

        LineDataSet systolicDataSet = new LineDataSet(systolicEntries, "Systolic");
        systolicDataSet.setColor(Color.RED);
        systolicDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        LineDataSet diastolicDataSet = new LineDataSet(diastolicEntries, "Diastolic");
        diastolicDataSet.setColor(Color.BLUE);
        diastolicDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        LineData lineData = new LineData(systolicDataSet, diastolicDataSet);
        chart.setData(lineData);

        chart.getAxisLeft().setAxisMinimum(0);
        chart.getAxisLeft().setAxisMaximum(200);
        chart.getAxisRight().setEnabled(false);
        chart.setAutoScaleMinMaxEnabled(false);
        chart.invalidate();

    }

    private void updateChart(LineChart chart, List<Float> systolicHistory, List<Float> diastolicHistory, List<Float> heartRateHistory) {
        List<Entry> systolicEntries = new ArrayList<>();
        List<Entry> diastolicEntries = new ArrayList<>();

        for (int i = 0; i < systolicHistory.size(); i++) {
            systolicEntries.add(new Entry(i, systolicHistory.get(i)));
            diastolicEntries.add(new Entry(i, diastolicHistory.get(i)));
        }

        LineDataSet systolicDataSet = new LineDataSet(systolicEntries, "Systolic");
        systolicDataSet.setColor(Color.RED);
        systolicDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        LineDataSet diastolicDataSet = new LineDataSet(diastolicEntries, "Diastolic");
        diastolicDataSet.setColor(Color.BLUE);
        diastolicDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        LineData lineData = new LineData(systolicDataSet, diastolicDataSet);
        chart.setData(lineData);

        chart.getAxisLeft().setAxisMinimum(0);
        chart.getAxisLeft().setAxisMaximum(200);
        chart.getAxisRight().setEnabled(false);
        chart.setAutoScaleMinMaxEnabled(false);
        chart.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Both permissions are granted
                startScanning();
            } else {
                // One or both permissions are denied
                Toast.makeText(this, "Location permissions are required for Bluetooth scanning.", Toast.LENGTH_LONG).show();
                startScanning();
            }
        }
    }



    private void initializeChart(LineChart chart, float yAxisMax) {
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMaximum(yAxisMax);
        yAxis.setAxisMinimum(0f);
    }

    private void updateChart(LineChart chart, List<Float> history, String label, int color) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            entries.add(new Entry(i, history.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setValueTextColor(Color.BLACK);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void updateTemperatureChart(List<Float> temperatureHistory) {
        updateChart(chartTemperature, temperatureHistory, "Temperature", Color.GREEN);

    }



    private void updateSpO2Chart(List<Float> spo2History) {
        updateChart(chartSpO2, spo2History, "SpO2", Color.CYAN);

    }

    private void updateHeartRateChart(List<Float> heartRateHistory) {
        updateChart(chartHeartRate, heartRateHistory, "Heart Rate", Color.RED);
    }

    private void updateBloodPressureChart(List<Float> systolicHistory, List<Float> diastolicHistory) {
        List<Entry> systolicEntries = new ArrayList<>();
        List<Entry> diastolicEntries = new ArrayList<>();

        for (int i = 0; i < systolicHistory.size(); i++) {
            systolicEntries.add(new Entry(i, systolicHistory.get(i)));
        }

        for (int i = 0; i < diastolicHistory.size(); i++) {
            diastolicEntries.add(new Entry(i, diastolicHistory.get(i)));
        }

        LineDataSet systolicDataSet = new LineDataSet(systolicEntries, "Systolic");
        systolicDataSet.setColor(Color.RED);
        systolicDataSet.setValueTextColor(Color.BLACK);

        LineDataSet diastolicDataSet = new LineDataSet(diastolicEntries, "Diastolic");
        diastolicDataSet.setColor(Color.BLUE);
        diastolicDataSet.setValueTextColor(Color.BLACK);

        LineData lineData = new LineData(systolicDataSet, diastolicDataSet);
        chartBloodPressure.setData(lineData);
        chartBloodPressure.invalidate();
    }




    private void sendDataToGoogleFit(float heartRate, float spo2, float temp, float systolicBP, float diastolicBP) {
        // Retrieve the signed-in Google account
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            // User is signed in, proceed with sending data to Google Fit

            // Create data sources for each data type
            DataSource heartRateDataSource = new DataSource.Builder()
                    .setAppPackageName(getPackageName())
                    .setDataType(TYPE_HEART_RATE_BPM)
                    .setType(DataSource.TYPE_RAW)
                    .build();

            DataSource spo2DataSource = new DataSource.Builder()
                    .setAppPackageName(getPackageName())
                    .setDataType(TYPE_OXYGEN_SATURATION)
                    .setType(DataSource.TYPE_RAW)
                    .build();

            DataSource tempDataSource = new DataSource.Builder()
                    .setAppPackageName(getPackageName())
                    .setDataType(TYPE_BODY_TEMPERATURE)
                    .setType(DataSource.TYPE_RAW)
                    .build();

            DataSource bpDataSource = new DataSource.Builder()
                    .setAppPackageName(getPackageName())
                    .setDataType(TYPE_BLOOD_PRESSURE)
                    .setType(DataSource.TYPE_RAW)
                    .build();

            // Create DataPoint objects for each data type
            DataPoint heartRateDataPoint = DataPoint.builder(heartRateDataSource)
                    .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setField(FIELD_BPM, heartRate)
                    .build();

            DataPoint spo2DataPoint = DataPoint.builder(spo2DataSource)
                    .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setField(FIELD_OXYGEN_SATURATION, spo2)
                    .setField(FIELD_SUPPLEMENTAL_OXYGEN_FLOW_RATE, 0.0f) // Set the supplemental oxygen flow rate
                    .build();

            DataPoint tempDataPoint = DataPoint.builder(tempDataSource)
                    .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setField(FIELD_BODY_TEMPERATURE, temp)
                    .build();

            DataPoint bpDataPoint = DataPoint.builder(bpDataSource)
                    .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setField(FIELD_BLOOD_PRESSURE_SYSTOLIC, systolicBP)
                    .setField(FIELD_BLOOD_PRESSURE_DIASTOLIC, diastolicBP)
                    .build();

            // Build a DataSet with each data point
            DataSet heartRateDataSet = DataSet.builder(heartRateDataSource)
                    .add(heartRateDataPoint)
                    .build();

            DataSet spo2DataSet = DataSet.builder(spo2DataSource)
                    .add(spo2DataPoint)
                    .build();

            DataSet tempDataSet = DataSet.builder(tempDataSource)
                    .add(tempDataPoint)
                    .build();

            DataSet bpDataSet = DataSet.builder(bpDataSource)
                    .add(bpDataPoint)
                    .build();

            // Insert the data sets into Google Fit
            insertDataSet(account, heartRateDataSet);
            insertDataSet(account, spo2DataSet);
            insertDataSet(account, tempDataSet);
            insertDataSet(account, bpDataSet);
        } else {
            // User is not signed in, handle this case (e.g., prompt the user to sign in).
            // You may want to display a message or take appropriate action here.
            Log.e("Google Fit", "User is not signed in to Google account.");
        }
    }

    private void insertDataSet(GoogleSignInAccount account, DataSet dataSet) {
        Task<Void> response = Fitness.getHistoryClient(this, account)
                .insertData(dataSet);

        response.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Data successfully inserted into Google Fit
                Log.d("Google Fit", "Data inserted successfully.");
            }
        });

        response.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Handle failure
                Log.e("Google Fit", "Failed to insert data: " + e.getMessage());
            }
        });
    }


}
