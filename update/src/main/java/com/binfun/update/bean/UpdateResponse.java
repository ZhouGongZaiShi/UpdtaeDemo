package com.binfun.update.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/28 18:22
 */
public class UpdateResponse {


    /**
     * package : com.iflyor.binfuntv
     * downloadServer : http://apkdl.binfun.tv
     * mkbrokers : ["http://183.131.135.142:3916/v1/caches"]
     * tracker : {"trackerServerUrl":"http://cdn.freebsd.org.cn/upload/","enabled":false}
     * sc : {"sysId":11,"xDomain":".easetuner.com"}
     * description : 光芒体育TV版
     * incompatibleVersion : 30
     * platform : 0
     * name : 光芒体育TV版
     * release : {"versionName":"0.1.150807.1","versionCode":54,"changeLog":"test\r\ntest","targetSdkVersion":"21","minSdkVersion":"14","updateDate":"2016-08-03T11:33:53.073Z","channel":"anzhuo","url":"http://apkdl.binfun.tv:5000/apk/Gm4TV0.1_54v0.1.150807.1_anzhuo_release.apk","md5":"70d90ecb1987942a5cd9e04995b1ae12"}
     */

    @SerializedName("package")
    private String packageX;
    private String downloadServer;
    /**
     * trackerServerUrl : http://cdn.freebsd.org.cn/upload/
     * enabled : false
     */

    private TrackerBean tracker;
    /**
     * sysId : 11
     * xDomain : .easetuner.com
     */

    private ScBean sc;
    private String description;
    private int incompatibleVersion;
    private int platform;
    private String name;
    /**
     * versionName : 0.1.150807.1
     * versionCode : 54
     * changeLog : test
     test
     * targetSdkVersion : 21
     * minSdkVersion : 14
     * updateDate : 2016-08-03T11:33:53.073Z
     * channel : anzhuo
     * url : http://apkdl.binfun.tv:5000/apk/Gm4TV0.1_54v0.1.150807.1_anzhuo_release.apk
     * md5 : 70d90ecb1987942a5cd9e04995b1ae12
     */

    private ReleaseBean release;
    private List<String> mkbrokers;

    public String getPackageX() {
        return packageX;
    }

    public void setPackageX(String packageX) {
        this.packageX = packageX;
    }

    public String getDownloadServer() {
        return downloadServer;
    }

    public void setDownloadServer(String downloadServer) {
        this.downloadServer = downloadServer;
    }

    public TrackerBean getTracker() {
        return tracker;
    }

    public void setTracker(TrackerBean tracker) {
        this.tracker = tracker;
    }

    public ScBean getSc() {
        return sc;
    }

    public void setSc(ScBean sc) {
        this.sc = sc;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIncompatibleVersion() {
        return incompatibleVersion;
    }

    public void setIncompatibleVersion(int incompatibleVersion) {
        this.incompatibleVersion = incompatibleVersion;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ReleaseBean getRelease() {
        return release;
    }

    public void setRelease(ReleaseBean release) {
        this.release = release;
    }

    public List<String> getMkbrokers() {
        return mkbrokers;
    }

    public void setMkbrokers(List<String> mkbrokers) {
        this.mkbrokers = mkbrokers;
    }

    public static class TrackerBean {
        private String trackerServerUrl;
        private boolean enabled;

        public String getTrackerServerUrl() {
            return trackerServerUrl;
        }

        public void setTrackerServerUrl(String trackerServerUrl) {
            this.trackerServerUrl = trackerServerUrl;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class ScBean {
        private int sysId;
        private String xDomain;

        public int getSysId() {
            return sysId;
        }

        public void setSysId(int sysId) {
            this.sysId = sysId;
        }

        public String getXDomain() {
            return xDomain;
        }

        public void setXDomain(String xDomain) {
            this.xDomain = xDomain;
        }
    }

    public static class ReleaseBean {
        private String versionName;
        private int versionCode;
        private String changeLog;
        private String targetSdkVersion;
        private String minSdkVersion;
        private String updateDate;
        private String channel;
        private String url;
        private String md5;

        public String getVersionName() {
            return versionName;
        }

        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }

        public int getVersionCode() {
            return versionCode;
        }

        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }

        public String getChangeLog() {
            return changeLog;
        }

        public void setChangeLog(String changeLog) {
            this.changeLog = changeLog;
        }

        public String getTargetSdkVersion() {
            return targetSdkVersion;
        }

        public void setTargetSdkVersion(String targetSdkVersion) {
            this.targetSdkVersion = targetSdkVersion;
        }

        public String getMinSdkVersion() {
            return minSdkVersion;
        }

        public void setMinSdkVersion(String minSdkVersion) {
            this.minSdkVersion = minSdkVersion;
        }

        public String getUpdateDate() {
            return updateDate;
        }

        public void setUpdateDate(String updateDate) {
            this.updateDate = updateDate;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }
    }
}
