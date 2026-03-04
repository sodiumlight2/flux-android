package org.nikanikoo.flux;

import android.app.Application;

import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.nikanikoo.flux.data.managers.PhotoUploadManager;
import org.nikanikoo.flux.data.models.Group;
import org.nikanikoo.flux.data.models.Notification;
import org.nikanikoo.flux.utils.AsyncTaskHelper;
import org.nikanikoo.flux.utils.Logger;
import org.nikanikoo.flux.utils.SSLHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class FluxApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.d("FluxApplication", "Инициализация приложения");
        Notification.setAppContext(this);
        Group.setAppContext(this);
        configurePicasso();
    }

    private void configurePicasso() {
        Logger.d("FluxApplication", "Настройка Picasso с поддержкой SSL");
        
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(Constants.Api.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.Api.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.Api.WRITE_TIMEOUT, TimeUnit.SECONDS);

        SSLHelper.configureToIgnoreSSL(clientBuilder);

        OkHttpClient client = clientBuilder.build();

        int memoryCacheSize = (int) (Runtime.getRuntime().maxMemory() / 8);
        
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        
        Picasso picasso = new Picasso.Builder(this)
                .downloader(new OkHttp3Downloader(client))
                .executor(executorService)
                .memoryCache(new com.squareup.picasso.LruCache(memoryCacheSize))
                .indicatorsEnabled(false)
                .loggingEnabled(Constants.Debug.ENABLE_NETWORK_LOGGING)
                .build();

        Picasso.setSingletonInstance(picasso);
        Logger.d("FluxApplication", "Picasso успешно настроен с кэшем: " + (memoryCacheSize / 1024 / 1024) + "MB и 8 потоками");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Logger.d("FluxApplication", "Приложение завершается");

        AsyncTaskHelper.shutdown();
        Logger.d("FluxApplication", "AsyncTaskHelper остановлен");

        PhotoUploadManager.getInstance(this).shutdown();
        Logger.d("FluxApplication", "PhotoUploadManager остановлен");
    }
}
