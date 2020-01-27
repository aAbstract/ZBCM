package com.example.zbcm;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.util.Set;

public class MainActivity extends Activity implements SensorEventListener {
    String console_log = "";
    TextView console;
    EditText dev_name_tv;
    ImageView bt_icon;
    ImageButton btn;
    static final UUID BTUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter blutooth_handler = null;
    BluetoothSocket rfcom_socket = null;
    Set<BluetoothDevice> bonded_devices;
    BluetoothDevice connected_device = null;
    BluetoothDevice bonded_device = null;
    String dev_address = "";
    String dev_name = "";
    BluetoothSocket connection_socket = null;
    boolean is_success = false;
    OutputStream out = null;
    InputStream in = null;
    DataInputStream in_handler = null;
    DataOutputStream out_handler = null;
    Handler ui_handler = new Handler();
    RelativeLayout layout_joystick;
    JoyStickClass js;
    char current_dir = 'n';
    String current_gyro = "";
    SensorManager sensors_handler = null;

    class network_callback implements Runnable {
        private String data;

        public network_callback(String _data) {
            this.data = _data;
        }

        @Override
        public void run() {
            console_log("[Received]: " + data);
        }
    }

    void console_log(String text) {
        console_log += text;
        console_log += "\n";
        console.setText(console_log);
    }

    void prepare_environment(String dev_name) throws IOException {
        blutooth_handler = BluetoothAdapter.getDefaultAdapter();
        bonded_devices = blutooth_handler.getBondedDevices();
        if (bonded_devices.size() > 0) {
            for (BluetoothDevice dev : bonded_devices) {
                if (dev.getName().equals(dev_name)) {
                    dev_address = dev.getAddress().toString();
                    dev_name = dev.getName().toString();
                    console_log("Connecting To Device: " + dev_name + " Address: " + dev_address + " ...");
                    connected_device = blutooth_handler.getRemoteDevice(dev_address);
                    connection_socket = connected_device.createInsecureRfcommSocketToServiceRecord(BTUUID);
                    connection_socket.connect();
                    bt_icon.setImageResource(R.drawable.ic_bluetooth_searching_black_24dp);
                    is_success = true;
                    out = connection_socket.getOutputStream();
                    in = connection_socket.getInputStream();
                    in_handler = new DataInputStream(new BufferedInputStream(in));
                    out_handler = new DataOutputStream(new BufferedOutputStream(out));
                    console_log("Connection Done");
                }
            }
        }
    }

    void start_network_reader() {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (true) {
                                if (in_handler.available() > 0) {
                                    String line = in_handler.readLine();
                                    ui_handler.post(new network_callback(line));
                                }
                            }
                        } catch (Exception e) {
                            console_log(e.getMessage());
                        }
                    }
                }
        ).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        console = (TextView) findViewById(R.id.console);
        console.setMovementMethod(new ScrollingMovementMethod());
        bt_icon = (ImageView) findViewById(R.id.connect_btn);
        dev_name_tv = (EditText) findViewById(R.id.name_tv);
        sensors_handler = (SensorManager) getSystemService(SENSOR_SERVICE);
        btn = (ImageButton) findViewById(R.id.connect_btn);
        layout_joystick = (RelativeLayout) findViewById(R.id.layout_joystick);
        js = new JoyStickClass(getApplicationContext(), layout_joystick, R.drawable.image_button);
        js.setStickSize(150, 150);
        js.setLayoutSize(500, 500);
        js.setLayoutAlpha(150);
        js.setStickAlpha(100);
        js.setOffset(90);
        js.setMinimumDistance(50);
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String dn = dev_name_tv.getText().toString();
                        try {
                            prepare_environment(dn);
                            if (is_success) {
                                start_network_reader();
                            }
                        } catch (Exception e) {
                            console_log(e.getMessage());
                        }
                    }
                }
        );
        layout_joystick.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js.drawStick(arg1);
                if (arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    int direction = js.get4Direction();
                    if (direction == JoyStickClass.STICK_UP) {
                        current_dir = 'w';
                    } else if (direction == JoyStickClass.STICK_RIGHT) {
                        current_dir = 'd';
                    } else if (direction == JoyStickClass.STICK_DOWN) {
                        current_dir = 's';
                    } else if (direction == JoyStickClass.STICK_LEFT) {
                        current_dir = 'a';
                    } else if (direction == JoyStickClass.STICK_NONE) {
                        current_dir = 'n';
                    }
                } else if (arg1.getAction() == MotionEvent.ACTION_UP) {
                    current_dir = 'n';

                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensors_handler.registerListener(this, sensors_handler.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop() {
        sensors_handler.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (is_success) {
            current_gyro = Float.toString(event.values[2]) + ", " + Float.toString(event.values[1]) + ", " + Float.toString(event.values[0]);
            String network_payload = current_dir + ", " + current_gyro + "\n";
            try {
                out_handler.writeChars(network_payload);
            } catch (Exception e) {
                console_log(e.getMessage());
            }
        }
    }
}
