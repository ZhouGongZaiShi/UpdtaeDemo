package com.binfun.update.callback;

import com.binfun.update.bean.ApkInfo;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/28 15:11
 */
public interface OnUpdateListener {
    void onCompleted(ApkInfo info);

    void onError(Throwable e);
}