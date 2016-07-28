package com.binfun.update.manager.fileload;

/**
 * Created by zs on 2016/7/7.
 */
public class FileLoadingBean {
    /**
     * 文件大小
     */
    long total;
    /**
     * 已下载大小
     */
    long progress;

    public long getProgress() {
        return progress;
    }

    public long getTotal() {
        return total;
    }

    public FileLoadingBean(long progress,long total) {
        this.total = total;
        this.progress = progress;
    }
}
