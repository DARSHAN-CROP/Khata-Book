package com.darshan.khata;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    static final int TEAL = 0xFF0E8F7E, BG = 0xFFF4F6F6, INK = 0xFF1B1F1F, MUTED = 0xFF66706F;
    static final int GREEN = 0xFF1E9E63, RED = 0xFFD64545, GREY = 0xFFEAEFEE;

    Store st;
    LinearLayout content, nav;
    TextView title;
    Button langBtn;
    int tab = 0;
    int openPerson = -1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        st = new Store(this);
        L.gu = st.lang().equals("gu");
        getWindow().setStatusBarColor(TEAL);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout head = Ui.row(this);
        head.setBackgroundColor(TEAL);
        head.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        title = Ui.text(this, "", 20, Color.WHITE, true);
        head.addView(title, Ui.weight(1));
        langBtn = Ui.button(this, "", 0x33FFFFFF, Color.WHITE, v -> {
            L.gu = !L.gu;
            st.setLang(L.gu ? "gu" : "en");
            render();
        });
        head.addView(langBtn);
        root.addView(head, Ui.match());

        ScrollView sv = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        sv.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        nav = Ui.row(this);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));
        root.addView(nav, Ui.match());
        setContentView(root);
        render();
    }

    // ---------------------------------------------------------------- helpers

    /** Indian digit grouping: 1,23,456 */
    static String indian(long n) {
        String s = Long.toString(Math.abs(n));
        if (s.length() > 3) {
            String last = s.substring(s.length() - 3), rest = s.substring(0, s.length() - 3);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rest.length(); i++) {
                if (i > 0 && (rest.length() - i) % 2 == 0) sb.append(',');
                sb.append(rest.charAt(i));
            }
            s = sb + "," + last;
        }
        return n < 0 ? "-" + s : s;
    }

    /** paise -> "1,234" or "1,234.50" (no rupee sign) */
    static String money(long paise) {
        long a = Math.abs(paise);
        String r = indian(a / 100);
        long q = a % 100;
        if (q != 0) r += String.format(Locale.US, ".%02d", q);
        return (paise < 0 ? "-" : "") + r;
    }

    static long parse(String s) {
        try {
            BigDecimal d = new BigDecimal(s.trim().replace(",", ""));
            if (d.signum() <= 0 || d.compareTo(new BigDecimal("99999999")) > 0) return -1;
            return d.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
        } catch (Exception e) { return -1; }
    }

    static String day(long ts) { return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(new Date(ts)); }

    static boolean sameMonth(long ts) {
        Calendar a = Calendar.getInstance(), b = Calendar.getInstance();
        b.setTimeInMillis(ts);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.MONTH) == b.get(Calendar.MONTH);
    }

    static void remove(JSONArray a, JSONObject o) {
        for (int i = 0; i < a.length(); i++) if (a.optJSONObject(i) == o) { a.remove(i); return; }
    }

    Button big(String s, int bg, int fg, View.OnClickListener l) {
        Button x = Ui.button(this, s, bg, fg, l);
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, 0, 0, Ui.dp(this, 12));
        x.setLayoutParams(lp);
        return x;
    }

    TextView empty(String s) {
        TextView t = Ui.text(this, s, 15, MUTED, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 30), 0, 0);
        return t;
    }

    EditText field(String hint, boolean number) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setInputType(number ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        return e;
    }

    LinearLayout dialogBox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), 0);
        return l;
    }

    void ask(String message, Runnable yes) {
        new AlertDialog.Builder(this).setMessage(message)
                .setPositiveButton(L.t("yes"), (d, w) -> yes.run())
                .setNegativeButton(L.t("cancel"), null).show();
    }

    TextView cross() {
        TextView x = Ui.text(this, "  \u2715", 18, MUTED, true);
        x.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
        return x;
    }

    interface Done { void ok(long paise, String note); }

    /** Dialog with amount + note. */
    void amountDialog(String dialogTitle, Done done) {
        LinearLayout box = dialogBox();
        final EditText amt = field(L.t("amount"), true);
        final EditText note = field(L.t("note"), false);
        box.addView(amt);
        box.addView(note);
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle(dialogTitle).setView(box)
                .setPositiveButton(L.t("save"), null).setNegativeButton(L.t("cancel"), null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long p = parse(amt.getText().toString());
            if (p < 0) { Toast.makeText(this, L.t("need_amount"), Toast.LENGTH_SHORT).show(); return; }
            dlg.dismiss();
            done.ok(p, note.getText().toString().trim());
        }));
        dlg.show();
    }

    // ---------------------------------------------------------------- shell

    void render() {
        title.setText(L.t("app"));
        langBtn.setText(L.t("lang_btn"));
        nav.removeAllViews();
        String[] keys = {"tab_ledger", "tab_credit"};
        for (int i = 0; i < keys.length; i++) {
            final int idx = i;
            boolean sel = i == tab;
            Button x = Ui.button(this, L.t(keys[i]), sel ? TEAL : Color.TRANSPARENT, sel ? Color.WHITE : MUTED, v -> { tab = idx; openPerson = -1; render(); });
            LinearLayout.LayoutParams lp = Ui.weight(1);
            lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
            nav.addView(x, lp);
        }
        content.removeAllViews();
        if (tab == 0) ledger();
        else if (openPerson >= 0 && openPerson < st.arr("people").length()) person(openPerson);
        else credit();
    }

    // ---------------------------------------------------------------- ledger

    void ledger() {
        final JSONArray a = st.arr("entries");
        long in = 0, out = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (!sameMonth(o.optLong("ts"))) continue;
            if (o.optString("type").equals("in")) in += o.optLong("amt"); else out += o.optLong("amt");
        }
        LinearLayout sum = Ui.card(this);
        sum.addView(Ui.text(this, L.t("this_month"), 14, MUTED, true));
        LinearLayout r = Ui.row(this);
        r.addView(box3(L.t("income"), "₹" + money(in), GREEN), Ui.weight(1));
        r.addView(box3(L.t("expense"), "₹" + money(out), RED), Ui.weight(1));
        r.addView(box3(L.t("balance"), "₹" + money(in - out), in - out >= 0 ? INK : RED), Ui.weight(1));
        sum.addView(r);
        content.addView(sum);

        LinearLayout btns = Ui.row(this);
        Button bi = Ui.button(this, "+  " + L.t("income"), GREEN, Color.WHITE, v -> addEntry("in"));
        Button bo = Ui.button(this, "+  " + L.t("expense"), RED, Color.WHITE, v -> addEntry("out"));
        LinearLayout.LayoutParams l1 = Ui.weight(1), l2 = Ui.weight(1);
        l1.setMargins(0, 0, Ui.dp(this, 6), 0);
        l2.setMargins(Ui.dp(this, 6), 0, 0, 0);
        btns.addView(bi, l1);
        btns.addView(bo, l2);
        LinearLayout.LayoutParams bl = Ui.match();
        bl.setMargins(0, 0, 0, Ui.dp(this, 12));
        content.addView(btns, bl);

        if (a.length() == 0) { content.addView(empty(L.t("no_entries"))); return; }
        ArrayList<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) list.add(a.optJSONObject(i));
        Collections.sort(list, new Comparator<JSONObject>() {
            public int compare(JSONObject x, JSONObject y) { return Long.compare(y.optLong("ts"), x.optLong("ts")); }
        });
        for (final JSONObject o : list) {
            boolean isIn = o.optString("type").equals("in");
            LinearLayout card = Ui.card(this);
            LinearLayout row = Ui.row(this);
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            String note = o.optString("note");
            left.addView(Ui.text(this, note.isEmpty() ? L.t(isIn ? "income_label" : "expense_label") : note, 16, INK, true));
            left.addView(Ui.text(this, day(o.optLong("ts")), 13, MUTED, false));
            row.addView(left, Ui.weight(1));
            row.addView(Ui.text(this, (isIn ? "+ ₹" : "- ₹") + money(o.optLong("amt")), 17, isIn ? GREEN : RED, true));
            TextView x = cross();
            x.setOnClickListener(v -> ask(L.t("sure"), () -> { remove(a, o); st.save(); render(); }));
            row.addView(x);
            card.addView(row);
            content.addView(card);
        }
    }

    View box3(String label, String value, int color) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, Ui.dp(this, 6), 0, 0);
        l.addView(Ui.text(this, label, 13, MUTED, false));
        l.addView(Ui.text(this, value, 17, color, true));
        return l;
    }

    void addEntry(final String type) {
        amountDialog(L.t(type.equals("in") ? "add_income" : "add_expense"), (p, note) -> {
            try {
                JSONObject o = new JSONObject();
                o.put("type", type);
                o.put("amt", p);
                o.put("note", note);
                o.put("ts", System.currentTimeMillis());
                st.arr("entries").put(o);
            } catch (JSONException ignored) { }
            st.save();
            render();
        });
    }

    // ---------------------------------------------------------------- credit (udhar)

    static long balanceOf(JSONObject person) {
        JSONArray tx = person.optJSONArray("tx");
        long b = 0;
        if (tx != null) for (int i = 0; i < tx.length(); i++) {
            JSONObject t = tx.optJSONObject(i);
            b += t.optString("type").equals("gave") ? t.optLong("amt") : -t.optLong("amt");
        }
        return b;
    }

    String balanceText(long b) {
        if (b > 0) return String.format(Locale.US, L.t("owe_me"), money(b));
        if (b < 0) return String.format(Locale.US, L.t("i_owe"), money(-b));
        return L.t("settled");
    }

    void credit() {
        final JSONArray ppl = st.arr("people");
        long recv = 0, give = 0;
        for (int i = 0; i < ppl.length(); i++) {
            long b = balanceOf(ppl.optJSONObject(i));
            if (b > 0) recv += b; else give -= b;
        }
        LinearLayout sum = Ui.card(this);
        LinearLayout r = Ui.row(this);
        r.addView(box3(L.t("total_recv"), "₹" + money(recv), GREEN), Ui.weight(1));
        r.addView(box3(L.t("total_give"), "₹" + money(give), RED), Ui.weight(1));
        sum.addView(r);
        content.addView(sum);

        content.addView(big("+  " + L.t("add_person"), TEAL, Color.WHITE, v -> {
            LinearLayout box = dialogBox();
            final EditText e = field(L.t("person"), false);
            box.addView(e);
            AlertDialog dlg = new AlertDialog.Builder(this).setTitle(L.t("add_person")).setView(box)
                    .setPositiveButton(L.t("save"), null).setNegativeButton(L.t("cancel"), null).create();
            dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(vv -> {
                String s = e.getText().toString().trim();
                if (s.isEmpty()) { Toast.makeText(this, L.t("need_name"), Toast.LENGTH_SHORT).show(); return; }
                try {
                    JSONObject o = new JSONObject();
                    o.put("name", s);
                    o.put("tx", new JSONArray());
                    ppl.put(o);
                } catch (JSONException ignored) { }
                st.save();
                dlg.dismiss();
                render();
            }));
            dlg.show();
        }));

        if (ppl.length() == 0) { content.addView(empty(L.t("no_people"))); return; }
        for (int i = 0; i < ppl.length(); i++) {
            final int idx = i;
            JSONObject p = ppl.optJSONObject(i);
            long b = balanceOf(p);
            LinearLayout card = Ui.card(this);
            LinearLayout row = Ui.row(this);
            row.addView(Ui.text(this, p.optString("name"), 18, INK, true), Ui.weight(1));
            row.addView(Ui.text(this, balanceText(b), 14, b > 0 ? GREEN : (b < 0 ? RED : MUTED), true));
            card.addView(row);
            card.setOnClickListener(v -> { openPerson = idx; render(); });
            content.addView(card);
        }
    }

    void person(final int idx) {
        final JSONArray ppl = st.arr("people");
        final JSONObject p = ppl.optJSONObject(idx);
        final JSONArray tx = p.optJSONArray("tx");
        long b = balanceOf(p);

        Button back = Ui.button(this, L.t("back"), GREY, INK, v -> { openPerson = -1; render(); });
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bl.setMargins(0, 0, 0, Ui.dp(this, 12));
        content.addView(back, bl);

        LinearLayout head = Ui.card(this);
        head.addView(Ui.text(this, p.optString("name"), 22, INK, true));
        head.addView(Ui.text(this, balanceText(b), 18, b > 0 ? GREEN : (b < 0 ? RED : MUTED), true));
        content.addView(head);

        LinearLayout btns = Ui.row(this);
        Button g = Ui.button(this, "+  " + L.t("gave"), RED, Color.WHITE, v -> addTx(p, "gave"));
        Button m = Ui.button(this, "+  " + L.t("got"), GREEN, Color.WHITE, v -> addTx(p, "got"));
        LinearLayout.LayoutParams l1 = Ui.weight(1), l2 = Ui.weight(1);
        l1.setMargins(0, 0, Ui.dp(this, 6), 0);
        l2.setMargins(Ui.dp(this, 6), 0, 0, 0);
        btns.addView(g, l1);
        btns.addView(m, l2);
        LinearLayout.LayoutParams bx = Ui.match();
        bx.setMargins(0, 0, 0, Ui.dp(this, 12));
        content.addView(btns, bx);

        if (b > 0) {
            final String name = p.optString("name");
            final long owed = b;
            content.addView(big(L.t("remind"), 0xFF25D366, Color.WHITE, v -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, String.format(Locale.US, L.t("remind_msg"), name, money(owed)));
                startActivity(Intent.createChooser(i, L.t("remind")));
            }));
        }

        if (tx.length() == 0) content.addView(empty(L.t("no_tx")));
        for (int i = tx.length() - 1; i >= 0; i--) {
            final JSONObject t = tx.optJSONObject(i);
            boolean gave = t.optString("type").equals("gave");
            LinearLayout card = Ui.card(this);
            LinearLayout row = Ui.row(this);
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            String note = t.optString("note");
            left.addView(Ui.text(this, L.t(gave ? "gave" : "got") + (note.isEmpty() ? "" : " - " + note), 16, INK, true));
            left.addView(Ui.text(this, day(t.optLong("ts")), 13, MUTED, false));
            row.addView(left, Ui.weight(1));
            row.addView(Ui.text(this, "₹" + money(t.optLong("amt")), 17, gave ? RED : GREEN, true));
            TextView x = cross();
            x.setOnClickListener(v -> ask(L.t("sure"), () -> { remove(tx, t); st.save(); render(); }));
            row.addView(x);
            card.addView(row);
            content.addView(card);
        }

        Button del = Ui.button(this, L.t("del_person"), GREY, RED, v -> ask(L.t("sure"), () -> {
            ppl.remove(idx);
            st.save();
            openPerson = -1;
            render();
        }));
        LinearLayout.LayoutParams dl = Ui.match();
        dl.setMargins(0, Ui.dp(this, 14), 0, 0);
        content.addView(del, dl);
    }

    void addTx(final JSONObject p, final String type) {
        amountDialog(L.t(type), (amt, note) -> {
            try {
                JSONObject o = new JSONObject();
                o.put("type", type);
                o.put("amt", amt);
                o.put("note", note);
                o.put("ts", System.currentTimeMillis());
                p.optJSONArray("tx").put(o);
            } catch (JSONException ignored) { }
            st.save();
            render();
        });
    }
}
