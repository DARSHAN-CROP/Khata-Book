package com.darshan.khata;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** V3: appearance settings, monthly category budgets, search/filter and CSV export. */
final class V3 {
    private V3() {}

    static String fq = "", ft = "all", fc = "all";

    // ---------------------------------------------------------------- appearance

    static void setSetting(MainActivity a, String key, Object val) {
        try {
            JSONObject s = a.st.getObj("settings");
            if (s == null) s = new JSONObject();
            if (val == null) s.remove(key); else s.put(key, val);
            a.st.put("settings", s);
        } catch (JSONException ignored) { }
        a.st.save();
        a.render();
    }

    static Button toggle(final MainActivity a, String labelKey, final boolean on, final String key) {
        Button b = Ui.button(a, String.format(Locale.US, L.t(labelKey), L.t(on ? "ap_on" : "ap_off")), on ? MainActivity.TEAL : Theme.grey, on ? Color.WHITE : MainActivity.INK, v -> setSetting(a, key, !on));
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, Ui.dp(a, 8), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    static View appearanceCard(final MainActivity a) {
        LinearLayout c = Ui.card(a);
        c.addView(Ui.text(a, L.t("ap"), 17, MainActivity.INK, true));
        TextView lbl = Ui.text(a, L.t("ap_color"), 13, MainActivity.MUTED, false);
        lbl.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 6));
        c.addView(lbl);
        LinearLayout sw = Ui.row(a);
        for (final String col : Theme.COLORS) {
            boolean on = Theme.parse(col) == Theme.accent;
            Button b = Ui.button(a, on ? "\u2713" : "", Theme.parse(col), Color.WHITE, v -> setSetting(a, "color", col));
            b.setMinWidth(0);
            b.setPadding(0, 0, 0, 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(a, 40), 1);
            lp.setMargins(Ui.dp(a, 3), 0, Ui.dp(a, 3), 0);
            sw.addView(b, lp);
        }
        c.addView(sw, Ui.match());
        c.addView(toggle(a, "ap_dark", Theme.dark, "dark"));
        String[] sz = {"ap_s0", "ap_s1", "ap_s2", "ap_s3"};
        Button sb = Ui.button(a, String.format(Locale.US, L.t("ap_size"), L.t(sz[Theme.size])), Theme.grey, MainActivity.INK, v -> setSetting(a, "size", (Theme.size + 1) % 4));
        LinearLayout.LayoutParams l1 = Ui.match();
        l1.setMargins(0, Ui.dp(a, 8), 0, 0);
        c.addView(sb, l1);
        c.addView(toggle(a, "ap_bold", Theme.bold, "bold"));
        c.addView(toggle(a, "ap_hc", Theme.hc, "hc"));
        Button rs = Ui.button(a, L.t("ap_reset"), Theme.grey, MainActivity.INK, v -> {
            JSONObject s = a.st.getObj("settings");
            if (s != null) for (String k : new String[]{"color", "dark", "size", "bold", "hc"}) s.remove(k);
            a.st.save();
            a.render();
        });
        c.addView(rs, l1);
        return c;
    }

    // ---------------------------------------------------------------- budgets (paise per category, same keys as the web app)

    static long budget(MainActivity a, String cat) {
        JSONObject b = a.st.getObj("budgets");
        return b == null ? 0 : b.optLong(cat, 0);
    }

    static long spent(MainActivity a, String cat) {
        JSONArray arr = a.st.arr("entries");
        long s = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null || !o.optString("type").equals("out") || !a.sameMonth(o.optLong("ts"))) continue;
            if (o.optString("cat", "other").equals(cat)) s += o.optLong("amt");
        }
        return s;
    }

    static int pct(long spent, long budget) { return budget <= 0 ? 0 : (int) Math.round(spent * 100.0 / budget); }

    static void alerts(MainActivity a) {
        for (String c : MainActivity.CATS) {
            long b = budget(a, c);
            if (b <= 0) continue;
            int p = pct(spent(a, c), b);
            if (p < 80) continue;
            LinearLayout card = Ui.card(a);
            card.setBackground(Ui.box(a, 0xFFFFF4E5, 14, 0xFFFFD9A0));
            card.addView(Ui.text(a, String.format(Locale.US, L.t("bud_warn"), L.t("cat_" + c), p), 14, 0xFF5A3B00, true));
            a.content.addView(card);
        }
    }

    static View budgetCard(final MainActivity a) {
        LinearLayout c = Ui.card(a);
        c.addView(Ui.text(a, L.t("bud"), 17, MainActivity.INK, true));
        c.addView(Ui.text(a, L.t("bud_hint"), 13, MainActivity.MUTED, false));
        final EditText[] in = new EditText[MainActivity.CATS.length];
        for (int i = 0; i < MainActivity.CATS.length; i++) {
            String cat = MainActivity.CATS[i];
            long b = budget(a, cat), sp = spent(a, cat);
            LinearLayout row = Ui.row(a);
            row.setPadding(0, Ui.dp(a, 10), 0, 0);
            row.addView(Ui.text(a, L.t("cat_" + cat), 14, MainActivity.INK, true), Ui.weight(1));
            row.addView(Ui.text(a, "\u20b9" + MainActivity.money(sp) + (b > 0 ? " / \u20b9" + MainActivity.money(b) : ""), 13, MainActivity.MUTED, false));
            c.addView(row);
            in[i] = new EditText(a);
            in[i].setHint("\u20b9");
            in[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            in[i].setSingleLine(true);
            if (b > 0) in[i].setText(MainActivity.plain(b));
            c.addView(in[i]);
            if (b > 0) {
                int p = pct(sp, b);
                c.addView(a.bar(Math.min(1f, p / 100f), p >= 100 ? MainActivity.RED : p >= 80 ? 0xFFE8A200 : MainActivity.GREEN));
                c.addView(Ui.text(a, p >= 100 ? L.t("bud_over") : String.format(Locale.US, L.t("bud_used"), p), 12, MainActivity.MUTED, false));
            }
        }
        Button save = Ui.button(a, L.t("save"), MainActivity.TEAL, Color.WHITE, v -> {
            try {
                JSONObject bd = new JSONObject();
                for (int i = 0; i < in.length; i++) {
                    long p = MainActivity.parse(in[i].getText().toString());
                    if (p > 0) bd.put(MainActivity.CATS[i], p);
                }
                a.st.put("budgets", bd);
            } catch (JSONException ignored) { }
            a.st.save();
            a.render();
        });
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, Ui.dp(a, 12), 0, 0);
        c.addView(save, lp);
        return c;
    }

    // ---------------------------------------------------------------- search / filter

    static boolean active() { return !fq.trim().isEmpty() || !ft.equals("all") || !fc.equals("all"); }

    static boolean matches(JSONObject o) {
        boolean isIn = o.optString("type").equals("in");
        if (!ft.equals("all") && !(ft.equals("in") == isIn)) return false;
        if (!fc.equals("all") && (isIn || !o.optString("cat", "other").equals(fc))) return false;
        String q = fq.trim().toLowerCase(Locale.US);
        if (!q.isEmpty()) {
            String hay = (o.optString("note") + " " + (o.has("cat") ? L.t("cat_" + o.optString("cat")) : "") + " " + MainActivity.plain(o.optLong("amt"))).toLowerCase(Locale.US);
            if (!hay.contains(q)) return false;
        }
        return true;
    }

    static ArrayList<JSONObject> apply(ArrayList<JSONObject> list) {
        if (!active()) return list;
        ArrayList<JSONObject> out = new ArrayList<>();
        for (JSONObject o : list) if (matches(o)) out.add(o);
        return out;
    }

    static void filterBar(final MainActivity a, int shown) {
        LinearLayout row = Ui.row(a);
        Button b = Ui.button(a, L.t("srch_btn"), Theme.grey, MainActivity.INK, v -> filterDialog(a));
        row.addView(b, Ui.weight(1));
        if (active()) {
            Button x = Ui.button(a, L.t("srch_clear") + " (" + shown + ")", MainActivity.TEAL, Color.WHITE, v -> { fq = ""; ft = "all"; fc = "all"; a.render(); });
            LinearLayout.LayoutParams lp = Ui.weight(1);
            lp.setMargins(Ui.dp(a, 8), 0, 0, 0);
            row.addView(x, lp);
        }
        LinearLayout.LayoutParams rl = Ui.match();
        rl.setMargins(0, 0, 0, Ui.dp(a, 10));
        a.content.addView(row, rl);
    }

    static void filterDialog(final MainActivity a) {
        LinearLayout box = a.dialogBox();
        final EditText q = a.field(L.t("srch"), false);
        q.setText(fq);
        box.addView(q);
        final String[] types = {"all", "in", "out"};
        final String[] typeNames = {L.t("f_all"), L.t("income"), L.t("expense")};
        final String[] cats = new String[MainActivity.CATS.length + 1];
        final String[] catNames = new String[cats.length];
        cats[0] = "all";
        catNames[0] = L.t("f_cat");
        for (int i = 0; i < MainActivity.CATS.length; i++) { cats[i + 1] = MainActivity.CATS[i]; catNames[i + 1] = L.t("cat_" + MainActivity.CATS[i]); }
        final int[] ti = {0}, ci = {0};
        for (int i = 0; i < types.length; i++) if (types[i].equals(ft)) ti[0] = i;
        for (int i = 0; i < cats.length; i++) if (cats[i].equals(fc)) ci[0] = i;
        final Button tb = Ui.button(a, "", Theme.grey, MainActivity.INK, null);
        final Button cb = Ui.button(a, "", Theme.grey, MainActivity.INK, null);
        final Runnable upd = () -> { tb.setText(typeNames[ti[0]]); cb.setText(catNames[ci[0]]); };
        upd.run();
        tb.setOnClickListener(v -> { ti[0] = (ti[0] + 1) % types.length; upd.run(); });
        cb.setOnClickListener(v -> { ci[0] = (ci[0] + 1) % cats.length; upd.run(); });
        box.addView(tb);
        box.addView(cb);
        new AlertDialog.Builder(a).setTitle(L.t("srch_btn")).setView(box)
                .setPositiveButton(L.t("ok"), (d, w) -> { fq = q.getText().toString(); ft = types[ti[0]]; fc = cats[ci[0]]; a.render(); })
                .setNegativeButton(L.t("cancel"), null).show();
    }

    // ---------------------------------------------------------------- CSV export (opens in Excel / Google Sheets)

    static String esc(String v) {
        if (v == null) v = "";
        return v.contains(",") || v.contains("\"") || v.contains("\n") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

    static String csv(MainActivity a) {
        StringBuilder sb = new StringBuilder("\ufeff");
        sb.append(L.t("csv_head")).append("\r\n");
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        JSONArray en = a.st.arr("entries");
        for (int i = 0; i < en.length(); i++) {
            JSONObject o = en.optJSONObject(i);
            boolean in = o.optString("type").equals("in");
            sb.append(f.format(new Date(o.optLong("ts")))).append(',').append(in ? "Income" : "Expense").append(',')
              .append(esc(o.has("cat") ? L.t("cat_" + o.optString("cat")) : "")).append(',').append(esc(o.optString("note"))).append(',')
              .append(MainActivity.plain(o.optLong("amt"))).append("\r\n");
        }
        JSONArray pe = a.st.arr("people");
        for (int i = 0; i < pe.length(); i++) {
            JSONObject p = pe.optJSONObject(i);
            JSONArray tx = p.optJSONArray("tx");
            if (tx == null) continue;
            for (int k = 0; k < tx.length(); k++) {
                JSONObject t = tx.optJSONObject(k);
                sb.append(f.format(new Date(t.optLong("ts")))).append(',').append(t.optString("type").equals("gave") ? "Credit given" : "Credit received").append(',')
                  .append(esc(p.optString("name"))).append(',').append(esc(t.optString("note"))).append(',').append(MainActivity.plain(t.optLong("amt"))).append("\r\n");
            }
        }
        return sb.toString();
    }

    static void exportCsv(MainActivity a) {
        try {
            File dir = new File(a.getCacheDir(), "statements");
            dir.mkdirs();
            File f = new File(dir, "khata-" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()) + ".csv");
            try (FileOutputStream o = new FileOutputStream(f)) { o.write(csv(a).getBytes("UTF-8")); }
            Statement.share(a, f, "text/csv", L.t("csv_btn"));
        } catch (Exception e) { Toast.makeText(a, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
    }
}
