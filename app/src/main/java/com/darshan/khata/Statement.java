package com.darshan.khata;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** One person's account as a PDF, shared (WhatsApp etc.) through the Android share sheet. */
final class Statement {
    private Statement() {}

    static File build(Context c, JSONObject person) throws Exception {
        final int W = 595, H = 842, M = 40;
        PdfDocument doc = new PdfDocument();
        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG), body = new Paint(Paint.ANTI_ALIAS_FLAG), bold = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setTextSize(22); title.setTypeface(Typeface.DEFAULT_BOLD); title.setColor(0xFF0E8F7E);
        body.setTextSize(13); body.setColor(0xFF1B1F1F);
        bold.setTextSize(14); bold.setTypeface(Typeface.DEFAULT_BOLD); bold.setColor(0xFF1B1F1F);
        JSONArray tx = person.optJSONArray("tx");
        ArrayList<JSONObject> list = new ArrayList<>();
        if (tx != null) for (int i = 0; i < tx.length(); i++) list.add(tx.getJSONObject(i));
        Collections.sort(list, new Comparator<JSONObject>() {
            public int compare(JSONObject x, JSONObject y) { return Long.compare(x.optLong("ts"), y.optLong("ts")); }
        });
        int pageNo = 1, y = 0;
        PdfDocument.Page page = null;
        Canvas cv = null;
        for (int i = -1; i <= list.size(); i++) {
            if (page == null || y > H - 90) {
                if (page != null) doc.finishPage(page);
                page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo++).create());
                cv = page.getCanvas();
                y = M + 20;
                if (pageNo == 2) {
                    cv.drawText(L.t("app") + " - " + L.t("stmt"), M, y, title);
                    y += 30;
                    cv.drawText(person.optString("name"), M, y, bold);
                    y += 18;
                    cv.drawText(MainActivity.day(System.currentTimeMillis()), M, y, body);
                    y += 28;
                }
            }
            if (i == -1) continue;
            if (i == list.size()) {
                y += 10;
                long b = MainActivity.balanceOf(person);
                String s = b > 0 ? String.format(Locale.US, L.t("owe_me"), MainActivity.money(b))
                        : b < 0 ? String.format(Locale.US, L.t("i_owe"), MainActivity.money(-b)) : L.t("settled");
                cv.drawText(L.t("balance") + ": " + s, M, y, bold);
                continue;
            }
            JSONObject t = list.get(i);
            boolean gave = t.optString("type").equals("gave");
            cv.drawText(MainActivity.day(t.optLong("ts")), M, y, body);
            String note = t.optString("note");
            if (note.length() > 28) note = note.substring(0, 28);
            cv.drawText(L.t(gave ? "gave" : "got") + (note.isEmpty() ? "" : " - " + note), M + 110, y, body);
            String amt = (gave ? "+ " : "- ") + MainActivity.money(t.optLong("amt"));
            cv.drawText(amt, W - M - body.measureText(amt), y, body);
            y += 20;
        }
        if (page != null) doc.finishPage(page);
        File dir = new File(c.getCacheDir(), "statements");
        dir.mkdirs();
        String safe = person.optString("name").replaceAll("[^A-Za-z0-9]", "_");
        File f = new File(dir, "Khata_" + (safe.isEmpty() ? "account" : safe) + ".pdf");
        try (FileOutputStream o = new FileOutputStream(f)) { doc.writeTo(o); }
        doc.close();
        return f;
    }

    static void share(Context c, File f, String mime, String chooserTitle) {
        Uri u = Uri.parse("content://" + c.getPackageName() + ".files/" + f.getName());
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_STREAM, u);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        c.startActivity(Intent.createChooser(i, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
