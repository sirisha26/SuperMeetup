package com.supermeetup.supermeetup.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.codepath.oauth.OAuthBaseClient;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.loopj.android.http.RequestParams;
import com.supermeetup.supermeetup.model.Category;
import com.supermeetup.supermeetup.model.Event;
import com.supermeetup.supermeetup.model.OpenEvent;
import com.supermeetup.supermeetup.model.Profile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class MeetupClient extends OAuthBaseClient {

    private static final BaseApi REST_API_INSTANCE = MeetupApi20.instance();
    private static final String REST_URL = "https://api.meetup.com";
    private static final String STREAM_URL = "http://stream.meetup.com";
    private static final String REST_CONSUMER_KEY = "tsnbip354bn734c40plu1nnif3";
    private static final String REST_CONSUMER_SECRET = "573r6lbbiq32q8619f0o2o7tkv";
    private static final String CALLBACK_URL = "meetupoauth://supermeetup.com";

    Retrofit retrofit;
    Retrofit streamRetrofit;

    private MeetupEndpointInterface apiService;
    private MeetupStreamApiInterface streamService;
    private OkHttpClient okHttpClient;
    // Used for pagination
    private String nextUrl;

    public MeetupClient(Context context) {
        super(context,
              REST_API_INSTANCE,
              REST_URL,
              REST_CONSUMER_KEY,
              REST_CONSUMER_SECRET,
              CALLBACK_URL);

        // Define the interceptor, add authentication headers
        Interceptor interceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                OAuth2AccessToken oAuth2AccessToken = (OAuth2AccessToken) MeetupClient.this.client.getAccessToken();
                String accessToken = oAuth2AccessToken.getAccessToken().toString();
                Request.Builder builder = chain.request().newBuilder();
                setAuthHeader(builder, accessToken);
                Response response = chain.proceed(builder.build());
                if (response.code() == 401) { //unauthorized
                    Request refreshTokenRequest = buildRefreshTokenRequest(prefs.getString("refresh_token", ""));
                    try (Response refreshTokenResponse = okHttpClient.newCall(refreshTokenRequest).execute()) {
                        if (!refreshTokenResponse.isSuccessful()) throw new IOException("Unexpected code " + refreshTokenResponse);

                        // {"access_token":"c766c534e39276a49b22a6eb3fe2bf8d","refresh_token":"7c80c7cf6f7cdaaa226fea9f0b92b117","token_type":"bearer","expires_in":3600}
                        try{
                            String rawResponse = refreshTokenResponse.body().string();
                            MeetupClient.this.updateAccessToken(rawResponse);
                            // Resend request
                            Request.Builder newBuilder = chain.request().newBuilder();
                            setAuthHeader(newBuilder, ((OAuth2AccessToken) MeetupClient.this.client.getAccessToken()).getAccessToken().toString()); //set auth token to updated

                            return chain.proceed(newBuilder.build()); //repeat request with new token

                        } catch (JSONException e) {
                            Log.e("Refresh access token:", e.getMessage());
                        }
                    }
                }

                return response;
            }
        };

        // Add the interceptor to OkHttpClient
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.interceptors().add(interceptor);
        okHttpClient = builder.build();

        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient);
        retrofit = retrofitBuilder.baseUrl(REST_URL).build();

        apiService = retrofit.create(MeetupEndpointInterface.class);
        streamRetrofit = retrofitBuilder.baseUrl(STREAM_URL).build();
        streamService = streamRetrofit.create(MeetupStreamApiInterface.class);
    }

    private void setAuthHeader(Request.Builder builder, String access_token) {
        if (access_token != null) {
            builder.addHeader("Authorization", String.format("Bearer %s", access_token));
        }
    }

    private Request buildRefreshTokenRequest(String refresh_token) {

        RequestParams params = new RequestParams();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refresh_token);
        params.put("client_id", REST_CONSUMER_KEY);
        params.put("client_secret", REST_CONSUMER_SECRET);
        RequestBody requestBody = RequestBody.create(null, "");

        String url = String.format("%s?%s", MeetupApi20.ACCESS_TOKEN_ENDPOINT, params.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        return request;
    }

    private void updateAccessToken(String rawResponse) throws JSONException {
        JSONObject reponseJsonObject = new JSONObject(rawResponse);
        OAuth2AccessToken newToken = new OAuth2AccessToken(
                reponseJsonObject.getString("access_token"),
                reponseJsonObject.getString("token_type"),
                reponseJsonObject.getInt("expires_in"),
                reponseJsonObject.getString("refresh_token"),
                null, rawResponse);
        client.setAccessToken(newToken);
        editor.putString("oauth_token", newToken.getAccessToken());
        editor.putString("refresh_token", newToken.getRefreshToken());
        editor.commit();
    }

    public void findEvent(@NonNull Callback<ArrayList<Event>> callback,
                          @Nullable String fields,
                          @Nullable Double lat,
                          @Nullable Double lon,
                          @Nullable Float radius,
                          @Nullable String text) {

        Call<ArrayList<Event>> call = apiService.findEvent(fields, lat, lon, radius, text);
        call.enqueue(callback);
    }

    public void findTopicCategories(@NonNull Callback<ArrayList<Category>> callback,
                                    @Nullable String fields,
                                    @Nullable Double lat,
                                    @Nullable Double lon,
                                    @Nullable Float radius) {

        Call<ArrayList<Category>> call = apiService.findTopicCategories(fields, lat, lon, radius);
        call.enqueue(callback);
    }

    public void recommendedEvents(@NonNull Callback<ArrayList<Event>> callback,
                                  @Nullable String fields,
                                  @Nullable Double lat,
                                  @Nullable Double lon,
                                  @Nullable Integer page,
                                  @Nullable Boolean self_groups,
                                  @Nullable Integer topic_category) {
        Call<ArrayList<Event>> call = apiService.recommendedEvents(fields, lat, lon, page, self_groups, topic_category);
        call.enqueue(callback);
    }


    public void saveNextUrlForRecommendedEvents(@NonNull retrofit2.Response response) {
        Headers headers = response.headers();
        if (headers != null) {
            nextUrl = headers.get("Link");
            if(nextUrl != null && !nextUrl.isEmpty()) {
                nextUrl = nextUrl.substring(nextUrl.indexOf('<') + 1, nextUrl.lastIndexOf('>'));
            }
        } else {
            nextUrl = null;
        }
    }

    public void getNextUrlForListEvents(@NonNull Callback<ArrayList<Event>> callback) {
        Call<ArrayList<Event>> call = apiService.getEventListNextPage(nextUrl);
        call.enqueue(callback);
    }

    public void getProfile(@NonNull Callback<Profile> callback,
                           @Nullable Long member_id,
                           @Nullable String fields) {
        String idString = "self";
        if (member_id != null) {
            idString = member_id.toString();
        }
        Call<Profile> call = apiService.getProfile(idString, fields);
        call.enqueue(callback);
    }

    public void streamOpenEvents(@NonNull Callback<OpenEvent> callback,
                                 @Nullable Long since_mtime) {
        Call<OpenEvent> call = streamService.openEvents(since_mtime);
        call.enqueue(callback);
    }

    public void getGroupEvents(@NonNull Callback<ArrayList<Event>> callback,
                               @Nullable String urlname,
                               @Nullable Boolean desc,
                               @Nullable String fields,
                               @Nullable Integer page,
                               @Nullable String scroll,
                               @Nullable String status) {
        Call<ArrayList<Event>> call = apiService.getGroupEvents(urlname, desc, fields, page, scroll, status);
        call.enqueue(callback);
    }

    public void getEvent(@NonNull Callback<Event> callback,
                         @Path("urlname") String urlname,
                         @Path("id") String id,
                         @Nullable @Query("fields") String fields) {
        Call<Event> call = apiService.getEvent(urlname, id, fields);
        call.enqueue(callback);
    }
}
