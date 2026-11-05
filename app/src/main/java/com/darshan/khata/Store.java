package com.darshan.khata;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Everything is kept on the phone (offline) as one small JSON text in SharedPreferences. */
final class Store {
    private final SharedPreferences sp;
    private JSONObject d;
    private final Context ctx;

    Store(Context c) {
        ctx = c.getApplicationContext();
        sp = c.getSharedPreferences("data_store", Context.MODE_PRIVATE);
        try { d = new JSONObject(sp.getString("data", "{}")); } catch (JSONException e) { d = new JSONObject(); }
    }

    Context context() { return ctx; }
    double updated() { return d.optDouble("updated", 0); }

    /** Write without marking a new change (used by sync and local-only data). */
    void saveRaw() { sp.edit().putString("data", d.toString()).apply(); }

    /** A real change by the user: remember when, then sync to the Google account in a few seconds. */
    void save() {
        try { d.put("updated", (double) System.currentTimeMillis()); } catch (JSONException ignored) { }
        saveRaw();
        Sync.poke(ctx);
    }

    void put(String k, Object v) { try { d.put(k, v); } catch (JSONException ignored) { } }

    JSONArray arr(String k) {
        JSONArray a = d.optJSONArray(k);
        if (a == null) {
            a = new JSONArray();
            try { d.put(k, a); } catch (JSONException ignored) { }
        }
        return a;
    }

    String lang() { return d.optString("lang", "gu"); }

    void setLang(String l) {
        try { d.put("lang", l); } catch (JSONException ignored) { }
        saveRaw();
    }

    private static String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    int sessionsToday() {
        JSONObject s = d.optJSONObject("sessions");
        return s == null ? 0 : s.optInt(today(), 0);
    }

    void addSession() {
        try {
            JSONObject s = d.optJSONObject("sessions");
            if (s == null) { s = new JSONObject(); d.put("sessions", s); }
            s.put(today(), s.optInt(today(), 0) + 1);
        } catch (JSONException ignored) { }
        saveRaw();
    }
}
