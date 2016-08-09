package com.binfun.update.manager;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.binfun.update.bean.UpdateResponse;
import com.binfun.update.callback.OnDownloadListener;
import com.binfun.update.callback.OnUpdateListener;
import com.binfun.update.common.Const;
import com.binfun.update.common.UpdateStatus;
import com.binfun.update.utils.DownloadManagerUtil;
import com.binfun.update.utils.HttpUtil;
import com.binfun.update.utils.MD5Util;
import com.binfun.update.utils.NetUtil;
import com.binfun.update.utils.SPUtil;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Map;


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

    public final static String SPKEY_DOWNLOAD_ID = "download_id";
    public final static String SPKEY_DOWNLOAD_MD5 = "download_md5";
    public final static String SPKEY_DOWNLOAD_FILE = "download_file";

    public static final int NOUPDATE = 0;
    public static final int UPDATE = 1;
    public static final int FORCE = 2;
    public static final int ERROR = 3;

    private static final int CANCEL_DOWNLOAD = 233;
    private static final int DOWNLOAD_UPDATE = 100;


    private int mDownloadPid;
    private Intent mDownloadIntent;

    private int mVersionCode;


    private final DownloadObserver mDownloadObserver;
    private final DownloadManager mManager;
    private final DownloadManagerUtil mDownloadManagerUtil;
    private long mDownloadId;
    private boolean isOnlyWifi;
    private CheckAsyncTask mCheckAsyncTask;
    private final Gson mGson;
    private String mChannel;
    private String mAppName;
    private String mApkMD5;

    @IntDef({NOUPDATE, UPDATE, FORCE, ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResultStatus {
    }


    public final static String APK_URL = "apk_url";

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


    private String mApkUrl;

    private int preProgress;


    private Handler mHandler;

    private boolean isForce;

    private static volatile UpdateManager mInstance;
    private ProgressDialog mDownloadDialog;
    private ProgressDialog mProgressDialog;
    private AlertDialog mResultDialog;

    private UpdateManager(Context context) {
        mContext = context.getApplicationContext();
        mHandler = new DownloadHandler();
        mGson = new Gson();
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

    public static UpdateManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (UpdateManager.class) {
                if (mInstance == null) {
                    mInstance = new UpdateManager(context);
                }
            }
        }
        return mInstance;
    }


    public void autoUpdate() {
        update(false);
    }

    public void forceUpdate() {
        update(true);
    }


    public void update(final boolean force) {
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
//            url.append("package=").append(mContext.getPackageName()).append("&channel=").append(mChannel);
            url.append("package=").append("com.iflyor.bindasuntv").append("&channel=").append(mChannel);
        }
        mCheckAsyncTask.execute(url.toString());

    }


    private void showResultDialog(String info, int code, @Nullable UpdateResponse response) {
        if (isAutoPopup) {
            if (isForce) {
                //强制状态下显示所有结果的对话框
                createResultDialog(info, code, response);
            } else {
                if (code == UPDATE || code == FORCE ||code ==ERROR) {
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
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
                } else {
                    mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
                }
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
        //下载之前删除上次下载的文件
        String fileName = SPUtil.getString(mContext,SPKEY_DOWNLOAD_FILE);
        if (!TextUtils.isEmpty(fileName)) {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        }

        if (NetUtil.isConnected(mContext)){
            if (mUpdateListener != null) {
                    mUpdateListener.onUpdateReturned(UpdateStatus.NoneNET,null);
            }
        }

        Uri uri = Uri.parse(mApkUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        if (isOnlyWifi) {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        } else {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        }
        String title = TextUtils.isEmpty(mAppName) ? "应用升级" : mAppName;
        request.setTitle(title);
//        request.setVisibleInDownloadsUi(false);
        request.setDescription(title + "开始下载...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.allowScanningByMediaScanner();
        request.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS, title);
//        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
//        File dir = new File(destFileDir);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        File file = new File(dir, destFileName);
//        if (file.exists() & file.isFile()) {
//            //防止apk文件数量过多
//            file.delete();
//        }
//        request.setDestinationInExternalPublicDir("/BF_DEFAULT_DIR/", "binfun.apk");

        mDownloadId = mManager.enqueue(request);
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
        message.what = DOWNLOAD_UPDATE;
        message.obj = bytesAndStatus;
        mHandler.sendMessage(message);
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

    public void setUpdateOnlyWifi(boolean isOnlyWifi) {
        this.isOnlyWifi = isOnlyWifi;
    }

    public void setParms(@NonNull Map<String, String> parms) {
        mParms = parms;
    }


    public void setUpdateAutoPopup(boolean isAutoPopup) {
        this.isAutoPopup = isAutoPopup;
    }


//    /**
//     * 设置APK文件下载文件夹路径
//     *
//     * @param fileDir 文件夹路径
//     */
//    public void setFileDir(String fileDir) {
//        destFileDir = fileDir;
//    }
//
//    /**
//     * 设置APK文件名
//     *
//     * @param fileName 文件名
//     */
//    public void setFileName(String fileName) {
//        destFileName = fileName;
//    }

    public void setVersionCode(int versionCode) {
        mVersionCode = versionCode;
    }

    public void setChannelName(@NonNull String channel) {
        mChannel = channel;
    }

    public void cancelCheckUpdate() {
        if (mCheckAsyncTask != null && mCheckAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mCheckAsyncTask.cancel(true);
        }
    }

    public void unRegister() {
        cancelCheckUpdate();
        hideDialogs();
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
                    mManager.remove(mDownloadId);
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

    public String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }


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
                showResultDialog("请求失败,请稍后再试", ERROR, null);
                return;
            } else {
                updateResponse = mGson.fromJson(response, UpdateResponse.class);
//                JSONTokener jsonTokener = new JSONTokener(response);
//                try {
//                    JSONObject jsonObj = (JSONObject) jsonTokener.nextValue();
//                    UpdateResponse updateResponse1 = new UpdateResponse();
//                    updateResponse1.setCode(jsonObj.getInt("code"));
//                    updateResponse1.setInfo(jsonObj.getString("info"));
//                    updateResponse1.setName(jsonObj.getString("name"));
//                    Log.d(TAG, "onPostExecute : " );
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }

            }
            if (updateResponse == null) {
                showResultDialog("服务器繁忙,请稍后再试", ERROR, null);
                //无数据
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
                    SPUtil.putString(mContext,SPKEY_DOWNLOAD_MD5,release.getMd5());
                }

                if (mUpdateListener != null) {
                    //用户设置了回调
                    if (mVersionCode < updateResponse.getIncompatibleVersion()) {
                        //强制更新
                        mUpdateListener.onUpdateReturned(UpdateStatus.FORCE, updateResponse);
                    } else if (mVersionCode < release.getVersionCode()) {
                        //有更新
                        mUpdateListener.onUpdateReturned(UpdateStatus.YES, updateResponse);
                    } else {
                        //无更新
                        mUpdateListener.onUpdateReturned(UpdateStatus.NO, updateResponse);
                    }

                } else {
                    //用户未设置回调
                    if (mVersionCode < updateResponse.getIncompatibleVersion()) {
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
            }
            super.onPostExecute(response);
        }
    }

    private class DownloadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == DOWNLOAD_UPDATE) {
                int[] statusBytes = (int[]) msg.obj;
                System.out.println("handleMessage : " + statusBytes[2]);
                switch (statusBytes[2]) {
                    case DownloadManager.STATUS_PENDING:

                        break;
                    case DownloadManager.STATUS_RUNNING:
                        int currProgress = statusBytes[0] * 100 / statusBytes[1];
                        if (preProgress < currProgress) {
                            if (mDownloadListener != null) {
                                mDownloadListener.onDownloadUpdate(currProgress);
                            } else {
                                setDownloadProgress(currProgress);
                            }
                            Log.d(TAG, "curr : " + currProgress);
                        }
                        preProgress = currProgress;
                        break;
                    case DownloadManager.STATUS_PAUSED:
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        Log.d(TAG, "handleMessage : file " + mDownloadManagerUtil.getFileName(mDownloadId));
                        break;
                    case DownloadManager.STATUS_FAILED:
                        if (mDownloadDialog != null) {
                            mDownloadDialog.dismiss();
                        }
                        if (mDownloadListener != null) {
                            mDownloadListener.onDownloadEnd(UpdateStatus.DOWNLOAD_COMPLETE_FAIL, null);
                        } else {
                            showResultDialog("下载失败,请稍后重试", ERROR, null);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
