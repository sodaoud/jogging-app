package com.toptal.joggingtracking;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.toptal.joggingtracking.util.ConstantUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int GET_ACCOUNTS_REQUEST_CODE = 8712;
    private static final int REQ_LOGIN = 237;
    private Account account;
    private AccountManager am;

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

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        am = AccountManager.get(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, GET_ACCOUNTS_REQUEST_CODE);
        } else {
            getAccountOrBackToWelcomeActivity();
        }

        View header = ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0);
        header.findViewById(R.id.username).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                b.setTitle("Change account");
                final Account[] accounts = am.getAccounts();
                if (accounts.length > 1) {
                    List<Account> list = new ArrayList<>(Arrays.asList(accounts));
                    list.remove(account);
                    ArrayAdapter<Account> adapter = new AccountAdapter(list);
                    b.setAdapter(adapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                                    .edit().putString(ConstantUtil.USER_PREF, accounts[which].name)
                                    .apply();

                            Intent intent = getIntent();
                            finish();
                            startActivity(intent);
                        }
                    });
                } else {
                    b.setMessage("You are logged as " + account.name);
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
                        am.removeAccount(account,
                                new AccountManagerCallback<Boolean>() {
                                    @Override
                                    public void run(AccountManagerFuture<Boolean> arg0) {
                                        startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
                                        finish();
                                    }
                                }, null);
                    }
                });
                b.show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_LOGIN && resultCode == RESULT_OK) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(ConstantUtil.USER_PREF, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
            editor.apply();

            Intent intent = getIntent();
            finish();
            startActivity(intent);
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

        if (id == R.id.nav_list) {
            // Handle the camera action
        } else if (id == R.id.nav_params) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void getAccountOrBackToWelcomeActivity() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            // Already granted
            return;
        }
        Account[] accounts = am.getAccounts();
        if (accounts.length > 0) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String username = prefs.getString(ConstantUtil.USER_PREF, null);
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
                prefs.edit().putString(ConstantUtil.USER_PREF, account.name).apply();
            }
        } else {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }

        View header = ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0);
        ((TextView) header.findViewById(R.id.username)).setText(account.name);
    }
}
