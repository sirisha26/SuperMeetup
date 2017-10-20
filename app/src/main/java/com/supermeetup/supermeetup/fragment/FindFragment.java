package com.supermeetup.supermeetup.fragment;

import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.supermeetup.supermeetup.MeetupApp;
import com.supermeetup.supermeetup.R;
import com.supermeetup.supermeetup.adapter.CategoryAndEventAdapter;
import com.supermeetup.supermeetup.adapter.EventAdapter;
import com.supermeetup.supermeetup.common.Util;
import com.supermeetup.supermeetup.databinding.FragmentFindBinding;
import com.supermeetup.supermeetup.dialog.LoadingDialog;
import com.supermeetup.supermeetup.model.Event;
import com.supermeetup.supermeetup.network.MeetupClient;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Irene on 10/19/17.
 */

public class FindFragment extends Fragment {
    private static FindFragment mFragment;
    private Location mLocation;
    private String mQuery;

    public static FindFragment getInstance(Location location, String query){
        if(mFragment == null){
            mFragment = new FindFragment();
        }
        mFragment.setLocation(location);
        mFragment.setQuery(query);
        return mFragment;
    }

    public void setLocation(Location location){
        if(location != null) {
            mLocation = location;
        }
    }

    public void setQuery(String query){
        mQuery = query;
    }

    private MeetupClient meetupClient;
    private FragmentFindBinding mFindBinding;
    private LoadingDialog mLoadingDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFindBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_find, container, false);
        View view = mFindBinding.getRoot();
        mLoadingDialog = new LoadingDialog(getActivity());
        mFindBinding.findList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFindBinding.findList.setAdapter(new EventAdapter());
        if(!TextUtils.isEmpty(mQuery)){
            mFindBinding.findSearchlayout.searchview.setQuery(mQuery, true);
        }

        mFindBinding.findSearchlayout.searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mQuery = mFindBinding.findSearchlayout.searchview.getQuery().toString();
                mFindBinding.findSearchlayout.searchview.clearFocus();
                loadEvents();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        meetupClient = MeetupApp.getRestClient(getActivity());
        loadEvents();
        return view;
    }

    private void loadEvents(){
        Util.hideSoftKeyboard(getActivity());
        mLoadingDialog.show();
        meetupClient.findEvent(new Callback<ArrayList<Event>>() {
            @Override
            public void onResponse(Call<ArrayList<Event>> call, Response<ArrayList<Event>> response) {
                int statusCode = response.code();
                ArrayList<Event> events = response.body();
                if(events != null){
                    ((EventAdapter) mFindBinding.findList.getAdapter()).setEvents(events, false);
                }
                mLoadingDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ArrayList<Event>> call, Throwable t) {
                // Log error here since request failed
                mLoadingDialog.dismiss();
                Log.e("finderror", "Find event request error: " + t.toString());
            }
        }, Util.FIELDS_DEFAULT, mLocation.getLatitude(), mLocation.getLongitude(), Util.RADIUS_DEFAULT, mQuery);
    }

}
