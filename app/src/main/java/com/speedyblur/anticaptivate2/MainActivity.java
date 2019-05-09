package com.speedyblur.anticaptivate2;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    final String LTAG = "GuestVendegLoginFront";
    boolean isFirstStart = true;
    Process proc;
    LogcatAdapter la;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Activity setup
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            setContentView(R.layout.first_start);
        } else {
            isFirstStart = false;
            setContentView(R.layout.activity_main);
            setup();
        }
    }

    @Override
    protected void onDestroy() {
        // Kill logcat
        proc.destroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.d(LTAG, "Permission granted. Yay \\o/");
                setContentView(R.layout.activity_main);
                setup();
            } else {
                Log.w(LTAG, "Permission denied. :/ Not setting up stuff.");
                new AlertDialog.Builder(this)
                        .setTitle("Információ")
                        .setMessage("Nem engedélyezte a helymeghatározási jogosultságot. A program most kilép.")
                        .setPositiveButton("Rendben", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        } // else ignore everything
    }

    public void doRequestPermission(View v) {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, 0);
    }

    /**
     * This guy starts Logcat, and appends every new line
     * to the LogcatAdapter's dataset.
     *
     * NEEDS TO BE RUN IN A SEPARATE THREAD!
     * (otherwise it'll just hang the app)
     *
     * @see LogcatAdapter
     */
    private void watchLogcat() {
        try {
            proc = Runtime.getRuntime().exec("logcat -v time "+LTAG+":V GuestVendegLoginService:V *:S");
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            String line;
            while ((line = br.readLine()) != null) {
                String finalLine = line;
                runOnUiThread(() -> {
                    la.data.add(0, finalLine);
                    la.notifyDataSetChanged();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Setup the
     * - UI
     * - NotificationChannel for service notifications
     * - JobExecutor (BroadcastReceiver triggered by AlarmManager)
     *
     * Automatically catches any errors in the process :)
     *
     * @see JobExecutor
     */
    private void setup() {
        try {
            // Logcat list setup
            RecyclerView rv = findViewById(R.id.logcatList);
            RecyclerView.LayoutManager lm = new LinearLayoutManager(this);
            rv.setLayoutManager(lm);
            la = new LogcatAdapter();
            rv.setAdapter(la);

            // Defer watchLogcat()
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    watchLogcat();
                }
            }, 200L);

            // Setup notification channel
            Log.d(LTAG, "Setting up the notification channel...");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel nCh = new NotificationChannel("general", "General", NotificationManager.IMPORTANCE_DEFAULT);
                nCh.setDescription("Shows notification when auto-login happens");
                NotificationManager nMan = getSystemService(NotificationManager.class);
                nMan.createNotificationChannel(nCh);
            }

            // Schedule job
            JobExecutor.register(this);
            Log.d(LTAG, "Scheduled job successfully.");
        } catch (Exception e) {
            Log.e(LTAG, "Setting up something failed! PLEASE REPORT THIS!");
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.forceReloadBtn) {
            sendBroadcast(new Intent(this, JobExecutor.class));
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isFirstStart) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }
}
