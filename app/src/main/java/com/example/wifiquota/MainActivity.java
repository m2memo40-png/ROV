package com.example.wifiquota;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.graphics.Color;
import android.net.VpnService;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import android.content.pm.PackageManager;

import java.util.Locale;

public class MainActivity extends Activity {
    public static final String PREFS = "quota_prefs";
    public static final String CHANNEL_ID = "quota_channel";
    public static final String ACTION_UPDATE = "com.example.wifiquota.UPDATE";
    private static final int VPN_REQ = 7001;

    private EditText quotaGb, durationDays, adminPin;
    private TextView usageText, percentText, statusText, ownerText, infoText;
    private ProgressBar progress;
    private BroadcastReceiver receiver;

    public static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null && Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID, c.getString(com.example.wifiquota.R.string.channel_name), NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 9001);
        }
        quotaGb = findViewById(R.id.quotaGb); durationDays = findViewById(R.id.durationDays); adminPin = findViewById(R.id.adminPin);
        usageText = findViewById(R.id.usageText); percentText = findViewById(R.id.percentText); statusText = findViewById(R.id.statusText); ownerText = findViewById(R.id.ownerText); infoText = findViewById(R.id.infoText); progress = findViewById(R.id.progress);
        loadPrefs(); updateOwner();

        findViewById(R.id.saveBtn).setOnClickListener(v -> saveNewPlan());
        findViewById(R.id.usageAccessBtn).setOnClickListener(v -> { try { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); } catch (Exception ignored) {} });
        findViewById(R.id.vpnBtn).setOnClickListener(v -> requestVpn());
        findViewById(R.id.startBtn).setOnClickListener(v -> startMonitor());
        findViewById(R.id.stopBtn).setOnClickListener(v -> stopMonitor());

        receiver = new BroadcastReceiver() { @Override public void onReceive(Context c, Intent i) { if (ACTION_UPDATE.equals(i.getAction())) render(i.getLongExtra("used",0), i.getLongExtra("quota",0), i.getIntExtra("pct",0), i.getBooleanExtra("blocked", false)); } };
        registerReceiver(receiver, new IntentFilter(ACTION_UPDATE), Context.RECEIVER_NOT_EXPORTED);
    }

    private void loadPrefs() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        quotaGb.setText(String.valueOf(p.getFloat("quotaGb", 5f)));
        durationDays.setText(String.valueOf(p.getInt("durationDays", 30)));
        adminPin.setText(p.getString("pin", "1234"));
    }

    private boolean pinOk() {
        String pin = adminPin.getText().toString().trim();
        if (pin.length() < 4) { Toast.makeText(this, "PIN يجب أن يكون 4 أرقام على الأقل", Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }

    private void saveNewPlan() {
        if (!pinOk()) return;
        float gb; int days;
        try { gb = Float.parseFloat(quotaGb.getText().toString().trim()); days = Integer.parseInt(durationDays.getText().toString().trim()); } catch (Exception e) { Toast.makeText(this, "أدخل أرقام صحيحة", Toast.LENGTH_SHORT).show(); return; }
        if (gb <= 0 || days <= 0) { Toast.makeText(this, "الباقة والمدة يجب أن تكونا أكبر من صفر", Toast.LENGTH_SHORT).show(); return; }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putFloat("quotaGb", gb).putInt("durationDays", days).putString("pin", adminPin.getText().toString().trim()).putLong("startMillis", System.currentTimeMillis()).putBoolean("t30",false).putBoolean("t60",false).putBoolean("t90",false).putBoolean("blocked",false).putBoolean("monitor",true).apply();
        stopService(new Intent(this, QuotaVpnService.class)); startMonitor(); Toast.makeText(this,"تم بدء باقة جديدة",Toast.LENGTH_SHORT).show();
    }

    private void startMonitor() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("monitor", true).apply();
        Intent i = new Intent(this, QuotaMonitorService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        Toast.makeText(this,"المراقبة تعمل الآن",Toast.LENGTH_SHORT).show();
    }

    private void stopMonitor() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("monitor", false).apply();
        stopService(new Intent(this, QuotaMonitorService.class)); stopService(new Intent(this, QuotaVpnService.class));
        Toast.makeText(this,"تم إيقاف المراقبة",Toast.LENGTH_SHORT).show();
    }

    private void requestVpn() {
        Intent prep = VpnService.prepare(this);
        if (prep != null) startActivityForResult(prep, VPN_REQ);
        else { Toast.makeText(this,"صلاحية VPN مفعلة بالفعل",Toast.LENGTH_SHORT).show(); startMonitor(); }
    }

    private void updateOwner() {
        DevicePolicyManager dpm = (DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);
        boolean owner = dpm != null && dpm.isDeviceOwnerApp(getPackageName());
        ownerText.setText("Device Owner: " + (owner ? "مفعل ✅" : "غير مفعل"));
        ownerText.setTextColor(Color.parseColor(owner ? "#2E7D32" : "#C62828"));
    }

    private void render(long used, long quota, int pct, boolean blocked) {
        usageText.setText("الاستخدام: " + QuotaMonitorService.formatBytes(used) + " / " + QuotaMonitorService.formatBytes(quota));
        percentText.setText("النسبة: " + pct + "%");
        progress.setProgress(Math.min(100,pct));
        statusText.setText(blocked ? "الحالة: محظور ⛔" : "الحالة: يعمل ✅");
        statusText.setTextColor(Color.parseColor(blocked ? "#C62828" : "#2E7D32"));
        infoText.setText("آخر تحديث " + new java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(new java.util.Date()));
    }

    @Override protected void onDestroy() { if (receiver != null) unregisterReceiver(receiver); super.onDestroy(); }
    @Override protected void onResume() { super.onResume(); updateOwner(); }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) { super.onActivityResult(requestCode,resultCode,data); if (requestCode==VPN_REQ && resultCode==RESULT_OK) { Toast.makeText(this,"تمت الموافقة على VPN — يمكن للحظر التلقائي العمل",Toast.LENGTH_LONG).show(); startMonitor(); } }
}
