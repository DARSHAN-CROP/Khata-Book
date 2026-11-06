package com.darshan.khata;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Google account sync with the web app, using Firebase over plain HTTPS (no extra libraries). */
final class Sync {
    private Sync() {}

    static final String API_KEY = "AIzaSyASuS7s9YNWW3fDjaV5NAitiSBpumxs46c";
    static final String PROJECT = "student-planner-darshan";
    static final String LOGIN_URL = "https://1darshankumarsavsani-blip.github.io/Khata-Book/app-auth.html";
    static final String DB = "projects/" + PROJECT + "/databases/(default)/documents";

    static Runnable onChanged;
    private static final Handler UI = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();

    static SharedPreferences sp(Context c) { return c.getSharedPreferences("sync", Context.MODE_PRIVATE); }
    static boolean signedIn(Context c) { return !sp(c).getString("refresh", "").isEmpty(); }
    static String email(Context c) { return sp(c).getString("email", ""); }
    static String status(Context c) { return sp(c).getString("status", ""); }
    static void signOut(Context c) { sp(c).edit().clear().apply(); }
    static void startLogin(Activity a) { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LOGIN_URL))); }

    static boolean handleIntent(Context c, Intent in) {
        Uri u = in == null ? null : in.getData();
        if (u == null || !"darshankhata".equals(u.getScheme())) return false;
        String r = u.getQueryParameter("refresh"), uid = u.getQueryParameter("uid");
        if (r == null || r.isEmpty() || uid == null || uid.isEmpty()) return false;
        String em = u.getQueryParameter("email");
        sp(c).edit().putString("refresh", r).putString("uid", uid).putString("email", em == null ? "" : em)
                .putBoolean("first", true).putString("status", "").apply();
        return true;
    }

    static void poke(Context c) {
        if (!signedIn(c)) return;
        final Context app = c.getApplicationContext();
        UI.removeCallbacksAndMessages(null);
        UI.postDelayed(() -> syncNow(app), 3000);
    }

    static void syncNow(final Context app) {
        if (!signedIn(app)) return;
        new Thread(() -> {
            String st;
            synchronized (LOCK) {
                try {
                    run(app);
                    st = "OK " + new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
                } catch (Exception e) {
                    st = "Error: " + e.getMessage();
                }
            }
            sp(app).edit().putString("status", st).apply();
            UI.post(() -> { if (onChanged != null) onChanged.run(); });
        }).start();
    }

    private static void run(Context app) throws Exception {
        SharedPreferences p = sp(app);
        JSONObject tok = new JSONObject(http("POST", "https://securetoken.googleapis.com/v1/token?key=" + API_KEY,
                "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(p.getString("refresh", ""), "UTF-8"),
                "application/x-www-form-urlencoded", null, false));
        String idToken = tok.getString("id_token");
        String uid = tok.optString("user_id", p.getString("uid", ""));
        p.edit().putString("refresh", tok.optString("refresh_token", p.getString("refresh", ""))).putString("uid", uid).apply();
        String name = DB + "/planner/" + uid;
        JSONObject remote = null;
        String body = http("GET", "https://firestore.googleapis.com/v1/" + name, null, null, idToken, true);
        if (body != null) {
            JSONObject f = new JSONObject(body).optJSONObject("fields");
            if (f != null && f.optJSONObject("khata") != null) remote = new JSONObject(f.getJSONObject("khata").getString("stringValue"));
        }
        Store s = new Store(app);
        JSONObject local = toWeb(s);
        double lu = local.optDouble("updated", 0), ru = remote == null ? -1 : remote.optDouble("updated", 0);
        boolean first = p.getBoolean("first", false);
        if (remote != null && first) {
            JSONObject merged = merge(local, remote);
            merged.put("updated", (double) System.currentTimeMillis());
            fromWeb(merged, s);
            push(name, merged, idToken);
        } else if (remote != null && ru > lu) {
            fromWeb(remote, s);
        } else if (remote == null || ru < lu) {
            push(name, local, idToken);
        }
        p.edit().putBoolean("first", false).apply();
    }

    /** Writes only the "khata" field (the planner's own field in the same document stays untouched). */
    private static void push(String name, JSONObject data, String idToken) throws Exception {
        JSONObject upd = new JSONObject().put("name", name)
                .put("fields", new JSONObject().put("khata", new JSONObject().put("stringValue", data.toString())));
        JSONObject w = new JSONObject().put("update", upd).put("updateMask", new JSONObject().put("fieldPaths", new JSONArray().put("khata")));
        JSONObject req = new JSONObject().put("writes", new JSONArray().put(w));
        http("POST", "https://firestore.googleapis.com/v1/" + DB + ":commit", req.toString(), "application/json", idToken, false);
    }

    private static String http(String method, String url, String body, String type, String bearer, boolean nullOn404) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestMethod(method);
        if (bearer != null) c.setRequestProperty("Authorization", "Bearer " + bearer);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", type);
            try (OutputStream o = c.getOutputStream()) { o.write(body.getBytes("UTF-8")); }
        }
        int code = c.getResponseCode();
        if (code == 404 && nullOn404) return null;
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        if (in != null) { byte[] b = new byte[4096]; int n; while ((n = in.read(b)) > 0) bo.write(b, 0, n); }
        String out = bo.toString("UTF-8");
        if (code >= 400) throw new Exception("HTTP " + code + " " + (out.length() > 120 ? out.substring(0, 120) : out));
        return out;
    }

    // ---- phone data and web data have the same shape (amounts in paise); the web copy needs ids ----
    private static String sid() { return Long.toString(System.currentTimeMillis(), 36) + Long.toString((long) (Math.random() * 1679616), 36); }

    static JSONObject toWeb(Store s) throws Exception {
        JSONArray en = new JSONArray(), pe = new JSONArray();
        JSONArray a = s.arr("entries");
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            if (!o.has("id")) o.put("id", sid());
            JSONObject e = new JSONObject().put("id", o.optString("id")).put("type", o.optString("type")).put("amt", o.optLong("amt"))
                    .put("note", o.optString("note")).put("ts", o.optLong("ts"));
            if (o.has("cat")) e.put("cat", o.optString("cat"));
            en.put(e);
        }
        a = s.arr("people");
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            if (!o.has("id")) o.put("id", sid());
            JSONArray tx = new JSONArray(), src = o.optJSONArray("tx");
            if (src != null) for (int k = 0; k < src.length(); k++) {
                JSONObject t = src.getJSONObject(k);
                if (!t.has("id")) t.put("id", sid());
                JSONObject x = new JSONObject().put("id", t.optString("id")).put("type", t.optString("type")).put("amt", t.optLong("amt"))
                        .put("note", t.optString("note")).put("ts", t.optLong("ts"));
                if (t.has("due")) x.put("due", t.optLong("due"));
                tx.put(x);
            }
            pe.put(new JSONObject().put("id", o.optString("id")).put("name", o.optString("name")).put("tx", tx));
        }
        s.saveRaw();
        JSONObject set = s.getObj("settings") == null ? new JSONObject() : new JSONObject(s.getObj("settings").toString());
        set.put("lang", s.lang());
        JSONObject bud = s.getObj("budgets") == null ? new JSONObject() : new JSONObject(s.getObj("budgets").toString());
        return new JSONObject().put("entries", en).put("people", pe).put("budgets", bud)
                .put("settings", set).put("updated", s.updated());
    }

    static void fromWeb(JSONObject w, Store s) throws Exception {
        JSONArray en = new JSONArray(), pe = new JSONArray();
        JSONArray a = w.optJSONArray("entries");
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            JSONObject e = new JSONObject().put("id", o.optString("id")).put("type", o.optString("type")).put("amt", o.optLong("amt"))
                    .put("note", o.optString("note")).put("ts", o.optLong("ts"));
            if (o.has("cat")) e.put("cat", o.optString("cat"));
            en.put(e);
        }
        a = w.optJSONArray("people");
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            JSONArray tx = new JSONArray(), src = o.optJSONArray("tx");
            if (src != null) for (int k = 0; k < src.length(); k++) {
                JSONObject t = src.getJSONObject(k);
                JSONObject x = new JSONObject().put("id", t.optString("id")).put("type", t.optString("type")).put("amt", t.optLong("amt"))
                        .put("note", t.optString("note")).put("ts", t.optLong("ts"));
                if (t.has("due")) x.put("due", t.optLong("due"));
                tx.put(x);
            }
            pe.put(new JSONObject().put("id", o.optString("id")).put("name", o.optString("name")).put("tx", tx));
        }
        s.put("entries", en);
        s.put("people", pe);
        if (w.optJSONObject("budgets") != null) s.put("budgets", w.getJSONObject("budgets"));
        if (w.optJSONObject("settings") != null) {
            JSONObject ws = new JSONObject(w.getJSONObject("settings").toString());
            ws.remove("lang"); // the language stays whatever this phone uses
            s.put("settings", ws);
        }
        s.put("updated", w.optDouble("updated", System.currentTimeMillis()));
        s.saveRaw();
        Reminders.schedule(s.context());
    }

    /** First sign-in: keep everything from both sides (same entry = same type, amount and time). */
    static JSONObject merge(JSONObject a, JSONObject b) throws Exception {
        JSONObject m = new JSONObject(b.toString());
        JSONArray en = m.optJSONArray("entries");
        if (en == null) { en = new JSONArray(); m.put("entries", en); }
        Map<String, Boolean> seen = new HashMap<>();
        for (int i = 0; i < en.length(); i++) seen.put(ek(en.getJSONObject(i)), true);
        JSONArray sa = a.optJSONArray("entries");
        if (sa != null) for (int i = 0; i < sa.length(); i++) if (seen.put(ek(sa.getJSONObject(i)), true) == null) en.put(sa.getJSONObject(i));
        JSONArray pe = m.optJSONArray("people");
        if (pe == null) { pe = new JSONArray(); m.put("people", pe); }
        JSONArray pa = a.optJSONArray("people");
        if (pa != null) for (int i = 0; i < pa.length(); i++) {
            JSONObject lp = pa.getJSONObject(i), match = null;
            for (int k = 0; k < pe.length(); k++) if (pe.getJSONObject(k).optString("name").equalsIgnoreCase(lp.optString("name"))) match = pe.getJSONObject(k);
            if (match == null) { pe.put(lp); continue; }
            JSONArray tx = match.optJSONArray("tx");
            if (tx == null) { tx = new JSONArray(); match.put("tx", tx); }
            Map<String, Boolean> ts = new HashMap<>();
            for (int k = 0; k < tx.length(); k++) ts.put(ek(tx.getJSONObject(k)), true);
            JSONArray lt = lp.optJSONArray("tx");
            if (lt != null) for (int k = 0; k < lt.length(); k++) if (ts.put(ek(lt.getJSONObject(k)), true) == null) tx.put(lt.getJSONObject(k));
        }
        return m;
    }

    private static String ek(JSONObject o) { return o.optString("type") + "|" + o.optLong("amt") + "|" + o.optLong("ts"); }
}
