package zhouxin.updatedemo;

import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.binfun.update.manager.UpdateManager;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private UpdateManager mUpdateManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUpdateManager = UpdateManager.init(this, "shafa","dss");
//        mUpdateManager = UpdateManager.init(this, "com.iflyor.binfuntv.game","shafa",10);

//        mUpdateManager.setOnUpdateListener(new OnUpdateListener() {
//
//            @Override
//            public void onUpdateReturned(int updateStatus, UpdateResponse updateInfo) {
//                Log.d(TAG, "onUpdateReturned : " + updateStatus);
//            }
//        });
//        mUpdateManager.setOnDownloadListener(new OnDownloadListener(){
//
//
//
//            @Override
//            public void onDownloadUpdate(int progress) {
//                Log.d(TAG, "onDownloadUpdate : " +progress);
//            }
//
//            @Override
//            public void onDownloadEnd(int result, String file) {
//                switch (result){
//                    case UpdateStatus.DOWNLOAD_COMPLETE_SUCCESS:
//                        Log.d(TAG, "onDownloadEnd : 下载成功  file" + file);
//                        break;
//                    case UpdateStatus.DOWNLOAD_COMPLETE_FAIL:
//                        Log.d(TAG, "onDownloadEnd : 下载失败  file" + file);
//                        break;
//                    default:
//                        break;
//                }
//            }
//        });
        mUpdateManager.setUpdateOnlyWifi(true);

//        mUpdateManager.setUpdateAutoPopup(false);
//        mUpdateManager.setOnUpdateListener(new OnUpdateListener() {
//            @Override
//            public void onUpdateReturned(int statusCode, UpdateResponse updateInfo) {
//                switch (statusCode) {
//                    case UpdateStatus.YES:
//                        Toast.makeText(MainActivity.this, "有更新", Toast.LENGTH_SHORT).show();
//                        mUpdateManager.download();
//                        break;
//                    case UpdateStatus.NO:
//                        Toast.makeText(MainActivity.this, "没有更新", Toast.LENGTH_SHORT).show();
//                        break;
//                    case UpdateStatus.TIMEOUT:
//                        Toast.makeText(MainActivity.this, "超时", Toast.LENGTH_SHORT).show();
//                        break;
//                    case UpdateStatus.FORCE:
//                        Toast.makeText(MainActivity.this, "强制更新", Toast.LENGTH_SHORT).show();
//                        break;
//                    default:
//                        break;
//                }
//            }
//        });

//        mUpdateManager.setUpdateAutoPopup(false);
//        mUpdateManager.setOnDownloadListener(new OnDownloadListener() {
//            @Override
//            public void onDownloadStart() {
//                Log.d(TAG, "download start");
//            }
//
//            @Override
//            public void onDownloadUpdate(int currProgress, int statusByte, int progress) {
//                Log.d(TAG, "onDownloadUpdate :progress " + currProgress +"  progressbyte " +statusByte +" total " +progress );
//            }
//
//            @Override
//            public void onDownloadEnd(int result, String file) {
//                switch (result) {
//                    case UpdateStatus.DOWNLOAD_COMPLETE_SUCCESS:
//                        //下载成功
//                        UpdateManager.installApk(MainActivity.this, new File(file));
//                        break;
//                    case UpdateStatus.DOWNLOAD_COMPLETE_FAIL:
//                        //下载失败
//                        break;
//                    case UpdateStatus.DOWNLOAD_CHECK_MD5_FAIL:
//                        //md5校验失败
//                        break;
//                    case UpdateStatus.CANCEL_DOWNLOAD:
//                        //取消下载
//                        break;
//                    default:
//                        break;
//                }
//            }
//        });
        mUpdateManager.autoUpdate(this);


    }


    private Map<String, String> getParms() {
        Map<String, String> parms = new ArrayMap<>();
//        PackageInfo packageInfo = null;
//        try {
//            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//        if (packageInfo != null) {
//            parms.put("versioncode", String.valueOf(packageInfo.versionCode));
//        }
//            parms.put("package", getPackageName());
        parms.put("package", "com.iflyor.binfuntv");

        parms.put("channel", "anzhuo");
        return parms;
    }

    public void checkUpdate(View v) {
        mUpdateManager.forceUpdate(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUpdateManager.stop();
//        mUpdateManager.unRegister();
//        Process.killProcess(Process.myPid());
//        System.exit(0);
    }
}
