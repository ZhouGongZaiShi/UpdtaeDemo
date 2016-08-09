package com.binfun.update.utils;

import android.content.Context;
import android.os.Environment;

import java.text.DecimalFormat;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/8/9 11:52
 */
public class SDUtil {
    private static final String TAG = "SDUtil";

    /**
     * Check if the primary "external" storage device is available.
     *
     * @return
     */
    public static boolean hasSDCardMounted() {
        String state = Environment.getExternalStorageState();
        return state != null && state.equals(Environment.MEDIA_MOUNTED);
    }


//    public static boolean isUseSD(Context context) {
//        File path = context.get
//        StatFs stat = new StatFs(path.getPath());
//        long blockCount = stat.getBlockCount();
//        long blockSize = stat.getBlockSize();
//        long availableBlocks = stat.getAvailableBlocks();
//        String totalSize = Formatter.formatFileSize(context, blockCount*blockSize);//格式化获得SD卡总容量
//        String availableSize = Formatter.formatFileSize(context, availableBlocks*blockSize);//获得SD卡可用容量
//        System.out.println("SD卡总容量:"+totalSize+"\nSD卡可用容量:"+availableSize+"\n");
//        File path1 = Environment.getDataDirectory();
//        StatFs stat1 = new StatFs(path1.getPath());
//        long blockCount1 = stat1.getBlockCount();
//        long blockSize1 = stat1.getBlockSize();
//        long availableBlocks1 = stat1.getAvailableBlocks();
//        String totalSize1 = Formatter.formatFileSize(context, blockCount1*blockSize1);
//        String availableSize1 = Formatter.formatFileSize(context, availableBlocks1*blockSize1);
//        System.out.println("手机Rom总容量:"+totalSize1+"\n手机Rom可用容量:"+availableSize1);
//        System.out.println("path sd "+Environment.getExternalStorageDirectory().getAbsolutePath() + " mobile " +Environment.getDataDirectory().getAbsolutePath());
//        System.out.println("  sd  useable " + getAppSize(Environment.getExternalStorageDirectory().getUsableSpace()) + " mobile useable  " + getAppSize(Environment.getRootDirectory().getUsableSpace()));
//        return hasSDCardMounted() && Environment.getExternalStorageDirectory().getUsableSpace() > Environment.getDataDirectory().getUsableSpace();
//    }


    private static void getRomSpace(Context context) {

    }

    static final DecimalFormat DOUBLE_DECIMAL_FORMAT = new DecimalFormat("0.##");

    public static final int MB_2_BYTE = 1024 * 1024;
    public static final int KB_2_BYTE = 1024;
    /**
     * @param size
     * @return
     */
    public static CharSequence getAppSize(long size) {
        if (size <= 0) {
            return "0M";
        }

        if (size >= MB_2_BYTE) {
            return new StringBuilder(16).append(DOUBLE_DECIMAL_FORMAT.format((double) size / MB_2_BYTE)).append("M");
        } else if (size >= KB_2_BYTE) {
            return new StringBuilder(16).append(DOUBLE_DECIMAL_FORMAT.format((double) size / KB_2_BYTE)).append("K");
        } else {
            return size + "B";
        }
    }
}