package com.binfun.update.callback;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/28 15:11
 */
public interface OnDownloadListener {

    void onDownloadUpdate(int progress);

    void onDownloadEnd(int result, String file);
}
