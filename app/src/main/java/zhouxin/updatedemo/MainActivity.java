package zhouxin.updatedemo;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
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

        mUpdateManager = UpdateManager.getInstance(this.getApplicationContext());
//        mUpdateManager.setParms(getParms());
        mUpdateManager.setChannelName("shafa");
        mUpdateManager.setVersionCode(getVersionCode());
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

        mUpdateManager.autoUpdate();





    }



    public int getVersionCode(){
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo != null) {
            return packageInfo.versionCode;
        }else {
            return -1;
        }
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

        parms.put("channel","anzhuo");
        return parms;
    }

    public void checkUpdate(View v) {

        
        mUpdateManager.forceUpdate();
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUpdateManager.unRegister();
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}
