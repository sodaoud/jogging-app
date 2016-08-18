package com.toptal.joggingtracking.util;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import com.google.gson.Gson;
import com.toptal.joggingtracking.datatype.TokenUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by sofiane on 8/18/16.
 */

public class Util {

    public static final String HOST = "192.168.1.35";
    public static final int PORT = 8080;
    public static final String SEGMENT_LOGIN = "login";
    public static final String SEGMENT_TRACK = "track";
    public static final String BASE_URL = "http://" + HOST + ":" + PORT;
    public static final String URL_LOGIN = BASE_URL + "/" + SEGMENT_LOGIN;
    public static final String URL_SIGN_UP = BASE_URL + "/signup";
    public static final String URL_TRACK = BASE_URL + "/" + SEGMENT_TRACK;
    public static final String USER_PREF = "USER_PREF";
    public static final String ACCOUNT = "ACCOUNT";

    public static final String ACCOUNT_TYPE = "com.toptal.jogtrack";
    public static final String ROLES = "ROLES";
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String USER = "USER";
    public static final int FAIL = -12;
    public static final int SUCCES = -13;

    private static Account account;
    private static AccountManager am;

    public static void getNewAuthToken(final Handler handler) {
        OkHttpClient client = new OkHttpClient();
        String username = account.name;
        String password = am.getPassword(account);
        JSONObject obj = new JSONObject();
        try {
            obj.put("username", username);
            obj.put("password", password);
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), obj.toString());
            Request request = new Request.Builder()
                    .url(Util.URL_LOGIN)
                    .post(body)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Message m = new Message();
                    m.what = FAIL;
                    handler.dispatchMessage(m);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response != null) {
                        ResponseBody body = response.body();
                        if (response.code() == 200) {
                            try {
                                Gson gson = new Gson();
                                TokenUtil tu = gson.fromJson(body.string(), TokenUtil.class);
                                setToken(tu);

                                Message m = new Message();
                                m.what = SUCCES;
                                handler.dispatchMessage(m);
                                return;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    Message m = new Message();
                    m.what = FAIL;
                    handler.dispatchMessage(m);
                }
            });

        } catch (JSONException e) {
            Message m = new Message();
            m.what = FAIL;
            handler.dispatchMessage(m);
        }
    }

    public static Account getAccount(Context ctx) {
        if (account == null) {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            Account[] accounts = getAccountManager(ctx).getAccountsByType(ACCOUNT_TYPE);
            if (accounts.length > 0) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                String username = prefs.getString(USER_PREF, null);
                if (username != null) {
                    for (Account ac : accounts) {
                        if (username.equals(ac.name)) {
                            account = ac;
                            break;
                        }
                    }
                }
                if (account == null) {
                    account = accounts[0];
                    prefs.edit().putString(USER_PREF, account.name).apply();
                }
            }
        }

        return account;
    }

    private static AccountManager getAccountManager(Context ctx) {
        if (am == null)
            am = AccountManager.get(ctx);
        return am;
    }


    private static void setToken(TokenUtil tu) {

        am.setUserData(account, Util.ADMIN, null);
        am.setUserData(account, Util.MANAGER, null);
        am.setUserData(account, Util.USER, null);
        for (String role : tu.getRoles()) {
            if (role.equals(Util.ADMIN)) am.setUserData(account, Util.ADMIN, "admin");
            if (role.equals(Util.MANAGER)) am.setUserData(account, Util.MANAGER, "manager");
            if (role.equals(Util.USER)) am.setUserData(account, Util.USER, "user");
        }
        am.setAuthToken(account, "Bearer", tu.getToken());

    }

    public static Bundle finishLogin(Context ctx, TokenUtil tu, String mUsername, String mPassword) {
        final Account account = new Account(mUsername, Util.ACCOUNT_TYPE);

        getAccountManager(ctx).addAccountExplicitly(account, mPassword, getRolesBundle(tu.getRoles()));
        getAccountManager(ctx).setAuthToken(account, "Bearer", tu.getToken());

        Bundle mResultBundle = new Bundle();
        mResultBundle.putString(AccountManager.KEY_ACCOUNT_NAME, mUsername);
        mResultBundle.putString(AccountManager.KEY_AUTHTOKEN, tu.getToken());

        return mResultBundle;
    }

    private static Bundle getRolesBundle(String[] roles) {
        Bundle b = new Bundle();
        for (String role : roles) {
            if (role.equals(Util.ADMIN)) b.putString(Util.ADMIN, "admin");
            if (role.equals(Util.MANAGER)) b.putString(Util.MANAGER, "manager");
            if (role.equals(Util.USER)) b.putString(Util.USER, "user");
        }
        return b;
    }

}
