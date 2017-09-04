package com.nmerris.roboresumedb;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utilities {

    // returns 'Present' if date is null, otherwise returns MMM dd, yyyy date string
    public static String getMonthDayYearFromDate(Date date) {
        String dateString = "";

        try {
            SimpleDateFormat dateFormatMonth = new SimpleDateFormat("MMM");
            dateString += (dateFormatMonth.format(date) + " ");
            SimpleDateFormat dateFormatDay = new SimpleDateFormat("dd");
            dateString += (dateFormatDay.format(date) + ", ");
            SimpleDateFormat dateFormatYear = new SimpleDateFormat("yyyy");
            dateString += (dateFormatYear.format(date));
        } catch (Exception e) {
            // this should only happen if date is null, because if anything was entered by user, it would have been validated otherwise
            // if end date was left empty by user
            dateString += "Present";
        }

        return dateString;
    }
    

}
