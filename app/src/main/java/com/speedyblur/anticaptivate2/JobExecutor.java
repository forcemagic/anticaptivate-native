package com.speedyblur.anticaptivate2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JobExecutor extends BroadcastReceiver {

    final int NOTIF_ID = 0;
    final int NOTIF_ERR_ID = 1;
    final String LTAG = "GuestVendegLoginService";
    final String CHANNEL_ID = "general";
    final String CAPTIVE_URL = "http://connectivitycheck.gstatic.com/generate_204";
    final String LOGIN_URL = "http://suliwifi-1.wificloud.ahrt.hu/login.html?redirect=redirect";
    final String PASSWORD = "3DK4KLFL";

    OkHttpClient htcli;
    NotificationManagerCompat notifMan;

    private boolean isCaptive() {
        Request req = new Request.Builder().url(CAPTIVE_URL).build();
        try {
            int status = htcli.newCall(req).execute().code();
            return status == 204;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean sendLoginRequest() {
        Request req = new Request.Builder()
                .url(LOGIN_URL)
                .addHeader("User-Agent", "Mozilla/5.0 (compatible) GuestVendegLoginService/0.1")
                .post(new FormBody.Builder()
                        .add("username", "diakhalo")
                        .add("password", PASSWORD)
                        .add("err_flag", "")            // Just to be on the safe side, I've left these params in
                        .add("buttonClicked", "4")
                        .add("err_msg", "")
                        .add("info_flag", "")
                        .add("info_msg", "")
                        .add("redirect_url", "http://kifu.gov.hu/")
                        .build())
                .build();
        try {
            Response resp = htcli.newCall(req).execute();

            // Let's just hope that KIFU doesn't use JS redirects
            // TODO: Check wrong password response
            return resp.isRedirect();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showErrorNotif(Context con) {
        if (notifMan == null) {
            Log.e(LTAG, "UNABLE TO SHOW ERROR NOTIFICATION! THIS IS VERY WRONG! REEEEE");
        }
        NotificationCompat.Builder errNotif = new NotificationCompat.Builder(con, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_error)
                .setContentTitle("GUEST_VENDEG Auto-Login Service")
                .setContentText("Something bad happened. Click here to open the app for more info.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(Color.RED)
                .setContentIntent(PendingIntent.getActivity(con, 0, new Intent(con, MainActivity.class), 0));
        notifMan.notify(NOTIF_ERR_ID, errNotif.build());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d(LTAG, "Job starting...");

            // Setting up globals
            htcli = new OkHttpClient.Builder().followRedirects(false).build();
            notifMan = NotificationManagerCompat.from(context);

            // Getting Wifi service
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            // Is Wifi even enabled?!
            if (!wm.isWifiEnabled()) {
                Log.d(LTAG, "Wifi not enabled, exiting.");
                return;
            }

            // Is the SSID the one we want?
            String currSSID = wm.getConnectionInfo().getSSID();
            if (!currSSID.equals("\"GUEST_VENDEG\"")) {
                Log.d(LTAG, "SSID not GUEST_VENDEG (" + currSSID + "), exiting.");
                return;
            }

            // Forking to background (needed for networking)
            Log.d(LTAG, "Forking to background thread");
            new Thread(() -> {
                try {
                    if (!isCaptive()) {
                        Log.d(LTAG, "Network seems fine, no captive portal detected.");
                        return;
                    }

                    NotificationCompat.Builder notif = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("GUEST_VENDEG Auto-Login Service")
                            .setContentText("You are connected to the right Wifi! Please wait, logging you in automagically...")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                    notifMan.notify(NOTIF_ID, notif.build());

                    Log.d(LTAG, "Sending login request now...");
                    if (sendLoginRequest()) {
                        Log.d(LTAG, "Success!");
                        notif.setContentText("Login successful! You should have Internet now.")
                                .setSmallIcon(R.drawable.notification_icon_done)
                                .setTimeoutAfter(10000L);
                    } else {
                        Log.d(LTAG, "Login unsuccessful (wrong credentials?)");
                        notif.setContentText("Something went wrong. I'm terribly sorry 😔")
                                .setSmallIcon(R.drawable.notification_icon_error)
                                .setTimeoutAfter(10000L);
                    }
                    notifMan.notify(NOTIF_ID, notif.build());
                } catch (Exception e) {
                    Log.e(LTAG, "Something horrible happened while communicating with the captive portal!");
                    showErrorNotif(context);
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            Log.e(LTAG, "Something horrible happened while checking network conditions!");
            showErrorNotif(context);
            e.printStackTrace();
        }
    }

    public static void register(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, JobExecutor.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 80000, pi);
    }
}
