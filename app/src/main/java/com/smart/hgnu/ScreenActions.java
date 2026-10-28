package com.smart.hgnu;

import org.json.JSONObject;

/** Pages emit actions; the activity/controller own navigation and network work. */
interface ScreenActions {
    void redraw();

    void navigateTo(Page page);

    void refresh();

    void selectSemester(JSONObject semester);

    void signIn(String account, String password);

    void signOut();
}
