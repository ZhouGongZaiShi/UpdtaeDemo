package com.binfun.update.bean;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/25 16:38
 */
public class ApkInfo {

    /**
     * update : true
     * force : false
     * apk_url : http://192.168.1.166:8080/updateDemo/BF.apk
     * update_log : 缤纷更新啦！还请亲们赶紧升级哦！
     * target_size : 6791168
     */

    private boolean update;
    private boolean force;
    private String apk_url;
    private String update_log;
    private String target_size;

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getApk_url() {
        return apk_url;
    }

    public void setApk_url(String apk_url) {
        this.apk_url = apk_url;
    }

    public String getUpdate_log() {
        return update_log;
    }

    public void setUpdate_log(String update_log) {
        this.update_log = update_log;
    }

    public String getTarget_size() {
        return target_size;
    }

    public void setTarget_size(String target_size) {
        this.target_size = target_size;
    }

    @Override
    public String toString() {
        return "ApkInfo{" +
                "update=" + update +
                ", force=" + force +
                ", apk_url='" + apk_url + '\'' +
                ", update_log='" + update_log + '\'' +
                ", target_size='" + target_size + '\'' +
                '}';
    }
}
