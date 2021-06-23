package cordova.plugin.finerprintscan;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.acpl.access_computech_fm220_sdk.*;

/**
 * This class echoes a string called from JavaScript.
 */
public class fingerprintscan extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scan")) {
            this.scan(message, callbackContext);
            return true;
        }
        return false;
    }

    public acpl_FM220_SDK FM220SDK;

    private Button Capture_No_Preview,Capture_PreView,Capture_BackGround;
    private TextView textMessage;
    private ImageView imageView;
    private static final String Telecom_Device_Key = "ACPLDEMO";

    //region USB intent and functions

    private UsbManager manager;
    private PendingIntent mPermissionIntent;
    private UsbDevice usb_Dev;
    private static final String ACTION_USB_PERMISSION = "com.ACPL.FM220_Telecom.USB_PERMISSION";

        public void scan(){
        FM220SDK.CaptureFM220(2,true,true);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                int pid, vid;
                pid = device.getProductId();
                vid = device.getVendorId();
                if ((pid == 0x8225 || pid == 0x8220)  && (vid == 0x0bca)) {
                    FM220SDK.stopCaptureFM220();
                    FM220SDK.unInitFM220();
                    usb_Dev=null;
                    textMessage.setText("FM220 disconnected");
                    DisableCapture();
                }
            }
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // call method to set up device communication
                            int pid, vid;
                            pid = device.getProductId();
                            vid = device.getVendorId();
                            if ((pid == 0x8225 || pid == 0x8220)  && (vid == 0x0bca)) {
                                fm220_Init_Result res =  FM220SDK.InitScannerFM220(manager,device,Telecom_Device_Key);
                                if (res.getResult()) {
                                    textMessage.setText("FM220 ready. "+res.getSerialNo());
                                    EnableCapture();
                                }
                                else {
                                    textMessage.setText("Error :-"+res.getError());
                                    DisableCapture();
                                }
                            }
                        }
                    } else {
                        textMessage.setText("User Blocked USB connection");
                        textMessage.setText("FM220 ready");
                        DisableCapture();
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null) {
                        // call method to set up device communication
                        int pid, vid;
                        pid = device.getProductId();
                        vid = device.getVendorId();
                        if ((pid == 0x8225)  && (vid == 0x0bca) && !FM220SDK.FM220isTelecom()) {
                            Toast.makeText(context,"Wrong device type application restart required!",Toast.LENGTH_LONG).show();
                            finish();
                        }
                        if ((pid == 0x8220)  && (vid == 0x0bca)&& FM220SDK.FM220isTelecom()) {
                            Toast.makeText(context,"Wrong device type application restart required!",Toast.LENGTH_LONG).show();
                            finish();
                        }

                        if ((pid == 0x8225 || pid == 0x8220) && (vid == 0x0bca)) {
                            if (!manager.hasPermission(device)) {
                                textMessage.setText("FM220 requesting permission");
                                manager.requestPermission(device, mPermissionIntent);
                            } else {
                                fm220_Init_Result res =  FM220SDK.InitScannerFM220(manager,device,Telecom_Device_Key);
                                if (res.getResult()) {
                                    textMessage.setText("FM220 ready. "+res.getSerialNo());
                                    EnableCapture();
                                }
                                else {
                                    textMessage.setText("Error :-"+res.getError());
                                    DisableCapture();
                                }
                            }
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        if (getIntent() != null) {
            return;
        }
        super.onNewIntent(intent);
        setIntent(intent);
        try {
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED) && usb_Dev==null) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    // call method to set up device communication & Check pid
                    int pid, vid;
                    pid = device.getProductId();
                    vid = device.getVendorId();
                    if ((pid == 0x8225)  && (vid == 0x0bca)) {
                        if (manager != null) {
                            if (!manager.hasPermission(device)) {
                                textMessage.setText("FM220 requesting permission");
                                manager.requestPermission(device, mPermissionIntent);
                            }
//                            else {
//                                fm220_Init_Result res =  FM220SDK.InitScannerFM220(manager,device,Telecom_Device_Key);
//                                if (res.getResult()) {
//                                    textMessage.setText("FM220 ready. "+res.getSerialNo());
//                                    EnableCapture();
//                                }
//                                else {
//                                    textMessage.setText("Error :-"+res.getError());
//                                    DisableCapture();
//                                }
//                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }



    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(mUsbReceiver);
            FM220SDK.unInitFM220();
        }  catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
    //endregion



    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        FM220SDK = new acpl_FM220_SDK(getApplicationContext(),this);
        textMessage = (TextView) findViewById(R.id.textMessage);
        Capture_PreView = (Button) findViewById(R.id.button2);
        Capture_No_Preview = (Button) findViewById(R.id.button);
        Capture_BackGround= (Button) findViewById(R.id.button3);
        imageView = (ImageView)  findViewById(R.id.imageView);

        //Region USB initialisation and Scanning for device
        SharedPreferences sp = getSharedPreferences("last_FM220_type", Activity.MODE_PRIVATE);
        boolean oldDevType = sp.getBoolean("FM220type", true);

        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final Intent piIntent = new Intent(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= 16) piIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        mPermissionIntent = PendingIntent.getBroadcast(getBaseContext(), 1, piIntent, 0);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(mUsbReceiver, filter);
        UsbDevice device = null;
        for ( UsbDevice mdevice : manager.getDeviceList().values()) {
            int pid, vid;
            pid = mdevice.getProductId();
            vid = mdevice.getVendorId();
            boolean devType;
            if ((pid == 0x8225) && (vid == 0x0bca)) {
                FM220SDK = new acpl_FM220_SDK(getApplicationContext(),this,true);
                devType=true;
            }
            else if ((pid == 0x8220) && (vid == 0x0bca)) {
                FM220SDK = new acpl_FM220_SDK(getApplicationContext(),this,false);
                devType=false;
            } else {
                FM220SDK = new acpl_FM220_SDK(getApplicationContext(),this,oldDevType);
                devType=oldDevType;
            }
            if (oldDevType != devType) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean("FM220type", devType);
                editor.commit();
            }
            if ((pid == 0x8225 || pid == 0x8220) && (vid == 0x0bca)) {
                device  = mdevice;
                if (!manager.hasPermission(device)) {
                    textMessage.setText("FM220 requesting permission");
                    manager.requestPermission(device, mPermissionIntent);
                } else {
                    Intent intent = this.getIntent();
                    if (intent != null) {
                        if (intent.getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
                            finishAffinity();
                        }
                    }
                    fm220_Init_Result res =  FM220SDK.InitScannerFM220(manager,device,Telecom_Device_Key);
                    if (res.getResult()) {
                        textMessage.setText("FM220 ready. "+res.getSerialNo());
                        EnableCapture();
                    }
                    else {
                        textMessage.setText("Error :-"+res.getError());
                        DisableCapture();
                    }
                }
                break;
            }
        }
        if (device == null) {
            textMessage.setText("Pl connect FM220");
            FM220SDK = new acpl_FM220_SDK(getApplicationContext(),this,oldDevType);
        }

        //endregion


        Capture_BackGround.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                DisableCapture();
                textMessage.setText("Pl wait..");
                imageView.setImageBitmap(null);
                FM220SDK.CaptureFM220(2);

            }
        });

        Capture_No_Preview.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                DisableCapture();
                FM220SDK.CaptureFM220(2,true,false);
            }
        });

        Capture_PreView.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                DisableCapture();
                FM220SDK.CaptureFM220(2,true,true);
            }
        });
    }

    private void DisableCapture() {
        Capture_BackGround.setEnabled(false);
        Capture_No_Preview.setEnabled(false);
        Capture_PreView.setEnabled(false);
        imageView.setImageBitmap(null);
    }
    private void EnableCapture() {
        Capture_BackGround.setEnabled(true);
        Capture_No_Preview.setEnabled(true);
        Capture_PreView.setEnabled(true);
    }
    @Override
    public void ScannerProgressFM220(final boolean DisplayImage,final Bitmap ScanImage,final boolean DisplayText,final String statusMessage) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (DisplayText) {
                    textMessage.setText(statusMessage);
                    textMessage.invalidate();
                }
                if (DisplayImage) {
                    imageView.setImageBitmap(ScanImage);
                    imageView.invalidate();
                }
            }
        });
    }

    @Override
    public void ScanCompleteFM220(final fm220_Capture_Result result) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FM220SDK.FM220Initialized())  EnableCapture();
                if (result.getResult()) {
                    imageView.setImageBitmap(result.getScanImage());
                    byte [] isotem  = result.getISO_Template();   // ISO TEMPLET of FingerPrint.....
//                    isotem is byte value of fingerprints
                    textMessage.setText("Success NFIQ:"+Integer.toString(result.getNFIQ())+"  SrNo:"+result.getSerialNo());
                } else {
                    imageView.setImageBitmap(null);
                    textMessage.setText(result.getError());
                }
                imageView.invalidate();
                textMessage.invalidate();
            }
        });
    
}
