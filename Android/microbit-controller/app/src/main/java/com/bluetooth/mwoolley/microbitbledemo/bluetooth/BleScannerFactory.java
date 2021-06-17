package com.bluetooth.mwoolley.microbitbledemo.bluetooth;

import android.content.Context;
import android.os.Build;

public class BleScannerFactory {

    public static BleScanner getBleScanner(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new BleScannerPostAndroid4(context);
        } else {
            return new BleScannerAndroid4(context);
        }
    }

}