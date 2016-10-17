package io.github.sodaoud.joggingapp.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by sofiane on 8/14/16.
 */

public class AuthService  extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return new Authenticator(this).getIBinder();
    }
}