package com.binfun.update.bean;


import java.util.List;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/28 18:22
 */
public class UpdateResponse {

    /**
     * package : com.iflyor.binfuntv.game
     * mkbrokers : ["http://183.131.135.142:3916/v1/caches"]
     * tracker : {"trackerServerUrl":"http://cdn.freebsd.org.cn/upload/","enabled":true}
     * sc : {"sysId":11,"xDomain":".easetuner.com"}
     * description : 缤纷电竞TV版
     * incompatibleVersion : 0
     * name : 缤纷电竞TV版
     * code : 0
     * info :
     * release : {"versionName":"0.1.160721.1","versionCode":80,"changeLog":"1. 重写界面，体验更流畅 2. 修复已知bug","targetSdkVersion":22,"minSdkVersion":14,"updateDate":"2016-08-05T06:37:41.600Z","channel":"shafa","url":"http://apkdl.binfun.tv/apk/BinFun4TV0.1_80v0.1.160721.1_shafa_release.apk","md5":"ec7b84edb0a4008976e904aa3f1aafb4","size":6789170}
     */

    private String packageX;
    /**
     * trackerServerUrl : http://cdn.freebsd.org.cn/upload/
     * enabled : true
     */

    private TrackerBean tracker;
    /**
     * sysId : 11
     * xDomain : .easetuner.com
     */

    private ScBean sc;
    private String description;
    private int incompatibleVersion;
    private String name;
    private int code;
    private String info;
    /**
     * versionName : 0.1.160721.1
     * versionCode : 80
     * changeLog : 1. 重写界面，体验更流畅 2. 修复已知bug
     * targetSdkVersion : 22
     * minSdkVersion : 14
     * updateDate : 2016-08-05T06:37:41.600Z
     * channel : shafa
     * url : http://apkdl.binfun.tv/apk/BinFun4TV0.1_80v0.1.160721.1_shafa_release.apk
     * md5 : ec7b84edb0a4008976e904aa3f1aafb4
     * size : 6789170
     */

    private ReleaseBean release;
    private List<String> mkbrokers;

    public String getPackageX() {
        return packageX;
    }

    public void setPackageX(String packageX) {
        this.packageX = packageX;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
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
        private int targetSdkVersion;
        private int minSdkVersion;
        private String updateDate;
        private String channel;
        private String url;
        private String md5;
        private int size;

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

        public int getTargetSdkVersion() {
            return targetSdkVersion;
        }

        public void setTargetSdkVersion(int targetSdkVersion) {
            this.targetSdkVersion = targetSdkVersion;
        }

        public int getMinSdkVersion() {
            return minSdkVersion;
        }

        public void setMinSdkVersion(int minSdkVersion) {
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

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }

    @Override
    public String toString() {
        return "UpdateResponse{" +
                "packageX='" + packageX + '\'' +
                ", tracker=" + tracker +
                ", sc=" + sc +
                ", description='" + description + '\'' +
                ", incompatibleVersion=" + incompatibleVersion +
                ", name='" + name + '\'' +
                ", code=" + code +
                ", info='" + info + '\'' +
                ", release=" + release +
                ", mkbrokers=" + mkbrokers +
                '}';
    }
}
