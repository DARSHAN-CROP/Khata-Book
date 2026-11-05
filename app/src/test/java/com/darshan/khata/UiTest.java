package com.darshan.khata;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
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
public class UiTest {
    static void shot(MainActivity a, String name) throws Exception {
        String dir = System.getProperty("shots.dir", "build/screenshots");
        new File(dir).mkdirs();
        View root = a.getWindow().getDecorView();
        int w = 1179, h = 2556;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bmp));
        try (FileOutputStream o = new FileOutputStream(new File(dir, name))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, o); }
    }

    @Test
    public void walkThroughAllScreens() throws Exception {
        assertEquals("1,23,456", MainActivity.indian(123456));
        assertEquals("999", MainActivity.indian(999));
        assertEquals("1,000", MainActivity.indian(1000));
        assertEquals("12,34,567.50", MainActivity.money(123456750));
        assertEquals(15050, MainActivity.parse("150.5"));
        assertEquals(-1, MainActivity.parse("abc"));
        assertEquals(-1, MainActivity.parse("0"));
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        L.gu = true;
        a.st.setLang("gu");
        long now = System.currentTimeMillis();
        Object[][] es = {{"in", 1500000L, "પગાર"}, {"out", 45000L, "શાકભાજી"}, {"out", 120000L, "વીજળી બિલ"}, {"in", 250000L, "ટ્યુશન"}};
        for (int i = 0; i < es.length; i++) {
            JSONObject o = new JSONObject();
            o.put("type", es[i][0]); o.put("amt", es[i][1]); o.put("note", es[i][2]); o.put("ts", now - i * 3600000L);
            a.st.arr("entries").put(o);
        }
        a.render();
        shot(a, "1_ledger_gu.png");
        // credit
        String[] names = {"રમેશભાઈ", "કિશન", "મીના"};
        long[][] txs = {{50000, 0}, {120000, 20000}, {0, 30000}};
        for (int i = 0; i < names.length; i++) {
            JSONObject p = new JSONObject();
            p.put("name", names[i]);
            org.json.JSONArray tx = new org.json.JSONArray();
            if (txs[i][0] > 0) { JSONObject t = new JSONObject(); t.put("type", "gave"); t.put("amt", txs[i][0]); t.put("note", "સામાન"); t.put("ts", now); tx.put(t); }
            if (txs[i][1] > 0) { JSONObject t = new JSONObject(); t.put("type", "got"); t.put("amt", txs[i][1]); t.put("note", ""); t.put("ts", now); tx.put(t); }
            p.put("tx", tx);
            a.st.arr("people").put(p);
        }
        a.tab = 1; a.render();
        shot(a, "2_credit_gu.png");
        assertEquals(50000, MainActivity.balanceOf(a.st.arr("people").optJSONObject(0)));
        assertEquals(100000, MainActivity.balanceOf(a.st.arr("people").optJSONObject(1)));
        assertEquals(-30000, MainActivity.balanceOf(a.st.arr("people").optJSONObject(2)));
        a.openPerson = 1; a.render();
        shot(a, "3_person_gu.png");
        L.gu = false; a.openPerson = -1; a.tab = 0; a.render();
        shot(a, "4_ledger_en.png");
        assertEquals("Khata Book", L.t("app"));
        a.st.save();
        assertEquals(4, new Store(a).arr("entries").length());
    }
}
