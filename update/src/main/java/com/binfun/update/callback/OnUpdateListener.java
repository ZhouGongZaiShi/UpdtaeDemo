package com.binfun.update.callback;

import com.binfun.update.bean.UpdateResponse;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/28 15:11
 */
public interface OnUpdateListener {
    void onUpdateReturned(int statusCode,UpdateResponse updateInfo);
}