package com.binfun.update.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.binfun.update.IDownloadCallback;
import com.binfun.update.IDownloadService;
import com.binfun.update.UpdateStatus;
import com.binfun.update.manager.UpdateManager;
import com.binfun.update.manager.fileload.FileCallback;
import com.binfun.update.manager.fileload.ProgressResponseBody;

import java.io.File;
import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;


/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/27 10:42
 */
public class DownloadService extends Service {
    private static final String TAG = "DownloadService";

    /**
     * 目标文件存储的文件夹路径
     */
    private String destFileDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File
            .separator + "BF_DEFAULT_DIR";
    /**
     * 目标文件存储的文件名
     */
    private String destFileName = "binfun.apk";

    private Retrofit mRetrofit;
    private String mApkUrl;

    private int mPid;

    private RemoteCallbackList<IDownloadCallback> mCallbackList = new RemoteCallbackList<>();

    private IDownloadService.Stub mBinder =  new IDownloadService.Stub(){

        @Override
        public void registerDownloadCallback(IDownloadCallback cb) throws RemoteException {
            mCallbackList.register(cb);
//            mCallbackList.beginBroadcast();
//            mCallbackList.finishBroadcast();
        }

        @Override
        public void unregisterDownloadCallback(IDownloadCallback cb) throws RemoteException {
            mCallbackList.unregister(cb);
//            mCallbackList.beginBroadcast();
//            mCallbackList.finishBroadcast();
        }

        @Override
        public int getPid() throws RemoteException{
            return mPid;
        }
    };


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate : ");
        super.onCreate();
        mPid = Process.myPid();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand : " );
        if (intent != null) {
            String fileDir = intent.getStringExtra(UpdateManager.FILE_DIR);
            String fileName = intent.getStringExtra(UpdateManager.FILE_NAME);
            String apkUrl = intent.getStringExtra(UpdateManager.APK_URL);
            if (!TextUtils.isEmpty(fileDir)) {
                destFileDir = fileDir;
            }
            if (!TextUtils.isEmpty(fileName)) {
                destFileName = fileName;
            }
            if (!TextUtils.isEmpty(apkUrl)) {
                mApkUrl = apkUrl;
            }
        }
        loadFile();
        return super.onStartCommand(intent, flags, startId);
    }


    private void loadFile() {
        // TODO: 2016/8/4  下载后回调线程问题 
        if (mRetrofit == null) {
            mRetrofit = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .baseUrl("http://api.binfun.tv:3020/api/")
                    .client(initOkHttpClient())
                    .build();
        }
        Log.d(TAG, "loadFile : url  " +mApkUrl );
        mRetrofit.create(IFileDownload.class)
                .download(mApkUrl).enqueue(new FileCallback(destFileDir,destFileName) {
            @Override
            public void onSuccess(File file) {
                int len = mCallbackList.beginBroadcast();
                for (int i = 0; i < len; i++) {
                    try {
                        mCallbackList.getBroadcastItem(i).onDownloadEnd(UpdateStatus.DOWNLOAD_COMPLETE_SUCCESS,file.getAbsolutePath());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                mCallbackList.finishBroadcast();
//                onSuccess(file);
            }

            @Override
            public void onDownloading(long progress, long total) {
                int len = mCallbackList.beginBroadcast();
                for (int i = 0; i < len; i++) {
                    try {
                        mCallbackList.getBroadcastItem(i).onDownloadUpdate(progress,total);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                mCallbackList.finishBroadcast();
//                onDownloading(progress,total);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure : " +t.getMessage() );
                int len = mCallbackList.beginBroadcast();
                for (int i = 0; i < len; i++) {
                    try {
                        mCallbackList.getBroadcastItem(i).onDownloadEnd(UpdateStatus.DOWNLOAD_COMPLETE_FAIL,"");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                mCallbackList.finishBroadcast();
//                onFailure(call,t);
            }
        });

    }

    /**
     * 初始化OkHttpClient
     *
     * @return
     */
    private OkHttpClient initOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.networkInterceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                return originalResponse
                        .newBuilder()
                        .body(new ProgressResponseBody(originalResponse.body()))
                        .build();
            }
        });
        return builder.build();
    }




    public interface IFileDownload {
        @GET
        Call<ResponseBody> download(@Url String url);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
