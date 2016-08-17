package com.toptal.joggingtracking.datatype;

import java.util.Date;

/**
 * Created by sofiane on 8/14/16.
 */

public class Track {
    private Date date;
    private int duration;
    private float speed;
    private int distance;
    private String userid;
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

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

}
