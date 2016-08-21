package com.toptal.joggingtracking;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.toptal.joggingtracking.datatype.User;
import com.toptal.joggingtracking.fragments.ActivityFragment;
import com.toptal.joggingtracking.fragments.ReportViewFragment;
import com.toptal.joggingtracking.fragments.UsersFragment;
import com.toptal.joggingtracking.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int GET_ACCOUNTS_REQUEST_CODE = 8712;
    private static final int REQ_LOGIN = 237;
    private static final int REQ_PROFILE = 9123;
    private String shownFragmentTag = null;
    private OkHttpClient client;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = ActivityFragment.newInstance(false);
        shownFragmentTag = String.valueOf(R.id.nav_list);
        fragmentManager.beginTransaction().add(R.id.container, fragment, shownFragmentTag).commit();
        navigationView.setCheckedItem(R.id.nav_list);

        getAccountOrBackToWelcomeActivity();

        View header = navigationView.getHeaderView(0);
        header.findViewById(R.id.username).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                b.setTitle("Change account");
                Account[] accounts = Util.getAccounts(MainActivity.this);
                if (accounts.length > 1) {
                    final List<Account> list = new ArrayList<>(Arrays.asList(accounts));
                    list.remove(Util.getAccount(MainActivity.this));
                    ArrayAdapter<Account> adapter = new AccountAdapter(list);
                    b.setAdapter(adapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Util.setDefaultAccount(MainActivity.this, list.get(which));

                            Intent intent = getIntent();
                            finish();
                            startActivity(intent);
                        }
                    });
                } else {
                    b.setMessage("You are logged as " + Util.getAccount(MainActivity.this).name);
                }
                b.setNegativeButton("Log with another user", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), REQ_LOGIN);
                    }
                });
                b.setPositiveButton("Log out", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Util.logout(MainActivity.this, new AccountManagerCallback<Boolean>() {
                            @Override
                            public void run(AccountManagerFuture<Boolean> arg0) {
                                startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
                                finish();
                            }
                        });
                    }
                });
                b.show();
            }
        });
        client = new OkHttpClient();
    }

    private void refreshNavView() {
        navigationView.getMenu().findItem(R.id.nav_user_management).setVisible(Util.hasRole(Util.ADMIN) || Util.hasRole(Util.MANAGER));
        navigationView.getMenu().findItem(R.id.nav_activity_management).setVisible(Util.hasRole(Util.ADMIN));
        navigationView.getMenu().findItem(R.id.nav_list).setVisible(Util.hasRole(Util.USER));
        navigationView.getMenu().findItem(R.id.nav_chart).setVisible(Util.hasRole(Util.USER));
        navigationView.getMenu().findItem(R.id.nav_params).setVisible(Util.hasRole(Util.USER));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNavView();
        new UserTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    class UserTask extends AsyncTask<Void, Void, Response> {

        UserTask() {
        }

        @Override
        protected Response doInBackground(Void... params) {
            String token = Util.getAuthToken(MainActivity.this);
            try {
                HttpUrl.Builder builder;
                builder = new HttpUrl.Builder()
                        .scheme("http")
                        .host(Util.HOST)
                        .port(Util.PORT)
                        .addPathSegment(Util.SEGMENT_USER)
                        .addPathSegment("c");

                Request.Builder requestBuilder = new Request.Builder()
                        .addHeader("Authorization", token)
                        .url(builder.build())
                        .get();
                return client.newCall(requestBuilder.build()).execute();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Response response) {
            if (response != null) {
                if (response.code() == 200) {
                    Gson gson = new Gson();
                    try {
                        User user = gson.fromJson(response.body().string(), User.class);
                        Util.setUser(user);
                        refreshNavView();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 204) {
                    Util.logout(MainActivity.this, new AccountManagerCallback<Boolean>() {
                        @Override
                        public void run(AccountManagerFuture<Boolean> future) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Logout")
                                    .setMessage("Your account has been deleted")
                                    .setCancelable(false)
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
                                            finish();
                                        }
                                    })
                                    .show();
                        }
                    });
                }
            } else {
                Toast.makeText(MainActivity.this, "Can't reach server", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_LOGIN && resultCode == RESULT_OK) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
        if (requestCode == REQ_PROFILE && resultCode == RESULT_OK) {
            Util.setUser((User) data.getSerializableExtra(ProfileActivity.USER));
        }
    }

    private class AccountAdapter extends ArrayAdapter<Account> {

        AccountAdapter(List<Account> accounts) {
            super(MainActivity.this, 0, accounts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Account account = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            }
            TextView name = (TextView) convertView.findViewById(android.R.id.text1);
            name.setText(account.name);
            return convertView;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GET_ACCOUNTS_REQUEST_CODE) {
            getAccountOrBackToWelcomeActivity();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        String tag = String.valueOf(id);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (shownFragmentTag != null) {
            Fragment toHide = fragmentManager.findFragmentByTag(shownFragmentTag);
            transaction.hide(toHide);
        }
        if (fragment == null) {
            if (id == R.id.nav_list) {
                fragment = ActivityFragment.newInstance(false);
            } else if (id == R.id.nav_chart) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                cal.add(Calendar.DAY_OF_MONTH, -7);
                Date begin = cal.getTime();
                cal.add(Calendar.DAY_OF_MONTH, 6);
                Date end = cal.getTime();
                fragment = ReportViewFragment.newInstance(begin, end);
            } else if (id == R.id.nav_params) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.USER, Util.getUser());
                startActivityForResult(intent, REQ_PROFILE);

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_user_management) {
                fragment = UsersFragment.newInstance();
            } else if (id == R.id.nav_activity_management) {
                fragment = ActivityFragment.newInstance(true);
            }
            transaction.add(R.id.container, fragment, tag);
        } else {
            transaction.show(fragment);
        }
        transaction.commit();

        shownFragmentTag = tag;
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void getAccountOrBackToWelcomeActivity() {
        if (Util.getAccount(this) == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }

        View header = ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0);
        ((TextView) header.findViewById(R.id.username)).setText(Util.getAccount(this).name);
    }
}
