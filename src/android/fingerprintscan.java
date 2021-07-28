package cordova.plugin.finerprintscan;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.acpl.access_computech_fm220_sdk.FM220_Scanner_Interface;
import com.acpl.access_computech_fm220_sdk.acpl_FM220_SDK;
import com.acpl.access_computech_fm220_sdk.fm220_Capture_Result;
import com.acpl.access_computech_fm220_sdk.fm220_Init_Result;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class fingerprintscan extends CordovaPlugin implements FM220_Scanner_Interface {
  public android.content.Context Context;
  public android.content.Context baseContext;
  public  UsbDevice device = null;
  private acpl_FM220_SDK FM220SDK;
  private static final String Telecom_Device_Key = "";
  public UsbManager manager;
  public PendingIntent mPermissionIntent;
  public UsbDevice usb_Dev;
  public static final String ACTION_USB_PERMISSION = "com.ACPL.FM220_Telecom.USB_PERMISSION";
  public Toast toast;
  private CallbackContext callbackContext = null;
  PluginResult pluginresult = null;
  public String base64;
  public  String temp1 ="";
  private byte[] t1, t2;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;
    if (action.equals("scanfinger")) {
      this.scanfinger(args, callbackContext);
      return true;
    }
    if (action.equals("startScanning")) {
      this.startScanning(args, callbackContext);
      return true;
    }
    if (action.equals("matchFingers")) {
     this.matchFingers(args.getJSONObject(0).getString("param1"));

      return true;
    }
    if (action.equals("registerDevice")) {
      this.registerDevice();

      return true;
    }

    return false;
  }

  public void scanfinger(JSONArray args, CallbackContext callbackContext) {
//    this.callbackContext = callbackContext;
//    cordova.setActivityResultCallback(this);
//    this.callbackContext.success(800);

    Log.d(fingerprintscan.class.getName() ,"scanfinger  "+args );
    try{
//      int p1 = Integer.parseInt("500");
////      callbackContext.success(p1);
//      PluginResult result  = new PluginResult(PluginResult.Status.OK);
//      PluginResult result = new PluginResult(PluginResult.Status.OK, 100  );
////    result.setKeepCallback(false);
//    if (callbackContext != null) {
//      callbackContext.sendPluginResult(result);
////      callbackContext = null;
//    }

    } catch(Exception e)
         {
           callbackContext.error("something wrong" + e);
     }


    }

  private final android.content.BroadcastReceiver mUsbReceiver = new android.content.BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
         device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        int pid, vid;
        pid = device.getProductId();
        vid = device.getVendorId();
        if ((pid == 0x8225 || pid == 0x8220) && (vid == 0x0bca)) {
          try {
            FM220SDK.stopCaptureFM220();
            FM220SDK.unInitFM220();
          }catch (Exception e){

          }
          usb_Dev = null;
          DisableCapture();
        }
      }
      if (ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this) {
           device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (intent.getBooleanExtra(
            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if (device != null) {
              // call method to set up device communication
              int pid, vid;
              pid = device.getProductId();
              vid = device.getVendorId();
              if ((pid == 0x8225 || pid == 0x8220) && (vid == 0x0bca)) {
                try {
                  fm220_Init_Result res = FM220SDK.InitScannerFM220(manager, device, Telecom_Device_Key);
                  if (res.getResult()) {
                    showToast("FM220 ready. " + res.getSerialNo());
                    EnableCapture();
                  } else {
                    showToast("Error! Try Again");
                    DisableCapture();
                  }
                }catch (Exception error){
//                  showToast("device does not exist or is restricted");
                }
              }
            }
          } else {
            showToast("User Blocked USB connection");
            DisableCapture();
          }
        }
      }
      if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
        synchronized (this) {
           device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (device != null) {
            // call method to set up device communication
            int pid, vid;
            pid = device.getProductId();
            vid = device.getVendorId();
            try {
              if ((pid == 0x8225) && (vid == 0x0bca) && !FM220SDK.FM220isTelecom()) {
                showToast( "Wrong device type application restart required!");
              }
            }catch (Exception e){
            }
            try {
              if ((pid == 0x8220) && (vid == 0x0bca) && FM220SDK.FM220isTelecom()) {
                showToast( "Wrong device type application restart required!");
              }
            }catch (Exception e){
            }


            if ((pid == 0x8225 || pid == 0x8220) && (vid == 0x0bca)) {
              try {
                if (!manager.hasPermission(device)) {
                  Log.d(fingerprintscan.class.getName(), "FM220 requesting permission");
                  manager.requestPermission(device, mPermissionIntent);
                } else {
                  fm220_Init_Result res = FM220SDK.InitScannerFM220(manager, device, Telecom_Device_Key);
                  if (res.getResult()) {
                    Log.d("FM220 ready. ", res.getSerialNo());
                    EnableCapture();
                  } else {
                    Log.d("Error :-", res.getError());
                    DisableCapture();
                  }
                }
              }
              catch (Exception error){
                showToast("Device does not exist or check OTG connection");
              }}
          }
        }
      }
    }
  };
  public void startScanning(JSONArray args, CallbackContext callback) {
    manager = (UsbManager) this.cordova.getActivity().getSystemService(android.content.Context.USB_SERVICE);
    final Intent piIntent = new Intent(ACTION_USB_PERMISSION);
    if (Build.VERSION.SDK_INT >= 16) piIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
    mPermissionIntent = PendingIntent.getBroadcast(this.cordova.getActivity().getBaseContext(), 1, piIntent, 0);
    for (UsbDevice mdevice : manager.getDeviceList().values()) {
      int pid, vid;
      pid = mdevice.getProductId();
      vid = mdevice.getVendorId();
      if ((pid == 0x8225 || pid == 0x8220) && (vid == 0x0bca)) {
        device = mdevice;
      }
    }
    try {
      if (!manager.hasPermission(device)) {
        manager.requestPermission(device, mPermissionIntent);
      }
        if (manager.hasPermission(device))
        {
        FM220SDK = new acpl_FM220_SDK(this.cordova.getActivity().getApplicationContext(), this, false);
        try {
          fm220_Init_Result res = FM220SDK.InitScannerFM220(manager, device, Telecom_Device_Key);
          if (res.getResult()) {
            Log.d("FM220 readyto use. ", res.getSerialNo());
            FM220SDK.CaptureFM220(2, true, true);
          }
        }catch (Exception e){
          this.showToast("Device disconnected");
        }
      } else {
        FM220SDK = new acpl_FM220_SDK(this.cordova.getActivity().getApplicationContext(), this, false);
        try{
        fm220_Init_Result res = FM220SDK.InitScannerFM220(manager, device, Telecom_Device_Key);
        if (res.getResult()) {
          Log.d("FM220 readyto use. ", res.getSerialNo());
            FM220SDK.CaptureFM220(2, true, true);
          }
        }
        catch (Exception error){
          showToast("device does not exist or is restricted");
        }

      }
    }catch (Exception e){
      showToast("Check Device or OTG Permission");
    }
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    this.cordova.getActivity().registerReceiver(mUsbReceiver, filter);

    PluginResult pluginresult = new PluginResult(PluginResult.Status.NO_RESULT);
    pluginresult.setKeepCallback(true);
  }
  private void DisableCapture() {

  }
  private void EnableCapture() {
  }

  @Override
  public void ScannerProgressFM220(final boolean DisplayImage, final Bitmap ScanImage, final boolean DisplayText, final String statusMessage) {
    showToast(statusMessage);
  }

  @Override
  public void ScanCompleteFM220(fm220_Capture_Result result) {

    if (result.getResult()) {
      showToast("Scan Successfull");
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      result.getScanImage().compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
      byte[] byteArray = byteArrayOutputStream.toByteArray();
      base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP);
      Log.d("base64image", base64);
      t1 = result.getISO_Template();
      String str = Base64.encodeToString(t1, Base64.DEFAULT);
//      this.matchFingers(str);
//      FM220SDK.MatchFM220(2, true, true, Base64.decode(str,Base64.DEFAULT));
      //      return to plugin as string
//      and convert to string to byte arr


      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject.put("base64", base64);
        jsonObject.put("t1", str);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      pluginresult = new PluginResult(PluginResult.Status.OK, jsonObject);
      pluginresult.setKeepCallback(true);
      if (callbackContext != null) {
        callbackContext.sendPluginResult(pluginresult);
        callbackContext = null;

      }


    }
    else {
      showToast("Scan Failed!! Try Again");
    }
  }


  @Override
  public void ScanMatchFM220(final fm220_Capture_Result result) {
    JSONObject jsonObject = new JSONObject();
    if (result.getResult()) {

      Log.d(fingerprintscan.class.getName() ,"result  "+"Finger matched" );
      showToast("Finger matched");

      try {
        jsonObject.put("status", true);

      } catch (JSONException e) {
        e.printStackTrace();
      }


    } else {
      Log.d(fingerprintscan.class.getName() ,"result  "+"Finger not matched" );
      showToast("Finger not  matched");
      try {
        jsonObject.put("status", false);

      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    pluginresult = new PluginResult(PluginResult.Status.OK, jsonObject);
    pluginresult.setKeepCallback(true);
    if (callbackContext != null) {
      callbackContext.sendPluginResult(pluginresult);
      callbackContext = null;

    }
  }

  public void onDestroy() {
    try {
      this.cordova.getActivity().unregisterReceiver(mUsbReceiver);
      FM220SDK.unInitFM220();
    } catch (Exception e) {
      e.printStackTrace();
    }
    super.onDestroy();
  }
  public void showToast(String message) {
    this.cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Context context = cordova.getActivity().getApplicationContext();

    if (toast != null) {
      toast.cancel();
    }
    toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
    toast.show();
  }
    });
  }
  private void matchFingers(String str ) {
    Log.d(fingerprintscan.class.getName() ,"str  "+str );
    manager = (UsbManager) this.cordova.getActivity().getSystemService(android.content.Context.USB_SERVICE);
    final Intent piIntent = new Intent(ACTION_USB_PERMISSION);
    if (Build.VERSION.SDK_INT >= 16) piIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
    mPermissionIntent = PendingIntent.getBroadcast(this.cordova.getActivity().getBaseContext(), 1, piIntent, 0);
    for (UsbDevice mdevice : manager.getDeviceList().values()) {
      int pid, vid;
      pid = mdevice.getProductId();
      vid = mdevice.getVendorId();
      if ((pid == 0x8225 || pid == 0x8220) && (vid == 0x0bca)) {
        device = mdevice;
      }
    }
    FM220SDK = new acpl_FM220_SDK(this.cordova.getActivity().getApplicationContext(), this, false);
    fm220_Init_Result res = FM220SDK.InitScannerFM220(manager, device, Telecom_Device_Key);
    if (!manager.hasPermission(device)) {
      Log.d("FM220 ready. ","FM220 requesting permission");
      manager.requestPermission(device, mPermissionIntent);

    }
    FM220SDK.MatchFM220(2, true, true, Base64.decode(str,Base64.DEFAULT));
  }
  public void registerDevice() {
    manager = (UsbManager) this.cordova.getActivity().getSystemService(android.content.Context.USB_SERVICE);
    final Intent piIntent = new Intent(ACTION_USB_PERMISSION);
    if (Build.VERSION.SDK_INT >= 16) piIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
    mPermissionIntent = PendingIntent.getBroadcast(this.cordova.getActivity().getBaseContext(), 1, piIntent, 0);
    for (UsbDevice mdevice : manager.getDeviceList().values()) {
      int pid, vid;
      pid = mdevice.getProductId();
      vid = mdevice.getVendorId();
      if ((pid == 0x8225 || pid == 0x8220) && (vid == 0x0bca)) {
        device = mdevice;
      }
    }
    try {
      if (!manager.hasPermission(device)) {
        manager.requestPermission(device, mPermissionIntent);
      }
    }
      catch (Exception error){
        showToast("device does not exist or is restricted");
      }
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    this.cordova.getActivity().registerReceiver(mUsbReceiver, filter);
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("status", true);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    pluginresult = new PluginResult(PluginResult.Status.OK, jsonObject);
    pluginresult.setKeepCallback(true);
      callbackContext.sendPluginResult(pluginresult);
      callbackContext = null;


    }
}
