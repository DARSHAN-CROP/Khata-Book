package com.darshan.khata;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Daily 9 AM notice when someone's due date (udhar) has come. */
public class Reminders extends BroadcastReceiver {
    static final String CH = "dues";

    static long endOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59);
        return c.getTimeInMillis();
    }

    /** Number of unpaid "gave" items whose due date is today or earlier, for people who still owe. */
    static int dueCount(Store st) {
        JSONArray ppl = st.arr("people");
        long end = endOfToday();
        int n = 0;
        for (int i = 0; i < ppl.length(); i++) {
            JSONObject p = ppl.optJSONObject(i);
            if (MainActivity.balanceOf(p) <= 0) continue;
            JSONArray tx = p.optJSONArray("tx");
            if (tx == null) continue;
            for (int k = 0; k < tx.length(); k++) {
                JSONObject t = tx.optJSONObject(k);
                long due = t.optLong("due", 0);
                if (t.optString("type").equals("gave") && due > 0 && due <= end) n++;
            }
        }
        return n;
    }

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, Reminders.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 9, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, 9); t.set(Calendar.MINUTE, 0); t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis()) t.add(Calendar.DAY_OF_MONTH, 1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    @Override
    public void onReceive(Context c, Intent in) {
        schedule(c);
        if ("android.intent.action.BOOT_COMPLETED".equals(in.getAction()) || "android.intent.action.MY_PACKAGE_REPLACED".equals(in.getAction())) return;
        Store st = new Store(c);
        L.gu = st.lang().equals("gu");
        int n = dueCount(st);
        if (n == 0) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(CH, "Dues", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(c, MainActivity.class).putExtra("tab", 1);
        PendingIntent pi = PendingIntent.getActivity(c, 10, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(L.t("app"))
                .setContentText(String.format(Locale.US, L.t("notif_text"), n)).setContentIntent(pi).setAutoCancel(true);
        nm.notify(77, b.build());
    }
}
