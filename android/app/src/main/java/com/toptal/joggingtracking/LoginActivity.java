package com.toptal.joggingtracking;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.toptal.joggingtracking.auth.AccountGeneral;
import com.toptal.joggingtracking.datatype.TokenUtil;
import com.toptal.joggingtracking.util.ConstantUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String ACCOUNT_TYPE = "ACCOUNT_TYPE";
    //    public static final String ADDING_NEW_ACCOUNT = "ADDING_NEW_ACCOUNT";
    public static final String AUTH_TYPE = "AUTH_TYPE";
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private OkHttpClient client;
    private AccountManager mAccountManager;
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        mUsernameView = (EditText) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button signInButton = (Button) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        client = new OkHttpClient();

        mAccountManager = AccountManager.get(getBaseContext());
    }

    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 5;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Response> {

        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Response doInBackground(Void... params) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("username", mUsername);
                obj.put("password", mPassword);
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), obj.toString());
                Request request = new Request.Builder()
                        .url(ConstantUtil.URL_LOGIN)
                        .post(body)
                        .build();
                return client.newCall(request).execute();

            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Response response) {
            mAuthTask = null;
            showProgress(false);

            if (response != null) {
                ResponseBody body = response.body();
                if (response.code() == 200) {
                    try {
                        Gson gson = new Gson();
                        TokenUtil tu = gson.fromJson(body.string(), TokenUtil.class);
                        finishLogin(tu, mUsername, mPassword);
                    } catch (IOException e) {
                        e.printStackTrace();
                        // TODO show error to the user
                    }
                } else {
                    // TODO show error to the user
                }
            } else {
                // TODO show error to the user
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    private void finishLogin(TokenUtil tu, String mUsername, String mPassword) {

        final Account account = new Account(mUsername, AccountGeneral.ACCOUNT_TYPE);

        mAccountManager.addAccountExplicitly(account, mPassword, getRolesBundle(tu.getRoles()));
        mAccountManager.setAuthToken(account, "Bearer", tu.getToken());

        Bundle mResultBundle = new Bundle();
        mResultBundle.putString(AccountManager.KEY_ACCOUNT_NAME, mUsername);
        mResultBundle.putString(AccountManager.KEY_AUTHTOKEN, tu.getToken());

        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onResult(mResultBundle);
        }
        Intent i = new Intent();
        i.putExtras(mResultBundle);
        setResult(RESULT_OK, i);
        finish();
    }

    private Bundle getRolesBundle(String[] roles) {
        Bundle b = new Bundle();
        for (String role : roles) {
            if (role.equals(AccountGeneral.ADMIN)) b.putString(AccountGeneral.ADMIN, "admin");
            if (role.equals(AccountGeneral.MANAGER)) b.putString(AccountGeneral.MANAGER, "manager");
            if (role.equals(AccountGeneral.USER)) b.putString(AccountGeneral.USER, "user");
        }
        return b;
    }
}

