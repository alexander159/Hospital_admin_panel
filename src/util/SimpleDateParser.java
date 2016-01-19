package util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleDateParser {
    private static final String[] DATE_PATTERN = {"(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/((19|20)\\d\\d)",  //like 30/06/2010, 30/6/2010 OR like 2014-04-29, 2014-4-29
            "((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])"};
    private static final String[] DATE_FORMAT = {"dd/MM/yyyy", "yyyy-MM-dd"};

    /**
     * Parse date using formats "dd/MM/yyyy", "yyyy-MM-dd". The output value depends on your timezone.
     *
     * @param date String value of date
     * @return java.util.Date value in case of success parsing, NULL otherwise
     */
    public static Date parseDate(final String date) {
        Date res = null;
        if (date != null) {
            for (String parse : DATE_FORMAT) {
                SimpleDateFormat sdf = new SimpleDateFormat(parse);
                try {
                    res = sdf.parse(date);
                    break;
                } catch (ParseException ignore) {
                }
            }
        }
        return res;
    }


    /**
     * Validate date using pattern like "30/06/2010", "30/6/2010" OR like "2014-04-29", "2014-4-29"
     *
     * @param date      String value of date
     * @param patternId index of DATE_PATTERN
     * @return true if date matches pattern, false otherwise
     */
    public static boolean validateDate(final String date, int patternId) {
        Pattern pattern = Pattern.compile(DATE_PATTERN[patternId]);
        Matcher matcher;

        matcher = pattern.matcher(date);
        if (matcher.matches()) {
            matcher.reset();
            return matcher.find();
        } else {
            return false;
        }
    }

    public static SimpleDate parse(final String date) {
        if (validateDate(date, 0)) {
            return parse(date, 0);
        } else if (validateDate(date, 1)) {
            return parse(date, 1);
        } else {
            return null;
        }
    }

    /**
     * Parse date using pattern like "30/06/2010", "30/6/2010" OR like "2014-04-29", "2014-4-29"
     *
     * @param date      String value of date
     * @param patternId index of DATE_PATTERN
     * @return SimpleDate in case of success parsing, NULL otherwise
     */
    public static SimpleDate parse(final String date, int patternId) {
        Pattern pattern = Pattern.compile(DATE_PATTERN[patternId]);
        Matcher matcher;

        matcher = pattern.matcher(date);
        if (matcher.matches()) {
            matcher.reset();
            if (matcher.find()) {
                if (patternId == 0) {
                    String[] dateArray = date.split("/");
                    return new SimpleDate(Integer.parseInt(dateArray[0]), Integer.parseInt(dateArray[1]), Integer.parseInt(dateArray[2]));
                    //return new SimpleDate(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
                } else if (patternId == 1) {
                    String[] dateArray = date.split("-");
                    return new SimpleDate(Integer.parseInt(dateArray[2]), Integer.parseInt(dateArray[1]), Integer.parseInt(dateArray[0]));
                    //return new SimpleDate(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(1)));
                } else {
                    return null;
                }

            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static class SimpleDate {
        private int day;
        private int month;
        private int year;

        public SimpleDate(int day, int month, int year) {
            this.day = day;
            this.month = month;
            this.year = year;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }
    }
}
