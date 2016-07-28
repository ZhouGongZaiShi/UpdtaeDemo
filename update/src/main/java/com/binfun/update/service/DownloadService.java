package com.binfun.update.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.binfun.update.IDownloadCallback;
import com.binfun.update.IDownloadService;
import com.binfun.update.manager.UpdateManager;
import com.binfun.update.manager.fileload.FileCallback;

import java.io.File;

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

    private Context mContext;
    private Retrofit mRetrofit;
    private String mApkUrl;


    private RemoteCallbackList<IDownloadCallback> mCallbackList = new RemoteCallbackList<>();

    private IDownloadService.Stub mBinder =  new IDownloadService.Stub(){

        @Override
        public void registerDownloadCallback(IDownloadCallback cb) throws RemoteException {
            mCallbackList.register(cb);
        }

        @Override
        public void unregisterDownloadCallback(IDownloadCallback cb) throws RemoteException {
            mCallbackList.unregister(cb);
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;
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

        if (mRetrofit == null) {
            mRetrofit = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .baseUrl("http://192.168.1.166:8080/updateDemo/")
                    .build();
        }
        mRetrofit.create(IFileDownload.class)
                .download(mApkUrl).enqueue(new FileCallback(destFileDir,destFileName) {
            @Override
            public void onSuccess(File file) {
                System.out.println("下载完毕   filename"+file.getName()+ "   "+ file.exists());
                installApk(file);
//                onSuccess(file);
            }

            @Override
            public void onDownloading(long progress, long total) {
                System.out.println("下载中 "+progress+"  /total :　"+total);
//                onDownloading(progress,total);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println("onFailure");
//                onFailure(call,t);
            }
        });

    }

    private void installApk(File file) {
        Uri uri = Uri.fromFile(file);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        mContext.startActivity(install);
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

    public interface DownloadListener{
        void onSuccess(File file);
        void onDownloading(long progress,long total);
        void onFailure(Call<ResponseBody> call, Throwable t);
    }

}
