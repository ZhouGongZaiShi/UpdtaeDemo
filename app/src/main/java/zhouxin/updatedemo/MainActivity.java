package zhouxin.updatedemo;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.binfun.update.bean.ApkInfo;
import com.binfun.update.manager.UpdateManager;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private UpdateManager mUpdateManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUpdateManager = new UpdateManager(this);
        mUpdateManager.setParms(getParms());
        mUpdateManager.isShowProgressDialog(false);
        mUpdateManager.isShowNoUpdate(false);
        mUpdateManager.setUpdateListener(new UpdateManager.UpdateListener() {
            @Override
            public void onCompleted(ApkInfo info) {
                System.out.println("apkinfo :　　onCompleted " + info.toString());
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
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



}
