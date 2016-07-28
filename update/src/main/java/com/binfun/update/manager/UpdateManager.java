package com.binfun.update.manager;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;

import com.binfun.update.IDownloadCallback;
import com.binfun.update.IDownloadService;
import com.binfun.update.UpdateStatus;
import com.binfun.update.bean.UpdateResponse;
import com.binfun.update.callback.OnDownloadListener;
import com.binfun.update.callback.OnUpdateListener;
import com.binfun.update.event.CancelDialogEvent;
import com.binfun.update.event.UpdateEvent;
import com.binfun.update.rxbus.RxBus;
import com.binfun.update.rxbus.RxBusSubscriber;
import com.binfun.update.service.DownloadService;
import com.binfun.update.ui.DownloadDialogFragment;
import com.binfun.update.ui.ProgressDialogFragment;
import com.binfun.update.ui.ResultDialogFragment;

import java.io.File;
import java.util.Map;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.fastjson.FastJsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * 描述 : 升级管理类
 * 作者 : 周鑫
 * 创建日期 : 2016/7/22 15:26
 */
public class UpdateManager {

    private static final String TAG = "UpdateManager";
    public static final String PROGRESS_DIALOG = "request_dialog";
    public static final String RESULT_DIALOG = "result_dialog";
    public static final String DOWNLOAD_DIALOG = "download_dialog";
    public final static String FILE_DIR = "file_dir";
    public final static String FILE_NAME = "file_name";
    public final static String APK_URL = "apk_url";

    private CompositeSubscription mCompositeSubscription;
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


//    private boolean isOnlyWifi = false;

    private boolean isAutoPopup = true;
    private boolean isShowNoUpdate = true;
    private Map<String, String> mParms;

    private OnUpdateListener mUpdateListener;
    private OnDownloadListener mDownloadListener;

    private int resultCode = ResultDialogFragment.NOUPDATE;

    private ProgressDialogFragment mProgressDialogFragment;
    private Retrofit mRetrofit;
    private Subscriber<UpdateResponse> mSubscriber;

    private String mApkUrl;

    private int preProgress;

    private IDownloadService mService;
    private IDownloadCallback mCallback = new IDownloadCallback.Stub() {

        @Override
        public void onDownloadUpdate(long progress, long total) throws RemoteException {
            int currProgress = (int) (progress * 100 / total);
            if (preProgress < currProgress) {
                if (mDownloadListener != null) {
                    mDownloadListener.onDownloadUpdate(currProgress);
                }
            }
            preProgress = currProgress;
            setDownloadProgress(currProgress);
        }

        @Override
        public void onDownloadEnd(int result, String file) throws RemoteException {
            if (mDownloadListener != null) {
                mDownloadListener.onDownloadEnd(result, file);
            }
        }
    };
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = IDownloadService.Stub.asInterface(iBinder);
            try {
                mService.registerDownloadCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (isBind && mService != null) {
                try {
                    mService.unregisterDownloadCallback(mCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            mService = null;
        }
    };
    private boolean isBind;

    private static volatile UpdateManager mInstance;
    private DownloadDialogFragment mDownloadDialogFragment;

    private UpdateManager(Context context) {
        mContext = context;
        subscribeEvent();
    }

    public static UpdateManager getInstance(Context context) {
        // TODO: 2016/7/28  处理API 自动更新与手动更新
        if (mInstance == null) {
            synchronized (RxBus.class) {
                if (mInstance == null) {
                    mInstance = new UpdateManager(context);
                }
            }
        }
        return mInstance;
    }


    private void subscribeEvent() {
        Subscription cancelDialogSubscription = RxBus.getDefault().toObservable(CancelDialogEvent.class).subscribe(new RxBusSubscriber<CancelDialogEvent>() {
            @Override
            protected void onEvent(CancelDialogEvent cancelEvent) {
                cancelCheckUpdate();
            }
        });
        Subscription updateSubscription = RxBus.getDefault().toObservable(UpdateEvent.class).subscribe(new RxBusSubscriber<UpdateEvent>() {
            @Override
            protected void onEvent(UpdateEvent updateEvent) {
                download();
            }
        });
        addSubscription(cancelDialogSubscription);
        addSubscription(updateSubscription);
    }

    public void addSubscription(Subscription subscription) {
        if (this.mCompositeSubscription == null) {
            this.mCompositeSubscription = new CompositeSubscription();
        }
        this.mCompositeSubscription.add(subscription);
    }

    public void autoUpate(){
        update(true);
    }

    public void forceUpdate(){
        update(false);
    }


    private  boolean isForce;
    public void update(boolean force) {
        isForce = force;
//        if (!NetUtil.isConnected(mContext)) {
//            resultCode = ResultDialogFragment.NONET;
//            showResultDialog("网络无连接", resultCode);
//            if (mUpdateListener != null) {
//                mUpdateListener.onError(new Throwable("No network connection!"));
//            }
//            return;
//        }

        clearApk();


        if (mRetrofit == null) {
            mRetrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.1.166:8080/updateDemo/")
                    .addConverterFactory(FastJsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build();
        }


        mSubscriber = new Subscriber<UpdateResponse>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                if (mUpdateListener != null) {
                    mUpdateListener.onUpdateReturned(UpdateStatus.Timeout, null);
                }
                resultCode = ResultDialogFragment.ERROR;
                if (mProgressDialogFragment != null) {
                    mProgressDialogFragment.dismiss();
                }
                showResultDialog("服务器繁忙,请稍后再试!", resultCode);
            }

            @Override
            public void onNext(UpdateResponse response) {
                if (mUpdateListener != null) {
                    //用户设置了回调
                    if (response.isUpdate()) {
                        mUpdateListener.onUpdateReturned(UpdateStatus.Yes, response);
                    } else {
                        mUpdateListener.onUpdateReturned(UpdateStatus.Yes, response);
                    }
                } else {
                    //用户未设置回调
                    String info;
                    if (response.isForce()) {
                        resultCode = ResultDialogFragment.FORCE;
                        info = response.getUpdate_log();
                        mApkUrl = response.getApk_url();
                        showResultDialog(info, resultCode);
                    } else if (response.isUpdate()) {
                        resultCode = ResultDialogFragment.UPDATE;
                        info = response.getUpdate_log();
                        mApkUrl = response.getApk_url();
                        showResultDialog(info, resultCode);
                    } else if (!response.isUpdate()) {
                        resultCode = ResultDialogFragment.NOUPDATE;
                        info = "已经是最新版本啦...";
                        if (isForce) {
                            showResultDialog(info, resultCode);
                        }
                    }
                    if (mProgressDialogFragment != null) {
                        mProgressDialogFragment.dismiss();
                    }
                }
            }
        };

        mRetrofit.create(IUpdateResponse.class)
                .getUpdateResponse(mParms)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showProgressDialog();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mSubscriber);
    }

    private void clearApk() {
        File downloadFile = new File(destFileDir+ File.separator+destFileName);
        if (downloadFile.exists()){
            downloadFile.delete();
        }
    }


    private void showResultDialog(String info, int code) {
        if (isAutoPopup&&isForce) {
            ResultDialogFragment dialog = ResultDialogFragment.newInstance(info, code);
            dialog.show(((FragmentActivity) mContext).getSupportFragmentManager(), RESULT_DIALOG);
        }
    }

    private void showProgressDialog() {
        if (isAutoPopup&&isForce) {
            mProgressDialogFragment = ProgressDialogFragment.newInstance();
            mProgressDialogFragment.show(((FragmentActivity) mContext).getSupportFragmentManager(), PROGRESS_DIALOG);
        }
    }

    private void showDownloadDialog(){
        mDownloadDialogFragment = DownloadDialogFragment.newInstance();
        mDownloadDialogFragment.show(((FragmentActivity) mContext).getSupportFragmentManager(), DOWNLOAD_DIALOG);
    }

    private void setDownloadProgress(int progress){
        if (mDownloadDialogFragment==null){
            return;
        }
        ProgressDialog dialog = (ProgressDialog) mDownloadDialogFragment.getDialog();
        dialog.setProgress(progress);
    }

    public void download() {
        showDownloadDialog();
        Intent intent = new Intent(mContext, DownloadService.class);
        intent.putExtra(FILE_DIR, destFileDir);
        intent.putExtra(FILE_NAME, destFileName);
        intent.putExtra(APK_URL, mApkUrl);
        mContext.startService(intent);
        isBind = mContext.bindService(new Intent(mContext, DownloadService.class), mConnection, Context.BIND_AUTO_CREATE);
    }


    /**
     * 设置检查更新监听回调
     */
    public void setOnUpdateListener(OnUpdateListener listener) {
        mUpdateListener = listener;
    }

    /**
     * 设置下载监听回调
     */
    public void setOnDownloadListener(OnDownloadListener listener) {
        mDownloadListener = listener;
    }

//    public void setUpdateOnlyWifi(boolean isOnlyWifi) {
//        this.isOnlyWifi = isOnlyWifi;
//    }

    public void setParms(Map<String, String> parms) {
        mParms = parms;
    }


    public void setUpdateAutoPopup(boolean isAutoPopup) {
        this.isAutoPopup = isAutoPopup;
    }


    public void isShowNoUpdate(boolean isShowNoUpdate) {
        this.isShowNoUpdate = isShowNoUpdate;
    }


    /**
     * 设置APK文件下载文件夹路径
     *
     * @param fileDir 文件夹路径
     */
    public void setFileDir(String fileDir) {
        destFileDir = fileDir;
    }

    /**
     * 设置APK文件名
     *
     * @param fileName 文件名
     */
    public void setFileName(String fileName) {
        destFileName = fileName;
    }

    public void cancelCheckUpdate() {
        if (mSubscriber != null && !mSubscriber.isUnsubscribed()) {
            mSubscriber.unsubscribe();
        }
    }

    public void unRegister() {
        if (isBind) {
            mContext.unbindService(mConnection);
            isBind = false;
        }
    }

    public interface IUpdateResponse {
        @GET("version.html")
        Observable<UpdateResponse> getUpdateResponse(@QueryMap Map<String, String> parameters);
    }

    public static void installApk(Context context, File file) {
        Uri uri = Uri.fromFile(file);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(install);
    }
}
