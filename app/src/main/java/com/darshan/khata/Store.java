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

    Store(Context c) {
        sp = c.getSharedPreferences("data_store", Context.MODE_PRIVATE);
        try { d = new JSONObject(sp.getString("data", "{}")); } catch (JSONException e) { d = new JSONObject(); }
    }

    void save() { sp.edit().putString("data", d.toString()).apply(); }

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
        save();
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
        save();
    }
}
