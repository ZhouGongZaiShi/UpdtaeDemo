// IDownloadCallback.aidl
package com.binfun.update;

// Declare any non-default types here with import statements

interface IDownloadCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
//    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
//            double aDouble, String aString);

    void onDownloadUpdate(long progress,long total);
    void onDownloadEnd(int result,String file);
}
