package com.darshan.khata;

import java.util.HashMap;
import java.util.Map;

/** Gujarati + English texts. */
final class L {
    private L() {}
    static boolean gu = true;
    private static final Map<String, String[]> M = new HashMap<>();
    private static void p(String k, String g, String e) { M.put(k, new String[]{g, e}); }
    static {
        p("tab_report", "રિપોર્ટ", "Report");
        p("tab_set", "સેટિંગ", "Settings");
        p("cat_food", "ખાવાનું", "Food");
        p("cat_travel", "મુસાફરી", "Travel");
        p("cat_shop", "ખરીદી", "Shopping");
        p("cat_bill", "બિલ", "Bills");
        p("cat_health", "દવા", "Health");
        p("cat_other", "બીજું", "Other");
        p("pick_cat", "ખર્ચની કેટેગરી પસંદ કરો", "Pick a category");
        p("due_q", "પાછા આપવાની તારીખ મૂકવી છે?", "Set a due date?");
        p("due_pick", "તારીખ પસંદ કરો", "Pick date");
        p("due_no", "ના", "No");
        p("due_on", "મુદત: %s", "Due: %s");
        p("late", "મુદત વીતી ગઈ", "Overdue");
        p("dues_title", "⏰ આજે / મુદત વીતેલા ઉધાર: %d", "⏰ Due today / overdue: %d");
        p("stmt", "હિસાબ", "Statement");
        p("stmt_share", "હિસાબ PDF શેર કરો", "Share statement PDF");
        p("rep_cat", "ખર્ચ કેટેગરી પ્રમાણે (આ મહિનો)", "Expenses by category (this month)");
        p("rep_6", "છેલ્લા 6 મહિના (આવક / ખર્ચ)", "Last 6 months (income / expense)");
        p("no_data", "હજી ડેટા નથી", "No data yet");
        p("pin_title", "PIN lock", "PIN lock");
        p("pin_set", "PIN મૂકો", "Set PIN");
        p("pin_enter", "PIN લખો", "Enter PIN");
        p("pin_remove", "PIN કાઢો", "Remove PIN");
        p("pin_on", "PIN ચાલુ છે", "PIN is on");
        p("pin_bad", "PIN 4 થી 8 અંકનો હોવો જોઈએ", "PIN must be 4 to 8 digits");
        p("pin_wrong", "ખોટો PIN", "Wrong PIN");
        p("pin_open", "ખોલો", "Unlock");
        p("rem_title", "ઉધાર રિમાઇન્ડર", "Credit reminders");
        p("rem_info", "રોજ સવારે 9 વાગ્યે મુદતવાળા ઉધારની સૂચના આવશે.", "A notice comes every day at 9 AM for dues.");
        p("rem_perm", "સૂચનાની પરવાનગી આપો", "Allow notifications");
        p("notif_text", "%d ઉધારની મુદત આવી ગઈ છે", "%d credit dues have come");
        p("sync_title", "Google sync", "Google sync");
        p("sync_on", "જોડાયેલ: %s", "Signed in: %s");
        p("sync_off", "Google થી sign in કરો તો ફોન અને PC પર એક જ ડેટા દેખાશે.", "Sign in with Google to see the same data on phone and PC.");
        p("sync_signin", "Google થી sign in", "Sign in with Google");
        p("sync_now", "હમણાં sync કરો", "Sync now");
        p("sync_out", "Sign out", "Sign out");
        p("sync_wait", "sync થઈ રહ્યું છે…", "Syncing…");
        p("sync_signed", "Sign in થઈ ગયું ✅", "Signed in ✅");
        p("backup", "Backup JSON શેર કરો", "Share backup JSON");
        p("app", "ખાતાવહી", "Khata Book");
        p("tab_ledger", "હિસાબ", "Ledger");
        p("tab_credit", "ઉધાર", "Credit");
        p("lang_btn", "English", "ગુજરાતી");
        p("cancel", "રદ કરો", "Cancel");
        p("save", "સાચવો", "Save");
        p("yes", "હા", "Yes");
        p("sure", "કાઢી નાખવું છે?", "Delete this?");
        p("income", "આવક", "Income");
        p("expense", "ખર્ચ", "Expense");
        p("balance", "બાકી", "Balance");
        p("this_month", "આ મહિનો", "This month");
        p("add_income", "આવક ઉમેરો", "Add income");
        p("add_expense", "ખર્ચ ઉમેરો", "Add expense");
        p("amount", "રકમ (રૂપિયા)", "Amount (rupees)");
        p("note", "નોંધ (વૈકલ્પિક)", "Note (optional)");
        p("need_amount", "સાચી રકમ લખો", "Enter a valid amount");
        p("need_name", "નામ લખો", "Enter a name");
        p("no_entries", "હજી કોઈ એન્ટ્રી નથી. ઉપરથી આવક કે ખર્ચ ઉમેરો.", "No entries yet. Add income or expense above.");
        p("add_person", "વ્યક્તિ ઉમેરો", "Add person");
        p("person", "નામ", "Name");
        p("no_people", "હજી કોઈ વ્યક્તિ નથી. ઉપરથી ઉમેરો.", "No people yet. Add one above.");
        p("gave", "દીધા", "I gave");
        p("got", "મળ્યા", "I received");
        p("owe_me", "તમને મળવાના: ₹%s", "To receive: ₹%s");
        p("i_owe", "તમારે આપવાના: ₹%s", "To give: ₹%s");
        p("settled", "હિસાબ ચૂકતે", "All settled");
        p("total_recv", "કુલ મળવાના", "Total to receive");
        p("total_give", "કુલ આપવાના", "Total to give");
        p("remind", "WhatsApp પર યાદ અપાવો", "Remind on WhatsApp");
        p("remind_msg", "નમસ્તે %s, તમારી પાસેથી ₹%s લેવાના બાકી છે. કૃપા કરીને ચૂકવી દેજો. આભાર!", "Hi %s, ₹%s is pending from you. Please pay when you can. Thanks!");
        p("back", "← પાછા", "← Back");
        p("del_person", "આ વ્યક્તિ કાઢી નાખો", "Delete this person");
        p("no_tx", "હજી કોઈ લેવડદેવડ નથી.", "No transactions yet.");
        p("income_label", "આવક", "Income");
        p("expense_label", "ખર્ચ", "Expense");
    }
    static String t(String k) {
        String[] v = M.get(k);
        return v == null ? k : v[gu ? 0 : 1];
    }
}
