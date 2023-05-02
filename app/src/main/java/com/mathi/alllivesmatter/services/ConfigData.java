package com.mathi.alllivesmatter.services;

import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.SystemProviders;


public class ConfigData {

    // default setting for booleans
    public static final boolean DEFAULT_UNREAD_NOTIFICATION = true;
    public static final boolean DEFAULT_24_HOUR_TIME = false;
    public static final boolean DEFAULT_SECONDS_TICK_ENABLE = true;
    public static final boolean DEFAULT_NOTIFICATION_COMPLICATION = false;
    public static final boolean DEFAULT_HIDE_COMPLICATIONS_AMBIENT = false;
    public static final boolean DEFAULT_SHOW_HANDS = true;
    // https://developer.android.com/reference/android/support/wearable/complications/SystemProviders
    public static final int[] DEFAULT_LEFT_COMPLICATION = {SystemProviders.WATCH_BATTERY, ComplicationData.TYPE_RANGED_VALUE};
    public static final int[] DEFAULT_RIGHT_COMPLICATION = {SystemProviders.STEP_COUNT, ComplicationData.TYPE_SHORT_TEXT};
    public static final int[] DEFAULT_TOP_COMPLICATION = {SystemProviders.DATE, ComplicationData.TYPE_SHORT_TEXT};
    public static final int[] DEFAULT_BOTTOM_COMPLICATION = {SystemProviders.NEXT_EVENT, ComplicationData.TYPE_LONG_TEXT};
    //public static final int[] DEFAULT_NOTIFICATION_COMPLICATION = {SystemProviders.UNREAD_NOTIFICATION_COUNT,  ComplicationData.TYPE_LONG_TEXT};


}
