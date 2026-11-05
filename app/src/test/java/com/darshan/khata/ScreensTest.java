package com.darshan.khata;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.json.JSONArray;
import org.json.JSONObject;

@RunWith(RobolectricTestRunner.class)
public class ScreensTest {
    @Test
    public void allTabsRender() throws Exception {
        android.content.Context c = org.robolectric.RuntimeEnvironment.getApplication();
        Store s = new Store(c);
        s.arr("entries").put(new JSONObject().put("type", "out").put("amt", 125000L).put("cat", "food").put("note", "x").put("ts", System.currentTimeMillis()));
        s.arr("entries").put(new JSONObject().put("type", "in").put("amt", 500000L).put("note", "pay").put("ts", System.currentTimeMillis()));
        JSONObject p = new JSONObject().put("name", "Ramesh").put("tx", new JSONArray().put(new JSONObject().put("type", "gave").put("amt", 300000L).put("note", "loan").put("ts", System.currentTimeMillis()).put("due", System.currentTimeMillis() - 2 * 86400000L)));
        s.arr("people").put(p);
        s.saveRaw();
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        for (int t = 0; t < 4; t++) { a.tab = t; a.openPerson = -1; a.render(); assertTrue(a.content.getChildCount() > 0); }
        a.tab = 1; a.openPerson = 0; a.render();
        assertTrue(a.content.getChildCount() > 3);
        a.getSharedPreferences("lock", 0).edit().putString("pin", MainActivity.pinHash("1234")).apply();
        a.lockShown = false; a.lockIfNeeded();
        assertTrue(a.lockShown);
    }
}
