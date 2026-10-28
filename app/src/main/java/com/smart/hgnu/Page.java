package com.smart.hgnu;

/** Order also defines the bottom-navigation icon and saved page index. */
enum Page {
    HOME("校园"),
    TIMETABLE("课表"),
    SERVICES("服务"),
    PROFILE("我的");
    final String label;

    Page(String label) {
        this.label = label;
    }

    static Page fromSavedIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : HOME;
    }
}
