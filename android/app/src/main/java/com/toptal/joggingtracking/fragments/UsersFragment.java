package com.toptal.joggingtracking.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.toptal.joggingtracking.ProfileActivity;
import com.toptal.joggingtracking.R;
import com.toptal.joggingtracking.UserActivity;
import com.toptal.joggingtracking.datatype.User;
import com.toptal.joggingtracking.util.Util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created by sofiane on 8/16/16.
 */
public class UsersFragment extends Fragment {

    private RecyclerView list;
    private Adapter adapter;
    private OkHttpClient client;
    private View noServerView;
    private UsersTask mUsersTask;
    private List<User> users;
    private LinearLayoutManager mLayoutManager;
    private FloatingActionButton fab;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public static Fragment newInstance() {

        UsersFragment fragment = new UsersFragment();

        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        client = new OkHttpClient();
        users = new ArrayList<>();

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("User List");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        list = (RecyclerView) v.findViewById(R.id.list);
        list.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getContext());
        list.setLayoutManager(mLayoutManager);
        noServerView = v.findViewById(R.id.no_data);
        noServerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoServer(false);
                getUsers();
            }
        });
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getUsers();
            }
        });
        adapter = new Adapter();
        list.setAdapter(adapter);

        fab = (FloatingActionButton) v.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewTrack();
            }
        });
        return v;
    }

    private void createNewTrack() {
        Intent i = new Intent(getActivity(), UserActivity.class);
        Bundle b = new Bundle();
        i.putExtras(b);
        startActivityForResult(i, 432);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwipeRefreshLayout.setRefreshing(true);
        getUsers();
    }

    private void getUsers() {
        mUsersTask = new UsersTask();
        mUsersTask.execute((Void) null);
    }


    private void showNoServer(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mSwipeRefreshLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        mSwipeRefreshLayout.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mSwipeRefreshLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        noServerView.setVisibility(show ? View.VISIBLE : View.GONE);
        noServerView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                noServerView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

    }

    class UsersTask extends AsyncTask<Void, Void, Response> {

        UsersTask() {
        }

        @Override
        protected Response doInBackground(Void... params) {
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("http")
                    .host(Util.HOST)
                    .port(Util.PORT)
                    .addPathSegment(Util.SEGMENT_USER)
                    .build();
            String token = Util.getAuthToken(getActivity());
            try {
                Request request = new Request.Builder()
                        .addHeader("Authorization", token)
                        .url(url)
                        .get()
                        .build();
                return client.newCall(request).execute();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Response response) {
            mUsersTask = null;
            mSwipeRefreshLayout.setRefreshing(false);
            Gson gson = new Gson();
            if (response != null) {
                String stringBody = null;
                try {
                    stringBody = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (response.code() == 200) {
                    users.clear();
                    Type type = new TypeToken<List<User>>() {
                    }.getType();
                    List<User> tmp = gson.fromJson(stringBody, type);
                    users.addAll(tmp);
                    list.getAdapter().notifyDataSetChanged();

                } else if (response.code() == 401) {
                    Util.getNewAuthToken(new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            if (msg.what == Util.SUCCES) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getUsers();
                                    }
                                });
                            } else {
                                showNoServer(true);
                            }
                        }
                    });
                } else {
                    if (users.isEmpty())
                        showNoServer(true);
                }
            } else {
                if (users.isEmpty())
                    showNoServer(true);
            }
        }

        @Override
        protected void onCancelled() {
            mUsersTask = null;
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView mUsernameView;
            CircleImageView mAvatarView;
            Toolbar toolbar;

            public ViewHolder(View v) {
                super(v);
                mUsernameView = (TextView) v.findViewById(R.id.username);
                mAvatarView = (CircleImageView) v.findViewById(R.id.avatar);
                toolbar = (Toolbar) v.findViewById(R.id.card_toolbar);
                toolbar.inflateMenu(R.menu.user_card_menu);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_card,
                    parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final User user = users.get(position);
            holder.mUsernameView.setText(user.getUsername());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getActivity(), UserActivity.class);
                    i.putExtra(UserActivity.USER, user);
                    startActivity(i);
                }
            });
            holder.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent i;
                    switch (item.getItemId()) {
                        case R.id.edit:
                            i = new Intent(getActivity(), UserActivity.class);
                            i.putExtra(UserActivity.USER, user);
                            getActivity().startActivity(i);
                            return true;
                        case R.id.delete:
                            deleteUser(user);
                            return true;
                        case R.id.profile:
                            i = new Intent(getActivity(), ProfileActivity.class);
                            i.putExtra(ProfileActivity.USER, user);
                            getActivity().startActivity(i);
                            return true;
                    }
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }

    private void deleteUser(final User user) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete this entry?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       new DeleteTask(user).execute();
                    }
                }).show();
    }

    class DeleteTask extends AsyncTask<Void, Void, Response> {

        private final User user;

        DeleteTask(User user) {
            this.user = user;
        }

        @Override
        protected Response doInBackground(Void... params) {

            HttpUrl.Builder builder = new HttpUrl.Builder()
                    .scheme("http")
                    .host(Util.HOST)
                    .port(Util.PORT)
                    .addPathSegment(Util.SEGMENT_USER)
                    .addPathSegment(user.getId());
            Request request = new Request.Builder()
                    .addHeader("Authorization", Util.getAuthToken(getActivity()))
                    .url(builder.build())
                    .delete()
                    .build();
            try {
                return client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Response response) {
            if (response.code() == 200) {
                getUsers();
            } else {
                Toast.makeText(getActivity(), "Can not delete the entry", Toast.LENGTH_LONG).show();
            }

        }

        @Override
        protected void onCancelled() {
        }
    }

}
