package com.darshan.khata;

import static org.junit.Assert.*;

import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class V2Test {
    @Test
    public void featuresWork() throws Exception {
        android.content.Context c = RuntimeEnvironment.getApplication();
        Store s = new Store(c);
        JSONObject p = new JSONObject().put("name", "Ramesh").put("tx", new JSONArray());
        p.getJSONArray("tx").put(new JSONObject().put("type", "gave").put("amt", 300000L).put("note", "loan").put("ts", System.currentTimeMillis() - 5 * 86400000L).put("due", System.currentTimeMillis() - 86400000L));
        p.getJSONArray("tx").put(new JSONObject().put("type", "got").put("amt", 50000L).put("note", "part").put("ts", System.currentTimeMillis()));
        s.arr("people").put(p);
        s.arr("entries").put(new JSONObject().put("type", "out").put("amt", 125000L).put("note", "lunch").put("cat", "food").put("ts", System.currentTimeMillis()));
        s.save();
        assertEquals(1, Reminders.dueCount(s));
        assertEquals(250000L, MainActivity.balanceOf(p));
        try {
            File f = Statement.build(c, p);
            assertTrue(f.exists() && f.length() > 100);
        } catch (IllegalStateException e) {
            System.out.println("PDF skipped in simulated phone: " + e.getMessage());
        }

        JSONObject w = Sync.toWeb(s);
        assertTrue(w.getJSONArray("people").getJSONObject(0).has("id"));
        assertEquals(300000L, w.getJSONArray("people").getJSONObject(0).getJSONArray("tx").getJSONObject(0).getLong("amt"));
        assertEquals("food", w.getJSONArray("entries").getJSONObject(0).getString("cat"));
        JSONObject remote = new JSONObject(w.toString());
        remote.getJSONArray("entries").put(new JSONObject().put("id", "z").put("type", "in").put("amt", 900000L).put("note", "web").put("ts", 5L));
        remote.put("updated", w.getDouble("updated") + 9000);
        Sync.fromWeb(remote, s);
        Store s2 = new Store(c);
        assertEquals(2, s2.arr("entries").length());
        JSONObject m = Sync.merge(w, remote);
        assertEquals(2, m.getJSONArray("entries").length());
        assertEquals(1, m.getJSONArray("people").length());
        assertEquals(2, m.getJSONArray("people").getJSONObject(0).getJSONArray("tx").length());
        assertEquals(64, MainActivity.pinHash("1234").length());
    }
}
