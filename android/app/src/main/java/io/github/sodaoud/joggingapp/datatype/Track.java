package io.github.sodaoud.joggingapp.datatype;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Track implements Serializable {

    final static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
    private Date date;
    private int duration;
    private int distance;
    private String userid;
    private String id;

    public Track() {
    }

    public Track(int duration, int distance) {
        this.duration = duration;
        this.distance = distance;
    }

    public Track(Date date, int duration, int distance) {
        this.date = date;
        this.duration = duration;
        this.distance = distance;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFormatedDuration() {
        StringBuilder b = new StringBuilder();
        int h = getNumOfHours();
        int m = getNumOfMinutes();
        int s = getNumOfSeconds();
        if (h < 10) {
            b.append("0");
        }
        b.append(h).append(":");
        if (m < 10) {
            b.append("0");
        }
        b.append(m).append(":");
        if (s < 10) {
            b.append("0");
        }
        b.append(s);
        return b.toString();
    }

    public int getNumOfHours() {
        return duration / 3600;
    }

    public int getNumOfMinutes() {
        return (duration - getNumOfHours() * 3600) / 60;
    }

    public int getNumOfSeconds() {
        return duration - getNumOfHours() * 3600 - getNumOfMinutes() * 60;
    }
//
//    public String getFormatedDistance() {
//        StringBuilder b = new StringBuilder();
//        int km = getNumOfKm();
//        int dm = getNumOfDm();
//        b.append(km).append(".");
//        if (dm < 10) {
//            b.append("0");
//        }
//        b.append(dm).append(" Km");
//        return b.toString();
//    }
//
//
//    public int getNumOfKm() {
//        return distance / 1000;
//    }
//
//    public int getNumOfDm() {
//        return (distance - getNumOfKm() * 1000) / 10;
//    }
//
//    public String getFormatedSpeed() {
//        //DecimalFormat.getInstance().format(((double)distance * 3.6) / duration);
//        return DecimalFormat.getInstance().format(((double)distance * 3.6) / duration) + " Km/h";
//    }
    public String getFormatedDate() {
        return sdf.format(date);
    }

}
