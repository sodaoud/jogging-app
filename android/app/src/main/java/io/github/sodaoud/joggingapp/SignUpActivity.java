package io.github.sodaoud.joggingapp;

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
import android.widget.Toast;

import io.github.sodaoud.joggingapp.datatype.ErrorUtil;
import io.github.sodaoud.joggingapp.datatype.HttpUtil;
import io.github.sodaoud.joggingapp.datatype.TokenUtil;
import io.github.sodaoud.joggingapp.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


/**
 * A login screen that offers login via username/password.
 */
public class SignUpActivity extends AppCompatActivity {

    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

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

        getSupportActionBar().setTitle("Register");
        client = new OkHttpClient();

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

    public class UserLoginTask extends AsyncTask<Void, Void, HttpUtil> {

        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected HttpUtil doInBackground(Void... params) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("username", mUsername);
                obj.put("password", mPassword);
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), obj.toString());
                Request request = new Request.Builder()
                        .url(Util.URL_SIGN_UP)
                        .post(body)
                        .build();
                return new HttpUtil(client.newCall(request).execute());

            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final HttpUtil response) {
            mAuthTask = null;
            showProgress(false);

            if (response != null) {
                String bodyString = null;
                bodyString = response.body().string();
                if (response.code() == 201) {
                    TokenUtil tu = TokenUtil.getFromString(bodyString);
                    Bundle mResultBundle = Util.finishLogin(SignUpActivity.this
                            , tu, mUsername, mPassword);

                    Intent i = new Intent();
                    i.putExtras(mResultBundle);

                    setResult(RESULT_OK, i);
                    finish();
                } else {
                    ErrorUtil err = ErrorUtil.getFromString(bodyString);
                    switch (err.getError()) {
                        case "USERNAME_ERROR":
                            mUsernameView.setError(err.getMessage());
                            break;
                        case "PASSWORD_ERROR":
                            mPasswordView.setError(err.getMessage());
                            break;
                        default:
                            Toast.makeText(SignUpActivity.this, "An Error happened, please try again later", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(SignUpActivity.this, "The server is not responding, please try again later", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

