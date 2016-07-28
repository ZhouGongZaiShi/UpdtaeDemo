package zhouxin.updatedemo;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.binfun.update.UpdateStatus;
import com.binfun.update.bean.ApkInfo;
import com.binfun.update.callback.OnDownloadListener;
import com.binfun.update.callback.OnUpdateListener;
import com.binfun.update.manager.UpdateManager;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private UpdateManager mUpdateManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUpdateManager = UpdateManager.getInstance(this);
        mUpdateManager.setParms(getParms());
        mUpdateManager.isShowProgressDialog(false);
        mUpdateManager.isShowNoUpdate(false);
        mUpdateManager.setOnUpdateListener(new OnUpdateListener() {
            @Override
            public void onCompleted(ApkInfo info) {
                System.out.println("apkinfo :　　onCompleted " + info.toString());
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        });
        mUpdateManager.setOnDownloadListener(new OnDownloadListener(){



            @Override
            public void onDownloadUpdate(int progress) {
                Log.d(TAG, "onDownloadUpdate : " +progress);
            }

            @Override
            public void onDownloadEnd(int result, String file) {
                switch (result){
                    case UpdateStatus.DOWNLOAD_COMPLETE_SUCCESS:
                        Log.d(TAG, "onDownloadEnd : 下载成功  file" + file);
                        break;
                    case UpdateStatus.DOWNLOAD_COMPLETE_FAIL:
                        Log.d(TAG, "onDownloadEnd : 下载失败  file" + file);
                        break;
                    default:
                        break;
                }
            }
        });

        mUpdateManager.checkUpdate();
    }

    private Map<String, String> getParms() {
        Map<String, String> parms = new ArrayMap<>();
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo != null) {
            parms.put("versioncode", String.valueOf(packageInfo.versionCode));
        }
        parms.put("packagename", getPackageName());
        return parms;
    }

    public void checkUpdate(View v) {

        mUpdateManager.isShowProgressDialog(true);
        mUpdateManager.isShowNoUpdate(true);
        mUpdateManager.checkUpdate();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUpdateManager.unRegister();
    }
}
