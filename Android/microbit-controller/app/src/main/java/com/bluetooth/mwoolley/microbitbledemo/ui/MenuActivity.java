package com.bluetooth.mwoolley.microbitbledemo.ui;

import java.util.List;

import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.WindowManager;
import android.widget.Toast;

import com.bluetooth.mwoolley.microbitbledemo.bluetooth.BleAdapterService;
import com.bluetooth.mwoolley.microbitbledemo.bluetooth.ConnectionStatusListener;
import com.bluetooth.mwoolley.microbitbledemo.Microbit.Constants;
import com.bluetooth.mwoolley.microbitbledemo.Microbit.MicroBit;
import com.bluetooth.mwoolley.microbitbledemo.R;
import com.bluetooth.mwoolley.microbitbledemo.Microbit.Utility;

public class MenuActivity extends AppCompatActivity implements ConnectionStatusListener {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";

    private BleAdapterService bluetooth_le_adapter;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetooth_le_adapter = ((BleAdapterService.LocalBinder) service).getService();
            bluetooth_le_adapter.setActivityHandler(mMessageHandler);
            connectToDevice();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetooth_le_adapter = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_menu);
        getSupportActionBar().setTitle(R.string.screen_title_menu);

        Log.d(Constants.TAG, "MenuActivity onCreate");

        // read intent data
        final Intent intent = getIntent();
        MicroBit.getInstance().setMicrobit_name(intent.getStringExtra(EXTRA_NAME));
        MicroBit.getInstance().setMicrobit_address(intent.getStringExtra(EXTRA_ID));
        MicroBit.getInstance().setConnection_status_listener(this);

        // connect to the Bluetooth service
        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();


        if (id == R.id.menu_menu_refresh) {
            refreshBluetoothServices();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshBluetoothServices() {
        if (MicroBit.getInstance().isMicrobit_connected()) {
            Toast toast = Toast.makeText(this, "Refreshing GATT services", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            MicroBit.getInstance().resetAttributeTables();
            bluetooth_le_adapter.refreshDeviceCache();
            bluetooth_le_adapter.discoverServices();
        } else {
            Toast toast = Toast.makeText(this, "Request Ignored - Not Connected", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // may already have unbound. No API to check state so....
            unbindService(mServiceConnection);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MicroBit.getInstance().isMicrobit_connected()) {
            showMsg(Utility.htmlColorGreen("Connected"));
        } else {
            showMsg(Utility.htmlColorRed("Disconnected"));
        }
    }

    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        if (MicroBit.getInstance().isMicrobit_connected()) {
            try {
                bluetooth_le_adapter.disconnect();
                // may already have unbound. No API to check state so....
                unbindService(mServiceConnection);
            } catch (Exception e) {
            }
        }
        finish();
    }

    private void connectToDevice() {
        showMsg(Utility.htmlColorBlue("Connecting to micro:bit"));
        if (bluetooth_le_adapter.connect(MicroBit.getInstance().getMicrobit_address())) {
        } else {
            showMsg(Utility.htmlColorRed("onConnect: failed to connect"));
        }
    }

    public void onDemoSelected(View view) {
        Log.d(Constants.TAG, "onDemoSelected ");
        if (!MicroBit.getInstance().isMicrobit_connected()|| !MicroBit.getInstance().isMicrobit_services_discovered() ) {
            Log.d(Constants.TAG, "onDemoSelected - micro:bit is not connected or service discovery has not completed so ignoring");
            showMsg(Utility.htmlColorRed("Not connected to micro:bit - find and connect again"));
            return;
        }
        ImageView iw = (ImageView) view;
        Intent intent;
        switch (iw.getId()) {

            case R.id.btn_leds:
                Log.d(Constants.TAG, "onDemoSelected LEDs");
                if (MicroBit.getInstance().hasService(BleAdapterService.LEDSERVICE_SERVICE_UUID)) {
                    intent = new Intent(MenuActivity.this, LEDsActivity.class);
                    startActivity(intent);
                } else {
                    showMsg(Utility.htmlColorRed("LED Service not on this micro:bit"));
                }
                break;

        }
    }

    // Service message handler???//////////////////
    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            String service_uuid = "";
            String characteristic_uuid = "";
            byte[] b = null;
            TextView value_text = null;

            switch (msg.what) {
                case BleAdapterService.GATT_CONNECTED:
                    showMsg(Utility.htmlColorGreen("Connected"));
                    showMsg(Utility.htmlColorGreen("Discovering services..."));
                    bluetooth_le_adapter.discoverServices();
                    break;
                case BleAdapterService.GATT_DISCONNECT:
                    showMsg(Utility.htmlColorRed("Disconnected"));
                    ((LinearLayout) MenuActivity.this.findViewById(R.id.menu_items_area)).setVisibility(View.VISIBLE);
                    break;
                case BleAdapterService.GATT_SERVICES_DISCOVERED:
                    Log.d(Constants.TAG, "XXXX Services discovered");
                    showMsg(Utility.htmlColorGreen("Ready"));
                    ((LinearLayout) MenuActivity.this.findViewById(R.id.menu_items_area)).setVisibility(View.VISIBLE);
                    List<BluetoothGattService> slist = bluetooth_le_adapter.getSupportedGattServices();
                    for (BluetoothGattService svc : slist) {
                        Log.d(Constants.TAG, "UUID=" + svc.getUuid().toString().toUpperCase() + " INSTANCE=" + svc.getInstanceId());
                        MicroBit.getInstance().addService(svc);
                    }
                    MicroBit.getInstance().setMicrobit_services_discovered(true);
                    break;
            }
        }
    };

    private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg);
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      ((TextView) MenuActivity.this.findViewById(R.id.message)).setText(Html.fromHtml(msg));
                                  }
                              });
                          }
                      }
        );
    }

    @Override
    public void connectionStatusChanged(boolean connected) {
        if (connected) {
            showMsg(Utility.htmlColorGreen("Connected"));
        } else {
            showMsg(Utility.htmlColorRed("Disconnected"));
        }
    }

    @Override
    public void serviceDiscoveryStatusChanged(boolean new_state) {

    }
}