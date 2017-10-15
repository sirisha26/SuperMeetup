package com.supermeetup.supermeetup.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.supermeetup.supermeetup.MeetupApp;
import com.supermeetup.supermeetup.R;
import com.supermeetup.supermeetup.databinding.ActivityHomeBinding;
import com.supermeetup.supermeetup.network.MeetupClient;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private MeetupClient meetupClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        meetupClient = MeetupApp.getRestClient(this);

        /*
        // test findEvent
        meetupClient.findEvent(new Callback<ArrayList<Event>>() {
            @Override
            public void onResponse(Call<ArrayList<Event>> call, Response<ArrayList<Event>> response) {
                int statusCode = response.code();
                ArrayList<Event> events = response.body();
            }

            @Override
            public void onFailure(Call<ArrayList<Event>> call, Throwable t) {
                // Log error here since request failed
                Log.e("finderror", "Find event request error: " + t.toString());
            }
        }, null, null, null, 0.5f, null);
        */

        /*
        // test findTopicCategories
        meetupClient.findTopicCategories(new Callback<ArrayList<Category>>() {
            @Override
            public void onResponse(Call<ArrayList<Category>> call, Response<ArrayList<Category>> response) {
                int statusCode = response.code();
                ArrayList<Category> categories = response.body();
            }

            @Override
            public void onFailure(Call<ArrayList<Category>> call, Throwable t) {
                // Log error here since request failed
                Log.e("finderror", "Find topic categories request error: " + t.toString());
            }
        }, null, null, null, null);
        */

        /*
        meetupClient.recommendedEvents(new Callback<ArrayList<Event>>() {
            @Override
            public void onResponse(Call<ArrayList<Event>> call, Response<ArrayList<Event>> response) {
                int statusCode = response.code();
                ArrayList<Event> categories = response.body();
                statusCode = 0;
            }

            @Override
            public void onFailure(Call<ArrayList<Event>> call, Throwable t) {
                // Log error here since request failed
                Log.e("finderror", "Recommended event request error: " + t.toString());
            }
        }, null, null, null, null, null, null);
        */
    }
}
