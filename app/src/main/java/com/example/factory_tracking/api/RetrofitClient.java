package com.example.factory_tracking.api;

import android.content.Context;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    // Base URL for the Node.js backend.
    // For emulator: use "http://10.0.2.2:3000/"
    // For physical device: use "http://YOUR_PC_IP:3000/" (replace with your PC's local IP)
    private static final String BASE_URL = "http://10.0.2.2:3000/";

    private static ApiService apiService;

    public static void init(Context context) {
        // No-op for now; kept for future extension if needed.
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static ApiService getApi() {
        if (apiService == null) {
            String base = getBaseUrl();
            if (!base.endsWith("/")) base += "/";
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(base)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}