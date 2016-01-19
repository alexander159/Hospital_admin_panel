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

    public static Date parseDate(final String d) {
        Date date = null;
        if (d != null) {
            for (String parse : DATE_FORMAT) {
                SimpleDateFormat sdf = new SimpleDateFormat(parse);
                try {
                    date = sdf.parse(d);
                    break;
                } catch (ParseException ignore) {
                }
            }
        }
        return date;
    }

    public static boolean validateDate(final String date, int patternId) {
        Pattern pattern = Pattern.compile(DATE_PATTERN[patternId]);
        Matcher matcher;

        matcher = pattern.matcher(date);
        if (matcher.matches()) {
            matcher.reset();
            if (matcher.find()) {
                /*if (patternId == 0) {
                    String day = matcher.group(1);
                    String month = matcher.group(2);
                    int year = Integer.parseInt(matcher.group(3));
                } else if (patternId == 1){
                    int year = Integer.parseInt(matcher.group(1));
                    String month = matcher.group(2);
                    String day = matcher.group(3);
                }*/
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static int getYear(final String date, int patternId) {
        Pattern pattern = Pattern.compile(DATE_PATTERN[patternId]);
        Matcher matcher;

        matcher = pattern.matcher(date);
        if (matcher.matches()) {
            matcher.reset();
            if (matcher.find()) {
                if (patternId == 0) {
                    return Integer.parseInt(matcher.group(3));
                } else if (patternId == 1) {
                    return Integer.parseInt(matcher.group(1));
                } else {
                    return -1;
                }

            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }
}
