package com.darshan.khata;

import static org.junit.Assert.*;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class V3Test {
    static void texts(View v, StringBuilder sb) {
        if (v instanceof TextView) sb.append(((TextView) v).getText()).append(" | ");
        if (v instanceof ViewGroup) { ViewGroup g = (ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) texts(g.getChildAt(i), sb); }
    }
    static String screen(MainActivity a) { StringBuilder sb = new StringBuilder(); texts(a.content, sb); return sb.toString(); }
    static Button find(View v, String text) {
        if (v instanceof Button && ((Button) v).getText().toString().contains(text)) return (Button) v;
        if (v instanceof ViewGroup) { ViewGroup g = (ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) { Button b = find(g.getChildAt(i), text); if (b != null) return b; } }
        return null;
    }

    @Test
    public void v3Features() throws Exception {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        L.gu = false;
        long now = System.currentTimeMillis();
        a.st.arr("entries").put(new JSONObject().put("id", "1").put("type", "out").put("amt", 60000).put("cat", "food").put("note", "Lunch").put("ts", now));
        a.st.arr("entries").put(new JSONObject().put("id", "2").put("type", "out").put("amt", 10000).put("cat", "travel").put("note", "Auto").put("ts", now));
        a.st.arr("entries").put(new JSONObject().put("id", "3").put("type", "in").put("amt", 500000).put("note", "Salary").put("ts", now));
        a.st.put("budgets", new JSONObject().put("food", 70000));
        a.st.save();
        a.tab = 0; a.render();
        String s = screen(a);
        assertTrue(s, s.contains("budget 86% used"));
        // filter
        V3.fq = "lun";
        a.render();
        s = screen(a);
        assertTrue(s, s.contains("Lunch") && !s.contains("Salary") && !s.contains("Auto"));
        V3.fq = ""; V3.ft = "in"; a.render();
        s = screen(a);
        assertTrue(s, s.contains("Salary") && !s.contains("Lunch"));
        V3.ft = "all"; V3.fc = "travel"; a.render();
        s = screen(a);
        assertTrue(s, s.contains("Auto") && !s.contains("Lunch"));
        V3.fc = "all";
        // report budget card
        a.tab = 2; a.render();
        s = screen(a);
        assertTrue(s, s.contains("Monthly budget") && s.contains("86% used"));
        // csv
        String csv = V3.csv(a);
        assertTrue(csv, csv.contains("Lunch") && csv.contains("600") && csv.contains("Expense"));
        // appearance in settings
        a.tab = 3; a.render();
        Button dark = find(a.content, "Dark mode: Off");
        assertNotNull(dark);
        dark.performClick();
        assertTrue(Theme.dark);
        assertEquals(0xFF111215, MainActivity.BG);
        // sync mapping keeps budgets and settings
        JSONObject w = Sync.toWeb(a.st);
        assertEquals(70000, w.getJSONObject("budgets").getLong("food"));
        assertTrue(w.getJSONObject("settings").optBoolean("dark"));
        Store s2 = new Store(a);
        s2.put("budgets", new JSONObject());
        w.getJSONObject("settings").put("color", "#2F6FED");
        Sync.fromWeb(w, s2);
        assertEquals(70000, s2.getObj("budgets").getLong("food"));
        assertEquals("#2F6FED", s2.getObj("settings").getString("color"));
        for (int t = 0; t <= 3; t++) { a.tab = t; a.render(); }
    }
}
