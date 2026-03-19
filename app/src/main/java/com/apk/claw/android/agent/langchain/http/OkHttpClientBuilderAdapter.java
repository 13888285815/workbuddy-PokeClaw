package com.apk.claw.android.agent.langchain.http;

import com.apk.claw.android.utils.XLog;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Adapts OkHttp's builder to LangChain4j's HttpClientBuilder SPI.
 */
public class OkHttpClientBuilderAdapter implements HttpClientBuilder {

    private static final String TAG = "OkHttp";

    private Duration connectTimeout = Duration.ofSeconds(60);
    private Duration readTimeout = Duration.ofSeconds(300);

    /**
     * 是否将请求/响应原始数据输出到文件（沙盒缓存目录）
     */
    private boolean fileLoggingEnabled = false;
    private File cacheDir;

    public OkHttpClientBuilderAdapter() {
    }

    public OkHttpClientBuilderAdapter setFileLoggingEnabled(boolean enabled, File cacheDir) {
        this.fileLoggingEnabled = enabled;
        this.cacheDir = cacheDir;
        return this;
    }

    @Override
    public Duration connectTimeout() {
        return connectTimeout;
    }

    @Override
    public HttpClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public Duration readTimeout() {
        return readTimeout;
    }

    @Override
    public HttpClientBuilder readTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public HttpClient build() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                message -> XLog.d(TAG, message)
        );
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .addInterceptor(loggingInterceptor);

        if (fileLoggingEnabled && cacheDir != null) {
            builder.addInterceptor(new FileLoggingInterceptor(cacheDir));
        }

        OkHttpClient okHttpClient = builder.build();
        return new OkHttpClientAdapter(okHttpClient);
    }
}
