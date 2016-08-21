package com.toptal.joggingtracking.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.toptal.joggingtracking.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by sofiane on 8/16/16.
 */
public class ReportFragment extends Fragment {


    ViewPager pager;

    public static Fragment newInstance() {
        ReportFragment fragment = new ReportFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_report, container, false);
        pager = (ViewPager) v.findViewById(R.id.pager);
        pager.setAdapter(new Adapter(getActivity().getSupportFragmentManager()));
        pager.setCurrentItem(199); // Begin from the last

        return v;
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

    class Adapter extends FragmentPagerAdapter {

        ArrayList<ReportViewFragment> fragList = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            int p = 199 - position;
            if (fragList.size() <= p) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.WEEK_OF_YEAR, -p);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                Date begin = cal.getTime();
                cal.add(Calendar.DAY_OF_MONTH, 6);
                Date end = cal.getTime();
                fragList.add(p, ReportViewFragment.newInstance(begin, end));
            }

            return fragList.get(p);
        }

        @Override
        public int getCount() {
            return 200;
        }


    }
}
