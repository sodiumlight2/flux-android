package org.nikanikoo.flux.data.managers.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;
import org.nikanikoo.flux.Constants;
import org.nikanikoo.flux.security.TokenManager;
import org.nikanikoo.flux.utils.Logger;
import org.nikanikoo.flux.utils.SSLHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.nikanikoo.flux.utils.CacheManager;

public class OpenVKApi {
    private static final String TAG = "OpenVKApi";
    private static final int THREAD_POOL_SIZE = 4;

    private final AtomicLong lastRequestTime = new AtomicLong(0);

    private static OpenVKApi instance;
    private final OkHttpClient client;
    private final CacheManager cacheManager;
    private final TokenManager tokenManager;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private static final String DEFAULT_CLIENT_NAME = Constants.Api.DEFAULT_CLIENT_NAME;


    private OpenVKApi(Context context) {
        cacheManager = new CacheManager(context.getApplicationContext());

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(Constants.Api.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.Api.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.Api.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .cache(cacheManager.getCache())
                .addInterceptor(CacheManager.getCacheInterceptor());

        SSLHelper.configureToIgnoreSSL(clientBuilder);

        client = clientBuilder.build();

        try {
            tokenManager = new TokenManager(context.getApplicationContext());
        } catch (TokenManager.EncryptionException e) {
            Logger.e(TAG, "Failed to initialize encrypted token storage", e);
            throw new RuntimeException("Unable to initialize API: secure storage unavailable", e);
        }

        executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });

        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized OpenVKApi getInstance(Context context) {
        if (instance == null) {
            instance = new OpenVKApi(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.cacheManager.evictAll();
            instance = null;
            Logger.d(TAG, "OpenVKApi instance reset, cache evicted");
        }
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public String getBaseUrl() {
        return tokenManager.getInstance();
    }

    public String getToken() {
        return tokenManager.getToken();
    }

    public void saveToken(String token) {
        tokenManager.saveToken(token);
    }

    public void saveInstance(String instanceUrl) {
        tokenManager.saveInstance(instanceUrl);
    }

    public void logout() {
        tokenManager.clear();
    }

    // интерфейсы для колбэков
    public interface LoginCallback {
        void onSuccess(String token);
        void onError(String error);
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public interface CountersCallback {
        void onSuccess(int messages, int notifications, int friends);
        void onError(String error);
    }

    public void login(String username, String password, LoginCallback callback) {
        login(username, password, null, callback);
    }

    public void loginWith2FA(String username, String password, String code, LoginCallback callback) {
        login(username, password, code, callback);
    }

    private void login(String username, String password, String code, LoginCallback callback) {
        executor.execute(() -> {
            enforceRateLimit();
            try {
                FormBody.Builder bodyBuilder = new FormBody.Builder()
                        .add("grant_type", "password")
                        .add("username", username)
                        .add("password", password);
                bodyBuilder.add("code", code != null ? code : "");

                if (DEFAULT_CLIENT_NAME != null) {
                    bodyBuilder.add("client_name", OpenVKApi.DEFAULT_CLIENT_NAME.trim());
                }
                RequestBody body = bodyBuilder.build();

                Request request = new Request.Builder()
                        .url(getBaseUrl() + "/token")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = "";
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }

                    Logger.apiResponse(TAG, (code != null ? "2FA " : "") + responseBody);

                    LoginResult result = processLoginResponse(responseBody);

                    mainHandler.post(() -> {
                        if (result.isSuccess()) {
                            callback.onSuccess(result.getToken());
                        } else {
                            callback.onError(result.getError());
                        }
                    });
                }
            } catch (Exception e) {
                Logger.e(TAG, "Ошибка входа", e);
                final String errorMsg = "Ошибка сети: " + e.getMessage();
                mainHandler.post(() -> callback.onError(errorMsg));
            }
        });
    }

    private void enforceRateLimit() {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastRequestTime.get();
        long timeSinceLastRequest = currentTime - lastTime;

        if (timeSinceLastRequest < Constants.Api.MIN_REQUEST_INTERVAL_MS) {
            try {
                Thread.sleep(Constants.Api.MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime.set(System.currentTimeMillis());
    }

    private static class LoginResult {
        private final boolean success;
        private final String token;
        private final String error;

        private LoginResult(boolean success, String token, String error) {
            this.success = success;
            this.token = token;
            this.error = error;
        }

        public static LoginResult success(String token) {
            return new LoginResult(true, token, null);
        }

        public static LoginResult error(String error) {
            return new LoginResult(false, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getToken() { return token; }
        public String getError() { return error; }
    }

    private LoginResult processLoginResponse(String responseBody) {
        if (responseBody.isEmpty()) {
            return LoginResult.error("Ошибка: Пустой ответ");
        }

        try {
            JSONObject json = new JSONObject(responseBody);

            if (json.has("error_code") || json.has("error") || json.has("error_msg")) {
                return LoginResult.error(responseBody);
            }

            if (json.has("access_token")) {
                String token = json.optString("access_token", null);
                if (!token.isEmpty()) {
                    return LoginResult.success(token);
                }
            }

            return LoginResult.error(responseBody);
        } catch (Exception jsonE) {
            return LoginResult.error(responseBody);
        }
    }

    public void callMethod(String methodName, Map<String, String> params, ApiCallback callback) {
        String token = getToken();
        if (token == null) {
            callback.onError("Нет access_token");
            return;
        }

        executor.execute(() -> {
            enforceRateLimit();
            try {
                StringBuilder urlBuilder = new StringBuilder(getBaseUrl() + "/method/" + methodName);
                urlBuilder.append("?access_token=").append(token);

                if (params != null) {
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        try {
                            String encodedKey = java.net.URLEncoder.encode(entry.getKey(), "UTF-8");
                            String encodedValue = java.net.URLEncoder.encode(entry.getValue(), "UTF-8");
                            urlBuilder.append("&").append(encodedKey).append("=").append(encodedValue);
                        } catch (java.io.UnsupportedEncodingException e) {
                            Logger.w(TAG, "Failed to encode: " + entry.getKey(), e);
                        }
                    }
                }

                Request request = new Request.Builder()
                        .url(urlBuilder.toString())
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String jsonString;
                    if (response.isSuccessful() && response.body() != null) {
                        jsonString = response.body().string();
                    } else {
                        String errorMsg = "HTTP ошибка: " + response.code();
                        if (response.body() != null) {
                            try {
                                errorMsg += " - " + response.body().string();
                            } catch (Exception e) { }
                        }
                        final String finalErrorMsg = errorMsg;
                        mainHandler.post(() -> callback.onError(finalErrorMsg));
                        return;
                    }

                    mainHandler.post(() -> {
                        try {
                            JSONObject json = new JSONObject(jsonString);
                            if (json.has("response")) {
                                callback.onSuccess(json);
                            } else if (json.has("error")) {
                                callback.onError(json.getJSONObject("error").optString("error_msg", "Неизвестная ошибка"));
                            } else {
                                callback.onError(jsonString);
                            }
                        } catch (Exception e) {
                            callback.onError("Парсинг JSON: " + e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                Logger.e(TAG, "Неизвестная ошибка API: " + methodName, e);
                final String errorMsg = "Ошибка: " + e.getMessage();
                mainHandler.post(() -> callback.onError(errorMsg));
            }
        });
    }

    public void getCounters(CountersCallback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("filter", "messages,notifications,friends");

        callMethod("account.getCounters", params, new ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONObject responseObj = response.getJSONObject("response");

                    int messages = responseObj.optInt("messages", 0);
                    int notifications = responseObj.optInt("notifications", 0);
                    int friends = responseObj.optInt("friends", 0);

                    Logger.d(TAG, "Counters - messages: " + messages +
                                     ", notifications: " + notifications +
                                     ", friends: " + friends);

                    callback.onSuccess(messages, notifications, friends);
                } catch (Exception e) {
                    Logger.e(TAG, "Ошибка парсинга счетчиков: " + e.getMessage(), e);
                    callback.onError("Ошибка парсинга счетчиков: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Logger.w(TAG, "Ошибка получения значения счетчиков: " + error);
                callback.onError(error);
            }
        });
    }
}