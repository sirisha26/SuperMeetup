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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.supermeetup.supermeetup.MeetupApp;
import com.supermeetup.supermeetup.R;
import com.supermeetup.supermeetup.activities.HomeActivity;
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
    private GoogleMap mGoogleMap;
    private boolean mIsListView = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFindBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_find, container, false);
        View view = mFindBinding.getRoot();
        mLoadingDialog = new LoadingDialog(getActivity());
        mFindBinding.findList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFindBinding.findList.setAdapter(new EventAdapter(null));
        if(!TextUtils.isEmpty(mQuery)){
            mFindBinding.findSearchlayout.searchview.setQuery(mQuery, true);
        }

        mFindBinding.findMap.onCreate(savedInstanceState);

        setViewMode();
        mFindBinding.findSwitchview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsListView = !mIsListView;
                setViewMode();
            }
        });

        mFindBinding.findSearchlayout.searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mQuery = mFindBinding.findSearchlayout.searchview.getQuery().toString();
                mFindBinding.findSearchlayout.searchview.clearFocus();
                loadEvents();
                ((HomeActivity) getActivity()).setQuery(mQuery);
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

    private void setViewMode(){
        if(!mIsListView){
            mFindBinding.findSwitchview.setImageResource(R.mipmap.ic_list);
            mFindBinding.findList.setVisibility(View.GONE);
            mFindBinding.findMap.setVisibility(View.VISIBLE);
            setMap(((EventAdapter) mFindBinding.findList.getAdapter()).getEvents());
        }else{
            mFindBinding.findSwitchview.setImageResource(R.mipmap.ic_map);
            mFindBinding.findList.setVisibility(View.VISIBLE);
            mFindBinding.findMap.setVisibility(View.GONE);
            mFindBinding.findMap.onPause();
        }
    }

    private void setMap(final ArrayList<Event> events){
        mFindBinding.findMap.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mFindBinding.findMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                mGoogleMap = mMap;

                // For showing a move to my location button
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.moveCamera(CameraUpdateFactory
                        .newLatLngZoom(new LatLng(mLocation.getLatitude(),mLocation.getLongitude()), Util.DEFAULT_RADIUS));

                final ArrayList<Event> eventsWithVenue = new ArrayList<>();
                for(Event e : events){
                    if(e.getVenue() != null && e.getVenue().isVisible()){
                        eventsWithVenue.add(e);
                    }
                }

                placeMarker(eventsWithVenue, mGoogleMap.getCameraPosition().target);

            }
        });

    }

    private void placeMarker(ArrayList<Event> events, LatLng target){
        mGoogleMap.clear();
        if(events != null && events.size() > 0){
            ArrayList<Event> nearbyEvents = Util.sortLocations(events, target);
            for(Event event : nearbyEvents) {
                LatLng markerLocation = Util.getVenueLatLng(event);
                mGoogleMap.addMarker(new MarkerOptions().position(markerLocation).title(event.getName()).snippet(event.getVenue().getFullAddress()));
            }
            target = Util.getVenueLatLng(events.get(0));
        }
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(target)
                .zoom(Util.DEFAULT_ZOOM)
                .build();
        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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
                    if(!mIsListView){
                        setMap(events);
                    }
                }
                mLoadingDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ArrayList<Event>> call, Throwable t) {
                // Log error here since request failed
                mLoadingDialog.dismiss();
                Log.e("finderror", "Find event request error: " + t.toString());
            }
        }, Util.DEFAULT_FIELDS, mLocation.getLatitude(), mLocation.getLongitude(), Util.DEFAULT_RADIUS, mQuery);
    }

}
