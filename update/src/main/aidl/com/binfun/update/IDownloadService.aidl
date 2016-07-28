// IDownloadService.aidl
package com.binfun.update;

// Declare any non-default types here with import statements
import com.binfun.update.IDownloadCallback;

interface IDownloadService {
    void registerDownloadCallback(IDownloadCallback cb);
    void unregisterDownloadCallback(IDownloadCallback cb);
}
