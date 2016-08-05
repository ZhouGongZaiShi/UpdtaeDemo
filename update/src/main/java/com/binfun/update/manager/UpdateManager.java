package com.binfun.update.manager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.binfun.update.IDownloadCallback;
import com.binfun.update.IDownloadService;
import com.binfun.update.UpdateStatus;
import com.binfun.update.bean.UpdateResponse;
import com.binfun.update.callback.OnDownloadListener;
import com.binfun.update.callback.OnUpdateListener;
import com.binfun.update.event.CancelDialogEvent;
import com.binfun.update.rxbus.RxBus;
import com.binfun.update.rxbus.RxBusSubscriber;
import com.binfun.update.service.DownloadService;
import com.binfun.update.ui.ResultDialogFragment;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
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
public class UpdateManager implements DialogInterface.OnClickListener {

    private static final String TAG = "UpdateManager";
    public static final String PROGRESS_DIALOG = "request_dialog";
    public static final String RESULT_DIALOG = "result_dialog";
    public static final String DOWNLOAD_DIALOG = "download_dialog";
    public final static String FILE_DIR = "file_dir";
    public final static String FILE_NAME = "file_name";

    public static final int NOUPDATE = 0;
    public static final int UPDATE = 1;
    public static final int FORCE = 2;
    public static final int ERROR = 3;

    private static final int CANCEL_DOWNLOAD = 233;


    private int mDownloadPid;
    private Intent mDownloadIntent;

    private int mVersionCode;


    @IntDef({NOUPDATE, UPDATE, FORCE, ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResultStatus {
    }


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
    private Map<String, String> mParms;

    private OnUpdateListener mUpdateListener;
    private OnDownloadListener mDownloadListener;


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
                } else {
                    setDownloadProgress(currProgress);
                }
                Log.d(TAG, "curr : " + currProgress);
            }
            preProgress = currProgress;
        }

        @Override
        public void onDownloadEnd(int result, String file) throws RemoteException {
            killDownloadService();
            Log.d(TAG, "onDownloadEnd : " + result);
            if (mDownloadListener != null) {
                mDownloadListener.onDownloadEnd(result, file);
            } else {
                if (mDownloadDialog != null) {
                    mDownloadDialog.dismiss();
                }
                switch (result) {
                    case UpdateStatus.DOWNLOAD_COMPLETE_SUCCESS:
                        Log.d(TAG, "onDownloadEnd : 下载成功");
                        installApk(mContext, new File(file));
                        break;
                    case UpdateStatus.DOWNLOAD_COMPLETE_FAIL:
                        Log.d(TAG, "onDownloadEnd : 下载失败");
                        mDownloadDialog.setMessage("下载失败!");
                        break;
                    default:
                        break;
                }
            }

        }
    };
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = IDownloadService.Stub.asInterface(iBinder);

            try {
                mDownloadPid = mService.getPid();
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
    private boolean isForce;

    private static volatile UpdateManager mInstance;
    private ProgressDialog mDownloadDialog;
    private ProgressDialog mProgressDialog;
    private AlertDialog mResultDialog;

    private UpdateManager(Context context) {
        mContext = context.getApplicationContext();
        subscribeEvent();
    }

    public static UpdateManager getInstance(Context context) {
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
        addSubscription(cancelDialogSubscription);
    }

    public void addSubscription(Subscription subscription) {
        if (this.mCompositeSubscription == null) {
            this.mCompositeSubscription = new CompositeSubscription();
        }
        this.mCompositeSubscription.add(subscription);
    }

    public void autoUpdate() {
        update(false);
    }

    public void forceUpdate() {
        update(true);
    }


    public void update(final boolean force) {
        isForce = force;
//        clearApk();
        // TODO: 2016/8/3 http错误       syntax error, unexpect token error

        if (mRetrofit == null) {
            mRetrofit = new Retrofit.Builder()
                    .baseUrl("http://api.binfun.tv:3020/api/v1/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(createOkHttpClient())
                    .build();
        }

        cancelCheckUpdate();

        mSubscriber = new Subscriber<UpdateResponse>() {
            @ResultStatus
            int resultCode = NOUPDATE;

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                if (mUpdateListener != null) {
                    mUpdateListener.onUpdateReturned(UpdateStatus.TIMEOUT, null);
                }
                resultCode = ERROR;
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                showResultDialog("请求失败:" + e.getMessage(), resultCode, null);
                Log.d(TAG, "onError : " + e.getMessage());
                System.out.println("onError : " + e.getMessage());
            }

            @Override
            public void onNext(UpdateResponse response) {
                if (mVersionCode == 0) {
                    return;
                }
                if (response == null) {
                    return;
                }
                UpdateResponse.ReleaseBean release = response.getRelease();
                if (release != null) {
                    mApkUrl = release.getUrl();
                }

                if (mUpdateListener != null) {
                    //用户设置了回调
                    if (mVersionCode < response.getIncompatibleVersion()) {
                        //强制更新
                        mUpdateListener.onUpdateReturned(UpdateStatus.FORCE, response);
                    } else if (mVersionCode < release.getVersionCode()) {
                        //有更新
                        mUpdateListener.onUpdateReturned(UpdateStatus.YES, response);
                    } else {
                        //无更新
                        mUpdateListener.onUpdateReturned(UpdateStatus.NO, response);
                    }

                } else {
                    //用户未设置回调
                    if (mVersionCode < response.getIncompatibleVersion()) {
                        //强制更新
                        resultCode = FORCE;
                        showResultDialog(null, resultCode, response);
                    } else if (mVersionCode < release.getVersionCode()) {
                        //有更新
                        resultCode = UPDATE;
                        showResultDialog(null, resultCode, response);
                    } else {
                        //无更新
                        resultCode = NOUPDATE;
                        if (isForce) {
                            showResultDialog("已经是最新版本啦...", resultCode, response);
                        }
                    }
                }
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
            }
        };


        mRetrofit.create(IUpdateResponse.class)
                .getUpdateResponse(mParms)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        Log.d(TAG, "call : 准备");
                        showProgressDialog();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread()) // 指定doOnSubscribe执行在主线程
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mSubscriber);
    }

    private OkHttpClient createOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(3000, TimeUnit.MILLISECONDS);
        return builder.build();
    }

//    private void clearApk() {
//
//        File downloadFile = new File(destFileDir+ File.separator+destFileName);
//        if (downloadFile.exists()){
//            downloadFile.delete();
//        }
//    }


    private void showResultDialog(String info, int code, @Nullable UpdateResponse response) {
        if (isAutoPopup) {
            if (isForce) {
                //强制状态下显示所有结果的对话框
//                ResultDialogFragment dialog = ResultDialogFragment.newInstance(info, code);
//                dialog.show(((FragmentActivity) mContext).getSupportFragmentManager(), RESULT_DIALOG);
                createResultDialog(info, code, response);
            } else {
                if (code == ResultDialogFragment.UPDATE || code == ResultDialogFragment.FORCE) {
                    //非强制状态下，仅显示强制更新与有更新的结果对话框
//                    ResultDialogFragment dialog = ResultDialogFragment.newInstance(info, code);
//                    dialog.show(((FragmentActivity) mContext).getSupportFragmentManager(), RESULT_DIALOG);
                    createResultDialog(info, code, response);
                }
            }
        }
    }

    private void createResultDialog(String info, int code, @Nullable UpdateResponse response) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        String msg;
        if (response == null) {
            msg = info;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("最新版本:")
                    .append(response.getRelease().getVersionName())
                    .append("\n")
                    .append("有新版本啦,是否升级？\n\n")
                    .append("更新内容\n")
                    .append(response.getRelease().getChangeLog());

            msg = sb.toString();
        }

        builder.setMessage(msg);

        switch (code) {
            case NOUPDATE:
                builder.setTitle("检查更新");
                builder.setNegativeButton("确认", this);
                break;
            case UPDATE:
                builder.setTitle("发现新版本");
                builder.setPositiveButton("立即更新", this);
                builder.setNegativeButton("以后再说", this);
                break;
            case FORCE:
                builder.setTitle("发现新版本");
                builder.setCancelable(false);
                builder.setPositiveButton("立即更新", this);
                break;
            case ERROR:
                builder.setTitle("检查更新");
                builder.setNegativeButton("确认", this);
                break;
            default:
                builder.setTitle("更新");
                builder.setNegativeButton("确认", this);
                break;
        }
        mResultDialog = builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mResultDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
        } else {
            mResultDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
        }
        mResultDialog.show();
    }

    private void showProgressDialog() {
        if (mUpdateListener == null) {
            if (isAutoPopup && isForce) {
//            mProgressDialogFragment = ProgressDialogFragment.newInstance();
//            mProgressDialogFragment.show(((FragmentActivity) mContext).getSupportFragmentManager(), PROGRESS_DIALOG);
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
                } else {
                    mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
                }
                mProgressDialog.setMessage("正在检查更新...");
                mProgressDialog.show();
            }
        }
    }

    private void showDownloadDialog() {
//        mDownloadDialogFragment = DownloadDialogFragment.newInstance();
//        mDownloadDialogFragment.show(((FragmentActivity) mContext).getSupportFragmentManager(), DOWNLOAD_DIALOG);
        mDownloadDialog = new ProgressDialog(mContext);
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mDownloadDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
        } else {
            mDownloadDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
        }
        mDownloadDialog.setTitle("正在下载中...");
        mDownloadDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消下载", this);
        mDownloadDialog.setCancelable(false);
        mDownloadDialog.setMax(100);
        mDownloadDialog.setCanceledOnTouchOutside(false);
        mDownloadDialog.show();
    }

    private void setDownloadProgress(int progress) {
        if (mDownloadDialog != null) {
            mDownloadDialog.setProgress(progress);
        }
    }

    public void download() {
        showDownloadDialog();
        mDownloadIntent = new Intent(mContext, DownloadService.class);
        mDownloadIntent.putExtra(FILE_DIR, destFileDir);
        mDownloadIntent.putExtra(FILE_NAME, destFileName);
        mDownloadIntent.putExtra(APK_URL, mApkUrl);
        mContext.startService(mDownloadIntent);
        isBind = mContext.bindService(mDownloadIntent, mConnection, Context.BIND_AUTO_CREATE);
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

    public void setParms(@NonNull Map<String, String> parms) {
        mParms = parms;
    }


    public void setUpdateAutoPopup(boolean isAutoPopup) {
        this.isAutoPopup = isAutoPopup;
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

    public void setVersionCode(int versionCode) {
        mVersionCode = versionCode;
    }

    public void cancelCheckUpdate() {
        if (mSubscriber != null && !mSubscriber.isUnsubscribed()) {
            mSubscriber.unsubscribe();
        }
    }

    public void unRegister() {
        cancelCheckUpdate();
        killDownloadService();
        hideDialogs();
    }

    private void killDownloadService() {
        if (isBind) {
            mContext.unbindService(mConnection);
            mContext.stopService(mDownloadIntent);
//            mConnection = null;
            isBind = false;
            if (mDownloadPid != 0) {
                android.os.Process.killProcess(mDownloadPid);
            }
        }
    }

    private void hideDialogs() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mDownloadDialog != null && mDownloadDialog.isShowing()) {
            mDownloadDialog.dismiss();
        }
        if (mResultDialog != null && mResultDialog.isShowing()) {
            mResultDialog.dismiss();
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {

            case DialogInterface.BUTTON_POSITIVE:
                //下载文件
                download();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                if (mDownloadDialog != null && mDownloadDialog.isShowing()) {
                    //取消下载
                    killDownloadService();
                }
                break;
            default:
                break;
        }
    }

    public interface IUpdateResponse {
        @GET("sysinfo")
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
