package com.binfun.update;

import android.database.ContentObserver;
import android.os.Handler;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/8/8 14:55
 */
public class DownloadObserver extends ContentObserver{


    public DownloadObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
        updateView();
    }

    private void updateView() {

    }
}
