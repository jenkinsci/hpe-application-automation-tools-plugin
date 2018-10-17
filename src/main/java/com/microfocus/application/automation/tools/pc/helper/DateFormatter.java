package com.microfocus.application.automation.tools.pc.helper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatter {

    public static final String DEFAULT_PATTERN = "E yyyy MMM dd 'at' HH:mm:ss.SSS a zzz";

    private SimpleDateFormat simpleDateFormat;
    private String pattern;
    private String date;

    public DateFormatter(String pattern){
        this.pattern = pattern.isEmpty() ? DEFAULT_PATTERN : pattern;
        simpleDateFormat = new SimpleDateFormat (this.pattern);
        renewDate();
    }

    public void renewDate() {
        try {
            date = simpleDateFormat.format(new Date());
        }
        catch (Exception ex) {
            this.pattern = DEFAULT_PATTERN;
            simpleDateFormat = new SimpleDateFormat (this.pattern);
            date = simpleDateFormat.format(new Date());
        }
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern.isEmpty() ? DEFAULT_PATTERN : pattern;
        simpleDateFormat = new SimpleDateFormat (this.pattern);
    }

    public String getDate(){
        try {
            renewDate();
            return date;
        }
        catch (Exception ex)
        {
            return "";
        }
    }

}
