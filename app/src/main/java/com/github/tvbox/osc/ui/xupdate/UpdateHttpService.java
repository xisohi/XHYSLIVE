package com.github.tvbox.osc.ui.xupdate;

import android.util.Log;

import androidx.annotation.NonNull;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.xuexiang.xupdate.proxy.IUpdateHttpService;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.OkHttpClient;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.concurrent.TimeUnit;


/**
 * @version 1.0
 * @auther lsj
 * @date 2023/08/14
 */
public class UpdateHttpService implements IUpdateHttpService {
    public static String baseUrl = Constants.UPDATE_URL;
    private OkHttpClient customOkHttpClient;

    public UpdateHttpService() {
        this.customOkHttpClient = createCustomOkHttpClient();
    }

    public UpdateHttpService setOkHttpClient(OkHttpClient okHttpClient) {
        this.customOkHttpClient = okHttpClient;
        return this;
    }

    @Override
    public void asyncGet(@NonNull String url, @NonNull Map<String, Object> params, final @NonNull Callback callBack) {
//        Log.e("XUpdate", "asyncGet--- " + url);

        // 使用自定义的 OkHttpClient 创建 OkGo 实例
        OkGo.<String>get(url)
                .params(transform(params))
                .client(customOkHttpClient) // 使用自定义客户端
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        //Log.e("XUpdate", "--- " + response);
                        callBack.onSuccess(response.body());
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        //Log.e("XUpdate", "onError--- " + response);
                        callBack.onError(response.getException());
                    }
                });
    }

    @Override
    public void asyncPost(@NonNull String url, @NonNull Map<String, Object> params, final @NonNull Callback callBack) {
        //Log.e("XUpdate", "asyncPost--- " + url);
        OkGo.<String>post(url)
                .params(transform(params))
                .client(customOkHttpClient) // 使用自定义客户端
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        callBack.onSuccess(response.body());
                        Log.e("XUpdate", "onSuccess--- " + response.body());
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        Log.e("XUpdate", "onError--- " + response);
                        callBack.onError(response.getException());
                    }
                });

    }

    @Override
    public void download(@NonNull String url, @NonNull String path, @NonNull String fileName, final @NonNull DownloadCallback callback) {
//        Log.e("XUpdate", "download--- " + url);
//        Log.e("XUpdate", "download--- " + path);
//        Log.e("XUpdate", "download--- " + fileName);
        OkGo.<File>get(url)
                .tag(url)                    // 请求的 tag, 主要用于取消对应的请求
                .client(customOkHttpClient) // 使用自定义客户端
                .execute(new FileCallback(path, fileName) {
                    @Override
                    public void downloadProgress(Progress progress) {
                        super.downloadProgress(progress);
                        callback.onProgress(progress.fraction, progress.totalSize);

                    }

                    @Override
                    public void onStart(com.lzy.okgo.request.base.Request<File, ? extends com.lzy.okgo.request.base.Request> request) {
                        super.onStart(request);
                        //Log.e("XUpdate", "download--- 下载开始" + fileName);
                        callback.onStart();

                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        //Log.e("XUpdate", "download--- 下载完成" + fileName);
                    }


                    @Override
                    public void onSuccess(Response<File> response) {
                        //Log.e("XUpdate", "download--- 下载成功" + fileName);
                        callback.onSuccess(response.body());

                    }

                    @Override
                    public void onError(Response<File> response) {
                        super.onError(response);
                        //Log.e("XUpdate", "download--- 下载失败");
                        callback.onError(response.getException());
                    }

                });
    }

    @Override
    public void cancelDownload(@NonNull String url) {
        OkGo.getInstance().cancelTag(url);
    }

    private Map<String, String> transform(Map<String, Object> params) {
        Map<String, String> map = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toString());
        }
        return map;
    }

    public static String getVersionCheckUrl() {
        if (baseUrl.endsWith("/")) {
            return baseUrl + "update/checkVersion";
        } else {
            return baseUrl + "/update/checkVersion";
        }
    }

    public static String getDownLoadUrl(String url) {
        if (baseUrl.endsWith("/")) {
            return baseUrl + "update/apk/" + url;
        } else {
            return baseUrl + "/update/apk/" + url;
        }
    }

    /**
     * 创建自定义的 OkHttpClient 以解决 SSL/TLS 握手问题
     */
    private OkHttpClient createCustomOkHttpClient() {
        try {
            // 创建信任所有证书的 TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // 安装 TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 创建 OkHttpClient 并配置
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true) // 跳过主机名验证
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

        } catch (Exception e) {
            Log.e("UpdateHttpService", "创建自定义OkHttpClient失败: " + e.getMessage());
            // 如果创建失败，返回默认的客户端
            return OkGo.getInstance().getOkHttpClient();
        }
    }
}