package com.binfun.update.manager.fileload;

import com.binfun.update.rxbus.RxBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/27 17:05
 */
public abstract class FileCallback implements Callback<ResponseBody> {
    /**
     * 订阅下载进度
     */
    private CompositeSubscription rxSubscriptions = new CompositeSubscription();
    /**
     * 目标文件存储的文件夹路径
     */
    private String destFileDir;
    /**
     * 目标文件存储的文件名
     */
    private String destFileName;


    public FileCallback(String destFileDir, String destFileName) {
        this.destFileDir = destFileDir;
        this.destFileName = destFileName;
        subscribeLoadProgress();// 订阅下载进度
    }



    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        try {
            saveFile(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private File saveFile(Response<ResponseBody> response) throws Exception {
        InputStream in = null;
        FileOutputStream out = null;
        byte[] buffer = new byte[2048 * 10];
        int len;
        try {
            File dir = new File(destFileDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            in = response.body().byteStream();
            File file = new File(dir, destFileName);
            out = new FileOutputStream(file);
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            onSuccess(file);
            unSubscribe();
            return file;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }


    /**
     * 订阅文件下载进度
     */
    private void subscribeLoadProgress() {
        rxSubscriptions.add(RxBus.getDefault()
                .toObservable(FileLoadingBean.class)
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<FileLoadingBean>() {
                    @Override
                    public void call(FileLoadingBean fileLoadEvent) {
                        onDownloading(fileLoadEvent.getProgress(), fileLoadEvent.getTotal());
                    }
                }));
    }

    private void unSubscribe() {
        if (!rxSubscriptions.isUnsubscribed()){
            rxSubscriptions.unsubscribe();
        }
    }


    /**
     * 成功后回调
     */
    public abstract void onSuccess(File file);

    /**
     * 下载过程回调
     */
    public abstract void onDownloading(long progress, long total);
}
