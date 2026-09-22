package com.example.wifiquota;

import android.content.Intent;
import android.net.VpnService;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

public class QuotaVpnService extends VpnService {
    private static volatile QuotaVpnService instance;
    private ParcelFileDescriptor vpnInterface;
    public static boolean isRunning() { return instance != null; }

    @Override public void onCreate() { super.onCreate(); instance = this; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            Builder b = new Builder()
                    .setSession("WiFi Quota Block")
                    .setBlocking(true)
                    .addAddress("10.255.0.2", 32)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("::", 0);
            if (vpnInterface != null) try { vpnInterface.close(); } catch (Exception ignored) {}
            vpnInterface = b.establish();
        } catch (Exception ignored) { }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (vpnInterface != null) { try { vpnInterface.close(); } catch (Exception ignored) {} vpnInterface = null; }
        instance = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return super.onBind(intent); }
}
