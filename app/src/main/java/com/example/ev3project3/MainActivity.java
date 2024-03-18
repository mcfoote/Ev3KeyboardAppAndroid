package com.example.ev3project3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

//// HERE
import java.io.InputStream;
import java.io.OutputStream;

// ver01 -- sending Direct Command


public class MainActivity extends AppCompatActivity {
    // BT Variables
    private final String CV_ROBOTNAME = "MFJRW24";
    private BluetoothAdapter cv_btInterface = null;
    private Set<BluetoothDevice> cv_pairedDevices = null;
    private BluetoothDevice cv_btDevice = null;
    private BluetoothSocket cv_btSocket = null;

    // Data stream to/from NXT bluetooth
    private InputStream cv_is = null;
    private OutputStream cv_os = null;

    TextView cv_label01;
    TextView cv_label02;

    SeekBar speedbar;
    boolean bkwdButtonHeld = false;
    boolean fdwButtonHeld = false;
    private boolean isBackwards = false;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        Button cv_buttonBkwd = (Button) findViewById(R.id.vv_buttonBkwd);
        Button cv_buttonFwd = (Button) findViewById(R.id.vv_buttonFwd);
        cv_label01 = (TextView) findViewById(R.id.vv_tvOut1);
        cv_label02 = (TextView) findViewById(R.id.vv_tvOut2);
        speedbar = findViewById(R.id.vv_speedBar);
        TextView cv_speedLbl = findViewById(R.id.vv_speedLbl);
        int initialValue = 50; // This is the initial value you want to set
        speedbar.setProgress(initialValue);
        */


        // Need grant permission once per install
        cpf_checkBTPermissions();




        speedbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update TextView with the current progress of the SeekBar
                //cv_speedLbl.setText("Speed: " + String.valueOf(progress));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // You can also implement some action here when the user first touches the SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // And here you can implement some action when the user releases the SeekBar
            }
        });
    }

//    private void moveForward(){
//        new Thread(() -> {
//            while (fdwButtonHeld) {
//                cpf_EV3MoveMotor();
//                try {
//                    Thread.sleep(50); // Add a delay to control update rate
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }
//
//    private void moveBackward(){
//        new Thread(() -> {
//            while (bkwdButtonHeld) {
//                cpf_EV3MoveMotorBackward();
//                try {
//                    Thread.sleep(50); // Add a delay to control update rate
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.menu_first) {
            cpf_requestBTPermissions();
            return true;
        } else if (id == R.id.menu_second) {
            cv_btDevice = cpf_locateInPairedBTList(CV_ROBOTNAME);
            return true;
        } else if (id == R.id.menu_third) {
            cpf_connectToEV3(cv_btDevice);
            return true;
        } else if (id == R.id.menu_fourth) {
            cpf_EV3MoveMotor();
            return true;
        } else if (id == R.id.menu_fifth) {
            cpf_EV3PlayTone();
            return true;
        } else if (id == R.id.menu_sixth) {
            cpf_disconnFromEV3(cv_btDevice);
            return true;
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        /*switch (menuItem.getItemId()) {
            case R.id.menu_first: cpf_requestBTPermissions();
                return true;
            case R.id.menu_second: cv_btDevice = cpf_locateInPairedBTList(CV_ROBOTNAME);
                return true;
            case R.id.menu_third: cpf_connectToEV3(cv_btDevice);
                return true;
            case R.id.menu_fourth: cpf_EV3MoveMotor();
                return true;
            case R.id.menu_fifth: cpf_EV3PlayTone();
                return true;
            case R.id.menu_sixth: cpf_disconnFromEV3(cv_btDevice);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }*/

    }

    private void cpf_checkBTPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            cv_label01.setText("BLUETOOTH_SCAN already granted.\n");
        } else {
            cv_label01.setText("BLUETOOTH_SCAN NOT granted.\n");
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            cv_label02.setText("BLUETOOTH_CONNECT NOT granted.\n");
        } else {
            cv_label02.setText("BLUETOOTH_CONNECT already granted.\n");
        }
    }

    // https://www.geeksforgeeks.org/android-how-to-request-permissions-in-android-application/
    // https://stackoverflow.com/questions/67722950/android-12-new-bluetooth-permissions
    private void cpf_requestBTPermissions() {
        // We can give any value but unique for each permission.
        final int BLUETOOTH_SCAN_CODE = 100;
        final int BLUETOOTH_CONNECT_CODE = 101;

        // Android version < 12, "android.permission.BLUETOOTH" just fine
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH granted for earlier Android", Toast.LENGTH_SHORT).show();
            return;
        }

        // Android 12+ has to go through the process
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    BLUETOOTH_SCAN_CODE);
        } else {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH_SCAN already granted", Toast.LENGTH_SHORT).show();
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    BLUETOOTH_CONNECT_CODE);
        } else {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH_CONNECT already granted", Toast.LENGTH_SHORT).show();
        }
    }

    // Modify from chap14, pp390 findRobot()
    private BluetoothDevice cpf_locateInPairedBTList(String name) {
        BluetoothDevice lv_bd = null;
        try {
            cv_btInterface = BluetoothAdapter.getDefaultAdapter();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            cv_pairedDevices = cv_btInterface.getBondedDevices();
            Iterator<BluetoothDevice> lv_it = cv_pairedDevices.iterator();
            while (lv_it.hasNext()) {
                lv_bd = lv_it.next();
                if (lv_bd.getName().equalsIgnoreCase(name)) {
                    cv_label01.setText(name + " is in paired list");
                    return lv_bd;
                }
            }
            cv_label01.setText(name + " is NOT in paired list");
        } catch (Exception e) {
            cv_label01.setText("Failed in findRobot() " + e.getMessage());
        }
        return null;
    }

    // Modify frmo chap14, pp391 connectToRobot()
    private void cpf_connectToEV3(BluetoothDevice bd) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cv_btSocket = bd.createRfcommSocketToServiceRecord
                    (UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            cv_btSocket.connect();

            //// HERE
            cv_is = cv_btSocket.getInputStream();
            cv_os = cv_btSocket.getOutputStream();
            cv_label02.setText("Connect to " + bd.getName() + " at " + bd.getAddress());
            isConnected = true;
        } catch (Exception e) {
            cv_label02.setText("Error interacting with remote device [" +
                    e.getMessage() + "]");
        }
    }

    private void cpf_disconnFromEV3(BluetoothDevice bd) {
        try {
            cv_btSocket.close();
            cv_is.close();
            cv_os.close();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cv_label02.setText(bd.getName() + " is disconnect ");
            isConnected = false;
        } catch (Exception e) {
            cv_label02.setText("Error in disconnect -> " + e.getMessage());
        }
    }

    private void cpf_EV3StopMotor(){
        try{
            byte[] buffer = new byte[11]; // 0x11 command length

            buffer[0] = (byte) (11 - 2);
            buffer[1] = 0;

            buffer[2] = 9;
            buffer[3] = 42;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xa3;
            buffer[8] = 0;
            buffer[9] = (byte) 0x0f;

            buffer[10] = 1;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch(Exception e){
            cv_label01.setText("Error in Brake(" + e.getMessage() + ")");
        }
    }

    private void cpf_EV3Move(){
        try{
            int speed;
            if (isBackwards == false){
                speed = speedbar.getProgress();
            }else{
                speed = -speedbar.getProgress();
            }

            byte[] buffer = new byte[15]; // 0x15 command length

            buffer[0] = (byte) (15 - 2);
            buffer[1] = 0;

            buffer[2] = 13;
            buffer[3] = 42;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xa4;
            buffer[8] = 0;
            buffer[9] = (byte) 0x0f;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) speed;

            buffer[12] = (byte) 0xa6;
            buffer[13] = 0;
            buffer[14] = (byte) 0x0f;

            cv_os.write(buffer);
            cv_os.flush();

        }
        catch(Exception e){
            cv_label01.setText("Error in Move(" + e.getMessage() + ")");
        }
    }


    // Communication Developer Kit Page 27
    // 4.2.2 Start motor B & C forward at power 50 for 3 rotation and braking at destination
    private void cpf_EV3MoveMotor() {
        try {
            //byte[] buffer = new byte[15];       // 0x12 command length

            byte[] buffer = new byte[15]; // 0x15 command length

            buffer[0] = (byte) (15 - 2);
            buffer[1] = 0;

            buffer[2] = 13;
            buffer[3] = 42;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xa4;
            buffer[8] = 0;
            buffer[9] = (byte) 0x0f;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) speedbar.getProgress();

            buffer[12] = (byte) 0xa6;
            buffer[13] = 0;
            buffer[14] = (byte) 0x0f;

//            buffer[15] = 0;
//
//            buffer[16] = 0;
//            buffer[17] = 0;
//            buffer[18] = 0;
//
//            buffer[19] = 1;

//            buffer[0] = (byte) (20-2);
//            buffer[1] = 0;
//
//            buffer[2] = 34;
//            buffer[3] = 12;
//
//            buffer[4] = (byte) 0x80;
//
//            buffer[5] = 0;
//            buffer[6] = 0;
//
//            buffer[7] = (byte) 0xae;
//            buffer[8] = 0;
//
//            buffer[9] = (byte) 0x06;
//
//            buffer[10] = (byte) 0x81;
//            buffer[11] = (byte) speedbar.getProgress();
//
//            buffer[12] = 0;
//
//            buffer[13] = (byte) 0x82;
//            buffer[14] = (byte) 0x84;
//            buffer[15] = (byte) 0x03;
//
//            buffer[16] = (byte) 0x82;
//            buffer[17] = (byte) 0xB4;
//            buffer[18] = (byte) 0x00;
//
//            buffer[19] = 1;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            cv_label01.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }

    private void cpf_EV3MoveMotorBackward() {
        try {
            byte[] buffer = new byte[15]; // 0x15 command length

            buffer[0] = (byte) (15 - 2);
            buffer[1] = 0;

            buffer[2] = 13;
            buffer[3] = 42;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xa4;
            buffer[8] = 0;
            buffer[9] = (byte) 0x0f;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) -speedbar.getProgress();

            buffer[12] = (byte) 0xa6;
            buffer[13] = 0;
            buffer[14] = (byte) 0x0f;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            cv_label01.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }

    // 4.2.5 Play a 1Kz tone at level 2 for 1 sec.
    private void cpf_EV3PlayTone() {
        try {
            byte[] buffer = new byte[17];       // 0x0f command length

            buffer[0] = (byte) (17-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0x94;
            buffer[8] = 1;

            buffer[9] = (byte) 0x81;
            buffer[10] = (byte) 0x02;

            buffer[11] = (byte) 0x82;
            buffer[12] = (byte) 0xd0;
            buffer[13] = (byte) 0x07;

            buffer[14] = (byte) 0x82;
            buffer[15] = (byte) 0xe8;
            buffer[16] = (byte) 0x03;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            cv_label02.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }
}