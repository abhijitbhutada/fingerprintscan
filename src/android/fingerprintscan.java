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

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;
    if (action.equals("scanfinger")) {
      this.scanfinger(args, callbackContext);
      return true;
    }
    if (action.equals("initialise")) {
      this.initialise(args, callbackContext);
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
//    callbackContext.success("return from plugin" + 100);
//      try{
////      int p1 = Integer.parseInt("100");
////      callback.success(p1);
////    callback.success("return from plugin");
////    callback.sendPluginResult(new PluginResult(PluginResult.Status.OK, 100));
//
//         } catch(Exception e)
//         {
//             callback.error("something wrong" + e);
//     }

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
          FM220SDK.stopCaptureFM220();
          FM220SDK.unInitFM220();
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
                fm220_Init_Result res = FM220SDK.InitScannerFM220(manager, device, Telecom_Device_Key);
                if (res.getResult()) {
//                  textMessage.setText("FM220 ready. " + res.getSerialNo());
                  EnableCapture();
                } else {
//                  textMessage.setText("Error :-" + res.getError());
                  DisableCapture();
                }
              }
            }
          } else {
//            textMessage.setText("User Blocked USB connection");
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
            if ((pid == 0x8225) && (vid == 0x0bca) && !FM220SDK.FM220isTelecom()) {
//              Toast.makeText(context, "Wrong device type application restart required!", Toast.LENGTH_LONG).show();
            }
            if ((pid == 0x8220) && (vid == 0x0bca) && FM220SDK.FM220isTelecom()) {
//              Toast.makeText(context, "Wrong device type application restart required!", Toast.LENGTH_LONG).show();
            }

            if ((pid == 0x8225 || pid == 0x8220) && (vid == 0x0bca)) {
              if (!manager.hasPermission(device)) {
                Log.d(fingerprintscan.class.getName() ,"FM220 requesting permission" );
                manager.requestPermission(device, mPermissionIntent);
              } else {
                fm220_Init_Result res = FM220SDK.InitScannerFM220(manager, device, Telecom_Device_Key);
                if (res.getResult()) {
                  Log.d("FM220 ready. " , res.getSerialNo());
                  EnableCapture();
                } else {
                  Log.d("Error :-" , res.getError());
                  DisableCapture();
                }
              }
            }
          }
        }
      }
    }
  };
  public void initialise(JSONArray args, CallbackContext callback) {
    Log.d("initialise", "initialise");

    manager = (UsbManager) this.cordova.getActivity().getSystemService(Context.USB_SERVICE);
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
    if (!manager.hasPermission(device)) {
      Log.d("FM220 ready. ","FM220 requesting permission");
      manager.requestPermission(device, mPermissionIntent);

      FM220SDK = new acpl_FM220_SDK(this.cordova.getActivity().getApplicationContext(), this, false);
      fm220_Init_Result res = FM220SDK.InitScannerFM220(manager, device, Telecom_Device_Key);
      if (res.getResult()) {
        Log.d("FM220 readyto use. ", res.getSerialNo());
        FM220SDK.CaptureFM220(2, true, true);
      }
    } else {
      FM220SDK = new acpl_FM220_SDK(this.cordova.getActivity().getApplicationContext(), this, false);
      fm220_Init_Result res = FM220SDK.InitScannerFM220(manager, device, Telecom_Device_Key);
      if (res.getResult()) {
        Log.d("FM220 readyto use. ", res.getSerialNo());
        FM220SDK.CaptureFM220(2, true, true);
      }

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
      String base64;
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      result.getScanImage().compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
      byte[] byteArray = byteArrayOutputStream.toByteArray();
      base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP);
      Log.d("base63image", base64);

      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject.put("base64", base64);
      } catch (JSONException e) {
        e.printStackTrace();
      }
//      Log.d("jsonObject", jsonObject.toString());
      pluginresult = new PluginResult(PluginResult.Status.OK, jsonObject);
      pluginresult.setKeepCallback(true);
      if (callbackContext != null) {
        callbackContext.sendPluginResult(pluginresult);
//no more result , hence the context is cleared.
        callbackContext = null;
//      FM219SDK.unInitFM220();
//      int p1 = Integer.parseInt("100");
//      callback.success(p1);
      }
//  }
//};

    }
    else {
      showToast("Scan Failed!! Try Again");
    }
  }

  public String convertToBase64String(Bitmap bitmap) {
    String base64;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
    byte[] byteArray = byteArrayOutputStream.toByteArray();
    base64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
    return base64;
  }

  @Override
  public void ScanMatchFM220(fm220_Capture_Result fm220_capture_result) {

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
}
