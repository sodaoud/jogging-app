package com.toptal.joggingtracking.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.toptal.joggingtracking.R;
import com.toptal.joggingtracking.datatype.User;
import com.toptal.joggingtracking.util.UserActivity;
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
                getTracks();
            }
        });
        mSwipeRefreshLayout = (SwipeRefreshLayout)v.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getTracks();
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
        getTracks();
    }

    private void getTracks() {
        mSwipeRefreshLayout.setRefreshing(true);
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
                                        getTracks();
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

            public ViewHolder(View v) {
                super(v);
                mUsernameView = (TextView) v.findViewById(R.id.username);
                mAvatarView = (CircleImageView) v.findViewById(R.id.avatar);
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
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }

}
