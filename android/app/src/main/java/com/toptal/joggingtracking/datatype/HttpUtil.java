package com.toptal.joggingtracking.datatype;

import java.io.IOException;

import okhttp3.Response;

/**
 * Created by sofiane on 8/22/16.
 */

public class HttpUtil {
    public int code;
    public String body;
    private Body b;

    public HttpUtil(Response r){
        code = r.code();
        b=new Body();
        try {
            body=r.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class Body{
        public String string(){
            return body;
        }
    }

    public Body body(){
        return b;
    }

    public int code(){
        return code;
    }
}
