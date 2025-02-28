package com.xiaozhi.ultis;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtil {

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public static String dayOfMonthStart() {
        // 本月起始
        Calendar thisMonthFirstDateCal = Calendar.getInstance();
        // 获取上月
        // thisMonthFirstDateCal.add(Calendar.MONTH, -1);
        thisMonthFirstDateCal.set(Calendar.DAY_OF_MONTH, thisMonthFirstDateCal.getActualMinimum(Calendar.DAY_OF_MONTH));
        String thisMonthFirstTime = format.format(thisMonthFirstDateCal.getTime()) + " 00:00:00";
        return thisMonthFirstTime;
    }

    public static String dayOfMonthEnd() {
        Calendar thisMonthEndDateCal = Calendar.getInstance();
        // 获取上月
        // thisMonthEndDateCal.add(Calendar.MONTH, -1);
        thisMonthEndDateCal.set(Calendar.DAY_OF_MONTH, thisMonthEndDateCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        String thisMonthEndTime = format.format(thisMonthEndDateCal.getTime()) + " 23:59:59";
        return thisMonthEndTime;
    }
}