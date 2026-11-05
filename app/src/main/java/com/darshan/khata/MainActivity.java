package com.darshan.khata;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
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
    static final String[] CATS = {"food", "travel", "shop", "bill", "health", "other"};
    long hiddenAt = 0;
    boolean lockShown = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        st = new Store(this);
        Sync.handleIntent(this, getIntent());
        Sync.onChanged = () -> { st = new Store(MainActivity.this); L.gu = st.lang().equals("gu"); render(); };
        L.gu = st.lang().equals("gu");
        tab = getIntent().getIntExtra("tab", 0);
        Reminders.schedule(this);
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
        lockIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sync.onChanged = () -> { st = new Store(MainActivity.this); L.gu = st.lang().equals("gu"); render(); };
        if (Sync.signedIn(this)) Sync.syncNow(getApplicationContext());
        if (hiddenAt > 0 && System.currentTimeMillis() - hiddenAt > 60000) lockIfNeeded();
    }

    @Override
    protected void onStop() {
        super.onStop();
        hiddenAt = System.currentTimeMillis();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (Sync.handleIntent(this, intent)) {
            Toast.makeText(this, L.t("sync_signed"), Toast.LENGTH_LONG).show();
            tab = 3;
            Sync.syncNow(getApplicationContext());
            render();
        } else if (intent.hasExtra("tab")) {
            tab = intent.getIntExtra("tab", 0);
            openPerson = -1;
            render();
        }
    }

    // ---------------------------------------------------------------- PIN lock

    static String pinHash(String pin) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(("khata:" + pin).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : h) sb.append(String.format(Locale.US, "%02x", x));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    String savedPin() { return getSharedPreferences("lock", MODE_PRIVATE).getString("pin", ""); }

    void lockIfNeeded() {
        final String h = savedPin();
        if (h.isEmpty() || lockShown) return;
        lockShown = true;
        final EditText e = field(L.t("pin_enter"), false);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        LinearLayout box = dialogBox();
        box.addView(e);
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle(L.t("pin_title")).setView(box).setCancelable(false)
                .setPositiveButton(L.t("pin_open"), null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (pinHash(e.getText().toString()).equals(h)) { lockShown = false; dlg.dismiss(); }
            else { e.setText(""); Toast.makeText(this, L.t("pin_wrong"), Toast.LENGTH_SHORT).show(); }
        }));
        dlg.show();
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
        String[] keys = {"tab_ledger", "tab_credit", "tab_report", "tab_set"};
        for (int i = 0; i < keys.length; i++) {
            final int idx = i;
            boolean sel = i == tab;
            Button x = Ui.button(this, L.t(keys[i]), sel ? TEAL : Color.TRANSPARENT, sel ? Color.WHITE : MUTED, v -> { tab = idx; openPerson = -1; render(); });
            x.setTextSize(12);
            x.setMaxLines(1);
            LinearLayout.LayoutParams lp = Ui.weight(1);
            lp.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
            nav.addView(x, lp);
        }
        content.removeAllViews();
        if (tab == 0) ledger();
        else if (tab == 2) report();
        else if (tab == 3) settings();
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
            String cname = o.has("cat") ? L.t("cat_" + o.optString("cat")) + " - " : "";
            left.addView(Ui.text(this, cname + day(o.optLong("ts")), 13, MUTED, false));
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
        if (type.equals("out")) {
            String[] names = new String[CATS.length];
            for (int i = 0; i < CATS.length; i++) names[i] = L.t("cat_" + CATS[i]);
            new AlertDialog.Builder(this).setTitle(L.t("pick_cat")).setItems(names, (d, which) -> addEntry2(type, CATS[which])).show();
        } else addEntry2(type, null);
    }

    void addEntry2(final String type, final String cat) {
        amountDialog(L.t(type.equals("in") ? "add_income" : "add_expense"), (p, note) -> {
            try {
                JSONObject o = new JSONObject();
                o.put("type", type);
                o.put("amt", p);
                o.put("note", note);
                if (cat != null) o.put("cat", cat);
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

        int dn = Reminders.dueCount(st);
        if (dn > 0) {
            LinearLayout bn = Ui.card(this);
            bn.setBackgroundColor(0xFFFFF4E5);
            bn.addView(Ui.text(this, String.format(Locale.US, L.t("dues_title"), dn), 15, INK, true));
            content.addView(bn);
        }

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

        content.addView(big(L.t("stmt_share"), GREY, INK, v -> {
            try { Statement.share(this, Statement.build(this, p), "application/pdf", L.t("stmt_share")); }
            catch (Exception e) { Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        }));

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
            long dueTs = t.optLong("due", 0);
            if (gave && dueTs > 0) {
                boolean late = dueTs < System.currentTimeMillis() - 86400000L + 1 && b > 0 && dueTs <= Reminders.endOfToday() && dueTs < startOfToday();
                left.addView(Ui.text(this, late ? L.t("late") + " - " + String.format(Locale.US, L.t("due_on"), day(dueTs)) : String.format(Locale.US, L.t("due_on"), day(dueTs)), 13, late ? RED : TEAL, true));
            }
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

    static long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    void addTx(final JSONObject p, final String type) {
        amountDialog(L.t(type), (amt, note) -> {
            final JSONObject o = new JSONObject();
            try {
                o.put("type", type);
                o.put("amt", amt);
                o.put("note", note);
                o.put("ts", System.currentTimeMillis());
            } catch (JSONException ignored) { }
            if (type.equals("gave")) {
                new AlertDialog.Builder(this).setMessage(L.t("due_q")).setCancelable(false)
                        .setPositiveButton(L.t("due_pick"), (d, w) -> {
                            Calendar c = Calendar.getInstance();
                            c.add(Calendar.DAY_OF_MONTH, 7);
                            new DatePickerDialog(this, (dp, yy, mm, dd) -> {
                                Calendar x = Calendar.getInstance();
                                x.set(yy, mm, dd, 9, 0, 0);
                                x.set(Calendar.MILLISECOND, 0);
                                try { o.put("due", x.getTimeInMillis()); } catch (JSONException ignored) { }
                                finishTx(p, o);
                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
                        })
                        .setNegativeButton(L.t("due_no"), (d, w) -> finishTx(p, o)).show();
            } else finishTx(p, o);
        });
    }

    void finishTx(JSONObject p, JSONObject o) {
        p.optJSONArray("tx").put(o);
        st.save();
        render();
    }

    // ---------------------------------------------------------------- report

    View bar(float frac, int color) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setBackgroundColor(0xFFE6E9E9);
        View f = new View(this);
        f.setBackgroundColor(color);
        l.addView(f, new LinearLayout.LayoutParams(0, Ui.dp(this, 10), Math.max(frac, 0.001f)));
        l.addView(new View(this), new LinearLayout.LayoutParams(0, Ui.dp(this, 10), Math.max(1f - frac, 0.001f)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 10));
        lp.setMargins(0, Ui.dp(this, 3), 0, 0);
        l.setLayoutParams(lp);
        return l;
    }

    static boolean monthOf(long ts, int back) {
        Calendar a = Calendar.getInstance(), b = Calendar.getInstance();
        a.set(Calendar.DAY_OF_MONTH, 1);
        a.add(Calendar.MONTH, -back);
        b.setTimeInMillis(ts);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.MONTH) == b.get(Calendar.MONTH);
    }

    void report() {
        JSONArray a = st.arr("entries");
        long[] by = new long[CATS.length];
        long[] in = new long[6], out = new long[6];
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            long ts = o.optLong("ts"), amt = o.optLong("amt");
            boolean isIn = o.optString("type").equals("in");
            for (int k = 0; k < 6; k++) if (monthOf(ts, k)) { if (isIn) in[k] += amt; else out[k] += amt; }
            if (!isIn && monthOf(ts, 0)) {
                String c = o.optString("cat", "other");
                int idx = CATS.length - 1;
                for (int k = 0; k < CATS.length; k++) if (CATS[k].equals(c)) idx = k;
                by[idx] += amt;
            }
        }
        LinearLayout c1 = Ui.card(this);
        c1.addView(Ui.text(this, L.t("rep_cat"), 17, INK, true));
        long mx = 1;
        for (long v : by) mx = Math.max(mx, v);
        boolean any = false;
        for (int k = 0; k < CATS.length; k++) {
            if (by[k] == 0) continue;
            any = true;
            LinearLayout row = Ui.row(this);
            row.setPadding(0, Ui.dp(this, 8), 0, 0);
            row.addView(Ui.text(this, L.t("cat_" + CATS[k]), 14, INK, false), Ui.weight(1));
            row.addView(Ui.text(this, "₹" + money(by[k]), 14, RED, true));
            c1.addView(row);
            c1.addView(bar(by[k] / (float) mx, RED));
        }
        if (!any) c1.addView(Ui.text(this, L.t("no_data"), 14, MUTED, false));
        content.addView(c1);

        LinearLayout c2 = Ui.card(this);
        c2.addView(Ui.text(this, L.t("rep_6"), 17, INK, true));
        long m2 = 1;
        for (int k = 0; k < 6; k++) m2 = Math.max(m2, Math.max(in[k], out[k]));
        for (int k = 5; k >= 0; k--) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, -k);
            TextView t = Ui.text(this, new SimpleDateFormat("MMM yyyy", Locale.US).format(cal.getTime()) + "   ₹" + money(in[k]) + " / ₹" + money(out[k]), 13, MUTED, false);
            t.setPadding(0, Ui.dp(this, 10), 0, 0);
            c2.addView(t);
            c2.addView(bar(in[k] / (float) m2, GREEN));
            c2.addView(bar(out[k] / (float) m2, RED));
        }
        content.addView(c2);
    }

    // ---------------------------------------------------------------- settings

    void settings() {
        // sync
        LinearLayout c0 = Ui.card(this);
        c0.addView(Ui.text(this, L.t("sync_title"), 17, INK, true));
        boolean in = Sync.signedIn(this);
        c0.addView(Ui.text(this, in ? String.format(Locale.US, L.t("sync_on"), Sync.email(this)) : L.t("sync_off"), 14, MUTED, false));
        if (in && !Sync.status(this).isEmpty()) c0.addView(Ui.text(this, Sync.status(this), 13, MUTED, false));
        if (!in) c0.addView(sp(Ui.button(this, L.t("sync_signin"), TEAL, Color.WHITE, v -> Sync.startLogin(this))));
        else {
            c0.addView(sp(Ui.button(this, L.t("sync_now"), TEAL, Color.WHITE, v -> {
                Toast.makeText(this, L.t("sync_wait"), Toast.LENGTH_SHORT).show();
                Sync.syncNow(getApplicationContext());
            })));
            c0.addView(sp(Ui.button(this, L.t("sync_out"), GREY, INK, v -> { Sync.signOut(this); render(); })));
        }
        content.addView(c0);

        // PIN
        LinearLayout c1 = Ui.card(this);
        c1.addView(Ui.text(this, L.t("pin_title"), 17, INK, true));
        if (savedPin().isEmpty()) {
            c1.addView(sp(Ui.button(this, L.t("pin_set"), TEAL, Color.WHITE, v -> setPinDialog())));
        } else {
            c1.addView(Ui.text(this, L.t("pin_on"), 14, MUTED, false));
            c1.addView(sp(Ui.button(this, L.t("pin_remove"), GREY, INK, v -> {
                getSharedPreferences("lock", MODE_PRIVATE).edit().remove("pin").apply();
                render();
            })));
        }
        content.addView(c1);

        // reminders
        LinearLayout c2 = Ui.card(this);
        c2.addView(Ui.text(this, L.t("rem_title"), 17, INK, true));
        c2.addView(Ui.text(this, L.t("rem_info"), 14, MUTED, false));
        c2.addView(sp(Ui.button(this, L.t("rem_perm"), GREY, INK, v -> {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 5);
        })));
        content.addView(c2);

        // backup
        LinearLayout c3 = Ui.card(this);
        c3.addView(sp(Ui.button(this, L.t("backup"), GREY, INK, v -> {
            try {
                File dir = new File(getCacheDir(), "statements");
                dir.mkdirs();
                File f = new File(dir, "khata-backup.json");
                try (FileOutputStream o = new FileOutputStream(f)) { o.write(Sync.toWeb(st).toString().getBytes("UTF-8")); }
                Statement.share(this, f, "application/json", L.t("backup"));
            } catch (Exception e) { Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        })));
        content.addView(c3);
    }

    View sp(Button b) {
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, Ui.dp(this, 8), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    void setPinDialog() {
        final EditText e = field(L.t("pin_set"), false);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        LinearLayout box = dialogBox();
        box.addView(e);
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle(L.t("pin_set")).setView(box)
                .setPositiveButton(L.t("save"), null).setNegativeButton(L.t("cancel"), null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = e.getText().toString();
            if (!s.matches("\\d{4,8}")) { Toast.makeText(this, L.t("pin_bad"), Toast.LENGTH_SHORT).show(); return; }
            getSharedPreferences("lock", MODE_PRIVATE).edit().putString("pin", pinHash(s)).apply();
            dlg.dismiss();
            render();
        }));
        dlg.show();
    }
}
