package com.example.wifiquota;

import android.app.*;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.*;
import android.net.ConnectivityManager;
import android.os.*;
import android.provider.Settings;

import java.util.Locale;

public class QuotaMonitorService extends Service {
    private static final int NOTIF_ID = 101;
    private static final long INTERVAL_MS = 30_000L;
    private Handler handler;
    private Runnable ticker;

    private android.content.SharedPreferences prefs() { return getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE); }

    @Override public void onCreate() {
        super.onCreate();
        MainActivity.ensureChannel(this);
        startForeground(NOTIF_ID, buildNotification("جاري مراقبة استهلاك Wi‑Fi"));
        handler = new Handler(Looper.getMainLooper());
        ticker = () -> { tick(); handler.postDelayed(ticker, INTERVAL_MS); };
        handler.post(ticker);
    }

    private Notification buildNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, MainActivity.CHANNEL_ID)
                .setContentTitle("مدير باقة Wi‑Fi")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void tick() {
        android.content.SharedPreferences p = prefs();
        if (!p.getBoolean("monitor", false)) { stopSelf(); return; }
        long now = System.currentTimeMillis();
        long start = p.getLong("startMillis", now);
        int duration = Math.max(1, p.getInt("durationDays", 30));
        if (now - start >= duration * 86400000L) {
            p.edit().putLong("startMillis", now).putBoolean("t30", false).putBoolean("t60", false).putBoolean("t90", false).putBoolean("blocked", false).apply();
            stopVpn();
            start = now;
        }

        long used = queryWifiBytes(start, now);
        double quotaGb = p.getFloat("quotaGb", 5f);
        long quotaBytes = (long)(quotaGb * 1024d * 1024d * 1024d);
        int pct = quotaBytes <= 0 ? 0 : (int)Math.min(100, Math.round(used * 100d / quotaBytes));
        String pctText = String.format(Locale.US, "%d%%", pct);

        if (pct >= 30 && !p.getBoolean("t30", false)) { notifyThreshold(30, used, quotaGb); p.edit().putBoolean("t30", true).apply(); }
        if (pct >= 60 && !p.getBoolean("t60", false)) { notifyThreshold(60, used, quotaGb); p.edit().putBoolean("t60", true).apply(); }
        if (pct >= 90 && !p.getBoolean("t90", false)) { notifyThreshold(90, used, quotaGb); p.edit().putBoolean("t90", true).apply(); }

        if (used >= quotaBytes && quotaBytes > 0) {
            if (!p.getBoolean("blocked", false)) p.edit().putBoolean("blocked", true).apply();
            ensureVpnBlock();
        } else if (p.getBoolean("blocked", false)) {
            p.edit().putBoolean("blocked", false).apply();
            stopVpn();
        }

        getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification("استهلاك الباقة: " + formatBytes(used) + " / " + String.format(Locale.US, "%.2f GB", quotaGb) + " — " + pctText));
        Intent up = new Intent(MainActivity.ACTION_UPDATE); up.setPackage(getPackageName()); up.putExtra("used", used).putExtra("quota", quotaBytes).putExtra("pct", pct).putExtra("blocked", p.getBoolean("blocked", false)); sendBroadcast(up);
    }

    private long queryWifiBytes(long start, long end) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return 0L;
            if (getPackageManager().checkPermission("android.permission.PACKAGE_USAGE_STATS", getPackageName()) != android.content.pm.PackageManager.PERMISSION_GRANTED) return 0L;
            NetworkStatsManager nsm = (NetworkStatsManager)getSystemService(NETWORK_STATS_SERVICE);
            NetworkStats.Bucket b = nsm.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, "", start, end);
            return b == null ? 0L : b.getRxBytes() + b.getTxBytes();
        } catch (Throwable e) { return 0L; }
    }

    private void ensureVpnBlock() {
        try {
            Intent prep = android.net.VpnService.prepare(this);
            if (prep != null) return; // MainActivity must grant VPN once.
            if (!QuotaVpnService.isRunning()) startService(new Intent(this, QuotaVpnService.class));
        } catch (Throwable ignored) { }
    }

    private void stopVpn() {
        try { stopService(new Intent(this, QuotaVpnService.class).setAction("STOP")); } catch (Throwable ignored) { }
    }

    private void notifyThreshold(int threshold, long used, double quotaGb) {
        String txt = "وصل الاستهلاك إلى " + threshold + "% — " + formatBytes(used) + " من " + String.format(Locale.US, "%.2f GB", quotaGb);
        Notification n = new Notification.Builder(this, MainActivity.CHANNEL_ID).setContentTitle("تنبيه الباقة").setContentText(txt).setSmallIcon(android.R.drawable.stat_notify_more).setAutoCancel(true).build();
        getSystemService(NotificationManager.class).notify(200 + threshold, n);
    }

    public static String formatBytes(long bytes) {
        double gb = bytes / 1073741824d;
        return String.format(Locale.US, "%.2f GB", gb);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public void onDestroy() { if (handler != null) handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
