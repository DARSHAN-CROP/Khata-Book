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
