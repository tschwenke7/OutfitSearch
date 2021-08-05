package com.example.outfitsearch;

import android.app.Application;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {
    private final int NUMBER_OF_THREADS = 4;

    public final ExecutorService backgroundExecutorService
            = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
