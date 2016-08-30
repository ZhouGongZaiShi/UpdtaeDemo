package com.binfun.update.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Window;

import com.binfun.update.bean.UpdateResponse;
import com.binfun.update.callback.OnDownloadListener;
import com.binfun.update.callback.OnUpdateListener;
import com.binfun.update.common.Const;
import com.binfun.update.common.UpdateStatus;
import com.binfun.update.utils.DownloadManagerUtil;
import com.binfun.update.utils.HttpUtil;
import com.binfun.update.utils.MD5Util;
import com.binfun.update.utils.SPUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;


/**
 * 描述 : 升级管理类
 * 作者 : 周鑫
 * 创建日期 : 2016/7/22 15:26
 */
public class UpdateManager implements DialogInterface.OnClickListener {

    private static final String TAG = "UpdateManager";


    private final static String SPKEY_DOWNLOAD_ID = "download_id";
    private final static String SPKEY_DOWNLOAD_MD5 = "download_md5";
    private final static String SPKEY_DOWNLOAD_FILE = "download_file";

    private static final int NOUPDATE = 0;
    private static final int UPDATE = 1;
    private static final int FORCE = 2;
    private static final int ERROR = 3;


    private static final int MSG_DOWNLOAD_UPDATE = 100;


    private int mVersionCode;


    private final DownloadObserver mDownloadObserver;
    private final DownloadManager mManager;
    private final DownloadManagerUtil mDownloadManagerUtil;
    private long mDownloadId;
    private boolean isOnlyWifi;
    private CheckAsyncTask mCheckAsyncTask;
    private String mChannel;
    private String mAppName;
    private String mGid;

    @IntDef({NOUPDATE, UPDATE, FORCE, ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResultStatus {
    }


    private Context mContext;


//    private boolean isOnlyWifi = false;

    private boolean isAutoPopup = true;
    private Map<String, String> mParms;

    private OnUpdateListener mUpdateListener;
    private OnDownloadListener mDownloadListener;


    private String mApkUrl;

    private int preProgress;


    private Handler mHandler;

    private boolean isForce;

    private static volatile UpdateManager mInstance;
    private ProgressDialog mDownloadDialog;
    private ProgressDialog mProgressDialog;
    private AlertDialog mResultDialog;

    private UpdateManager(Context context) {
        mContext = context;
        mVersionCode = getVersionCode();

        mHandler = new DownloadHandler();
        mManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        mDownloadManagerUtil = new DownloadManagerUtil(mManager);

        //订阅下载变化
        mDownloadObserver = new DownloadObserver();
        mContext.getContentResolver().registerContentObserver(DownloadManagerUtil.CONTENT_URI, true, mDownloadObserver);

        //注册下载完成广播
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.DOWNLOAD_COMPLETE");
        BroadcastReceiver receiver = new CompleteReceiver();
        mContext.registerReceiver(receiver, filter);
    }

    private UpdateManager(Context context, String channel, String gid) {
        this(context);
        mChannel = channel;
        mGid = gid;
    }

    private UpdateManager(Context context, Map<String, String> parms) {
        this(context);
        mParms = parms;
    }

    /**
     * 初始化
     *
     * @param context
     * @param channel 渠道号
     * @param gid     GID
     * @return
     */
    public static UpdateManager init(@NonNull Context context, @NonNull String channel, @NonNull String gid) {
        if (mInstance == null) {
            synchronized (UpdateManager.class) {
                if (mInstance == null) {
                    mInstance = new UpdateManager(context, channel, gid);
                }
            }
        }
        return mInstance;
    }

    /**
     * @param context
     * @param parms   请求参数Map<String,String>,如 package=com.xx.xx, map.put("package","com.xx.xx")即可。
     * @return
     */
    public static UpdateManager init(@NonNull Context context, @NonNull Map<String, String> parms) {
        if (mInstance == null) {
            synchronized (UpdateManager.class) {
                if (mInstance == null) {
                    mInstance = new UpdateManager(context, parms);
                }
            }
        }
        return mInstance;
    }

    public void autoUpdate(@NonNull Context context) {
        mContext = context;
        update(false);
    }

    public void forceUpdate(@NonNull Context context) {
        mContext = context;
        update(true);
    }


    private void update(boolean force) {
        if (mParms == null && TextUtils.isEmpty(mChannel)) {
            throw new IllegalArgumentException("you must call setChannel(String channel),and channel cannot be empty");
        }


        isForce = force;
        cancelCheckUpdate();
        mCheckAsyncTask = new CheckAsyncTask();
        StringBuilder url = new StringBuilder(Const.BASE_QUEST_URL);
        url.append("?");
        if (mParms != null) {
            for (String key : mParms.keySet()) {
                url.append(key).append("=").append(mParms.get(key)).append("&");
            }
            url.delete(url.length() - 1, url.length());
        } else {
            url.append("package=").append(mContext.getPackageName())
//            url.append("package=").append("com.bfmarket.bbmarket")
                    .append("&channel=").append(mChannel)
                    .append("&gid=").append(mGid)
                    .append("&sysver=").append(Build.VERSION.SDK_INT)
                    .append("&ver=").append(getVersionName());
        }
        mCheckAsyncTask.execute(url.toString());
    }


    private void showResultDialog(String info, int code, @Nullable UpdateResponse response) {
        if (isAutoPopup) {
            if (isForce) {
                //强制状态下显示所有结果的对话框
                createResultDialog(info, code, response);
            } else {
                if (code == UPDATE || code == FORCE) {
                    createResultDialog(info, code, response);
                }
            }
        }
    }

    private void createResultDialog(String info, int code, @Nullable UpdateResponse response) {
        if (((Activity) mContext).isFinishing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        String msg = "";
        if (response == null || code == NOUPDATE || code == ERROR) {
            msg = info;
        } else {
            if (code == FORCE) {
                StringBuilder sb = new StringBuilder();
                sb.append("最新版本 : ")
                        .append(response.getRelease().getVersionName())
                        .append("<br>")
                        .append("新版本大小 : ")
                        .append(Formatter.formatFileSize(mContext, response.getRelease().getSize()))
                        .append("<br><br>")
                        .append("更新内容<br>")
                        .append(response.getRelease().getChangeLog());

                msg = sb.toString();
            } else if (code == UPDATE) {
                StringBuilder sb = new StringBuilder();
                sb.append("最新版本 : ")
                        .append(response.getRelease().getVersionName())
                        .append("\n")
                        .append("新版本大小 : ")
                        .append(Formatter.formatFileSize(mContext, response.getRelease().getSize()))
                        .append("\n\n")
                        .append("更新内容\n")
                        .append(response.getRelease().getChangeLog());

                msg = sb.toString();
            }
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
                builder.setMessage(Html.fromHtml(msg + "<br><br>" + "<font color = 'red'>低版本已不兼容,为了更好您更好的体验,请立即更新至最新版本</font>"));
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
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            mResultDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
//        } else {
//            mResultDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
//        }
        mResultDialog.show();
    }

    private void showProgressDialog() {
        if (((Activity) mContext).isFinishing()) {
            return;
        }
        if (mUpdateListener == null) {
            if (isAutoPopup && isForce) {
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
//                } else {
//                    mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
//                }
                mProgressDialog.setMessage("正在检查更新...");
                mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        cancelCheckUpdate();
                    }
                });
                mProgressDialog.show();
            }
        }
    }

    private void showDownloadDialog() {
        if (((Activity) mContext).isFinishing()) {
            return;
        }
        if (!isAutoPopup) {
            return;
        }
        mDownloadDialog = new ProgressDialog(mContext);
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            mDownloadDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
//        } else {
//            mDownloadDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
//        }
        mDownloadDialog.setTitle("正在下载中...");
        if (!isForceUpdate) {
            mDownloadDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消下载", this);
        }
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

    private void download() {
        //下载之前删除上次下载的文件
        String fileName = SPUtil.getString(mContext, SPKEY_DOWNLOAD_FILE);
        if (!TextUtils.isEmpty(fileName)) {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        }


        Uri uri = Uri.parse(mApkUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        if (isOnlyWifi) {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        }
        String title = TextUtils.isEmpty(mAppName) ? "应用升级" : mAppName;
        request.setTitle(title);
        request.setDescription(title + "开始下载...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.allowScanningByMediaScanner();
        request.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS, title);


        mDownloadId = mManager.enqueue(request);
        if (mDownloadListener!=null){
            mDownloadListener.onDownloadStart();
        }
        SPUtil.putLong(mContext, SPKEY_DOWNLOAD_ID, mDownloadId);
        showDownloadDialog();

    }

    public  void download(Context context,UpdateResponse updateResponse) {
        //下载之前删除上次下载的文件
        String fileName = SPUtil.getString(context, SPKEY_DOWNLOAD_FILE);
        if (!TextUtils.isEmpty(fileName)) {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        }

        Uri uri = Uri.parse(updateResponse.getRelease().getUrl());
        DownloadManager.Request request = new DownloadManager.Request(uri);

        if (isOnlyWifi) {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        }
        String title = TextUtils.isEmpty(updateResponse.getName()) ? "应用升级" : updateResponse.getName();
        request.setTitle(title);
        request.setDescription(title + "开始下载...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.allowScanningByMediaScanner();
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, title);


        mDownloadId = mManager.enqueue(request);
        if (mDownloadListener!=null){
            mDownloadListener.onDownloadStart();
        }
        SPUtil.putLong(mContext, SPKEY_DOWNLOAD_ID, mDownloadId);
        showDownloadDialog();
    }

    class DownloadObserver extends ContentObserver {


        public DownloadObserver() {
            super(mHandler);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateView();
        }


    }

    private void updateView() {
        int[] bytesAndStatus = mDownloadManagerUtil.getBytesAndStatus(mDownloadId);
        Message message = Message.obtain();
        message.what = MSG_DOWNLOAD_UPDATE;
        message.obj = bytesAndStatus;
        mHandler.sendMessage(message);
    }

    /**
     * 设置检查更新监听回调
     */
    public void setOnUpdateListener(OnUpdateListener updateListener) {
        mUpdateListener = updateListener;
    }

    /**
     * 设置下载监听回调
     */
    public void setOnDownloadListener(OnDownloadListener downloadListener) {
        mDownloadListener = downloadListener;
    }

    public void setUpdateOnlyWifi(boolean isOnlyWifi) {
        this.isOnlyWifi = isOnlyWifi;
    }


    public void setUpdateAutoPopup(boolean isAutoPopup) {
        this.isAutoPopup = isAutoPopup;
    }

    /**
     * 恢复默认设置
     */
    public void setDefault() {
        setUpdateOnlyWifi(false);
        setUpdateAutoPopup(true);
        setOnDownloadListener(null);
        setOnUpdateListener(null);
    }


    private void cancelCheckUpdate() {
        if (mCheckAsyncTask != null && mCheckAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mCheckAsyncTask.cancel(true);
        }
    }

    public void stop() {
        mContext.getContentResolver().unregisterContentObserver(mDownloadObserver);
        cancelCheckUpdate();
        hideDialogs();
    }


    private void hideDialogs() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        if (mDownloadDialog != null && mDownloadDialog.isShowing()) {
            mDownloadDialog.dismiss();
            mDownloadDialog = null;
        }
        if (mResultDialog != null && mResultDialog.isShowing()) {
            mResultDialog.dismiss();
            mResultDialog = null;
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
                    mManager.remove(mDownloadId);
                }
                if (mDownloadListener != null) {
                    mDownloadListener.onDownloadEnd(UpdateStatus.CANCEL_DOWNLOAD, null);
                }
                break;
            default:
                break;
        }
    }


    public static void installApk(Context context, File file) {
        Uri uri = Uri.fromFile(file);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(install);
    }


    class CompleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {

            long did = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            //获取调用DownloadManager时保存的id
            long myId = SPUtil.getLong(mContext, SPKEY_DOWNLOAD_ID);
            if (myId != did) {// 用于验证是否是下载的同一个apk
                return;
            }
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(did);
            Cursor c = mManager.query(query);
            if (c != null && c.moveToFirst()) {


                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL:
                        if (mDownloadDialog != null) {
                            mDownloadDialog.dismiss();
                        }
                        //如果文件名不为空，说明已经存在了，然后获取uri，进行安装
                        File path = new File(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)));
                        if (!path.exists()) {
                            return;
                        }
                        SPUtil.putString(mContext, SPKEY_DOWNLOAD_FILE, path.getAbsolutePath());
                        if ((SPUtil.getString(mContext, SPKEY_DOWNLOAD_MD5)).equalsIgnoreCase(MD5Util.getMD5File(path))) {
                            //MD5校验完成
                            if (mDownloadListener == null) {
                                installApk(mContext, path);
                            } else {
                                mDownloadListener.onDownloadEnd(UpdateStatus.DOWNLOAD_COMPLETE_SUCCESS, path.getAbsolutePath());
                            }
                        } else {
                            //MD5校验失败
                            if (mDownloadListener == null) {
                                showResultDialog("下载出错,请稍后重试", ERROR, null);
                            } else {
                                mDownloadListener.onDownloadEnd(UpdateStatus.DOWNLOAD_CHECK_MD5_FAIL, path.getAbsolutePath());
                            }
                        }
                        break;
                    default:
                        mManager.remove(did);
                        break;
                }
            }
        }
    }

    public int getVersionCode() {
        PackageInfo packageInfo = null;
        try {
            packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo != null) {
            return packageInfo.versionCode;
        } else {
            return -1;
        }
    }


    public String getVersionName() {
        PackageInfo packageInfo = null;
        try {
            packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo != null) {
            return packageInfo.versionName;
        } else {
            return "";
        }
    }

    private boolean isForceUpdate;

    private class CheckAsyncTask extends AsyncTask<String, Integer, String> {
        private int resultCode = NOUPDATE;

        @Override
        protected void onPreExecute() {
            showProgressDialog();
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected String doInBackground(String... strings) {
            if (isCancelled()) {
                return null;
            }
            return HttpUtil.getNetText4Https(strings[0]);
        }

        @Override
        protected void onPostExecute(String response) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (mVersionCode == 0) {
                return;
            }
            UpdateResponse updateResponse;
            if (TextUtils.isEmpty(response)) {
                if (mUpdateListener == null) {
                    showResultDialog("请求失败,请稍后再试", ERROR, null);
                } else {
                    mUpdateListener.onUpdateReturned(UpdateStatus.TIMEOUT, null);
                }
                return;
            } else {
                updateResponse = parseJsonString(response);
            }
            if (updateResponse == null) {
                //无数据
                showResultDialog("服务器繁忙,请稍后再试", ERROR, null);
                return;
            }
            if (updateResponse.getCode() == -1) {
                //未找到对应升级apk
                showResultDialog("服务器繁忙,请稍后再试", ERROR, null);
                return;
            } else if (updateResponse.getCode() == 0) {
                //数据正常
                mAppName = updateResponse.getName();

                UpdateResponse.ReleaseBean release = updateResponse.getRelease();


                if (release != null) {
                    mApkUrl = release.getUrl();
                    SPUtil.putString(mContext, SPKEY_DOWNLOAD_MD5, release.getMd5());
                }

                if (mUpdateListener != null) {
                    if (mVersionCode < updateResponse.getIncompatibleVersion()) {
                        //强制更新
                        isForceUpdate = true;
                        mUpdateListener.onUpdateReturned(UpdateStatus.FORCE, updateResponse);
                    } else if (mVersionCode < release.getVersionCode()) {
                        //有更新
                        mUpdateListener.onUpdateReturned(UpdateStatus.YES, updateResponse);
                    } else {
                        //无更新
                        mUpdateListener.onUpdateReturned(UpdateStatus.NO, updateResponse);
                    }

                }
                if (mVersionCode <= updateResponse.getIncompatibleVersion()) {
                    //强制更新
                    resultCode = FORCE;
                    showResultDialog(null, resultCode, updateResponse);
                } else if (mVersionCode < release.getVersionCode()) {
                    //有更新
                    resultCode = UPDATE;
                    showResultDialog(null, resultCode, updateResponse);
                } else {
                    //无更新
                    resultCode = NOUPDATE;
                    if (isForce) {
                        showResultDialog("已经是最新版本啦...", resultCode, updateResponse);
                    }
                }

            }
            super.onPostExecute(response);
        }
    }

    private UpdateResponse parseJsonString(String response) {
        JSONTokener jsonTokener = new JSONTokener(response);
        JSONObject jsonObj = null;
        try {
            jsonObj = (JSONObject) jsonTokener.nextValue();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        UpdateResponse updateResponse = new UpdateResponse();
        try {
            updateResponse.setCode(jsonObj.getInt("code"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            updateResponse.setInfo(jsonObj.getString("info"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            updateResponse.setName(jsonObj.getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            updateResponse.setIncompatibleVersion(jsonObj.getInt("incompatibleVersion"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        UpdateResponse.ReleaseBean releaseBean = new UpdateResponse.ReleaseBean();
        JSONObject releaseJson = null;
        try {
            releaseJson = jsonObj.getJSONObject("release");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (releaseJson != null) {
            try {
                releaseBean.setVersionName(releaseJson.getString("versionName"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                releaseBean.setVersionCode(releaseJson.getInt("versionCode"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                releaseBean.setChangeLog(releaseJson.getString("changeLog"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                releaseBean.setTargetSdkVersion(releaseJson.getInt("targetSdkVersion"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                releaseBean.setMinSdkVersion(releaseJson.getInt("minSdkVersion"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                releaseBean.setUpdateDate(releaseJson.getString("updateDate"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                releaseBean.setChannel(releaseJson.getString("channel"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                releaseBean.setUrl(releaseJson.getString("url"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                releaseBean.setMd5(releaseJson.getString("md5"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                releaseBean.setSize(releaseJson.getInt("size"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            updateResponse.setRelease(releaseBean);
        }
        return updateResponse;
    }

    private class DownloadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_DOWNLOAD_UPDATE) {
                int[] statusBytes = (int[]) msg.obj;
                switch (statusBytes[2]) {
                    case DownloadManager.STATUS_PENDING:

                        break;
                    case DownloadManager.STATUS_RUNNING:
                        int currProgress = statusBytes[0] * 100 / statusBytes[1];
                        if (preProgress < currProgress) {
                            if (mDownloadListener != null) {
                                mDownloadListener.onDownloadUpdate(currProgress, statusBytes[0], statusBytes[1]);
                            }
                            setDownloadProgress(currProgress);
                            Log.d(TAG, "curr : " + currProgress);
                        }
                        preProgress = currProgress;
                        break;
                    case DownloadManager.STATUS_PAUSED:
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
//                        Log.d(TAG, "handleMessage : file " + mDownloadManagerUtil.getFileName(mDownloadId));
                        break;
                    case DownloadManager.STATUS_FAILED:
                        if (mDownloadDialog != null) {
                            mDownloadDialog.dismiss();
                        }
                        if (mDownloadListener != null) {
                            mDownloadListener.onDownloadEnd(UpdateStatus.DOWNLOAD_COMPLETE_FAIL, null);
                        }
                        showResultDialog("下载失败,请稍后重试", ERROR, null);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
