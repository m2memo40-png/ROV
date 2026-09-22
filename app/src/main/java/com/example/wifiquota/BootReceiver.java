package com.example.wifiquota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences p = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean("monitor", false)) return;
        Intent svc = new Intent(context, QuotaMonitorService.class);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(svc); else context.startService(svc);
    }
}
