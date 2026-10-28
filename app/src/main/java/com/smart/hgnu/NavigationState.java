package com.smart.hgnu;

/** UI selections only. Credentials and server data do not belong here. */
final class NavigationState {
    Page selectedPage = Page.HOME;
    int selectedWeekday = -1; // -1 = default to today; 0 = all; 1..7 = Monday..Sunday.
    String serviceQuery = "";
    int selectedWeek;
    String timetableSemester = "";
    boolean timetableTableMode = true;
}
