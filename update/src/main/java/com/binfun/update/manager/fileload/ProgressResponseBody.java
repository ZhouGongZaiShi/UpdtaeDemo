package com.binfun.update.manager.fileload;

import com.binfun.update.rxbus.RxBus;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/27 16:00
 */
public class ProgressResponseBody extends ResponseBody {
    private ResponseBody mResponseBody;
    private BufferedSource bufferedSource;

    public ProgressResponseBody(ResponseBody responseBody) {
        mResponseBody = responseBody;
    }

    @Override
    public MediaType contentType() {
        return mResponseBody.contentType();
    }

    @Override
    public long contentLength() {
        return mResponseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        return Okio.buffer(new ForwardingSource(mResponseBody.source()) {

            long totalBytesRead = 0L;
            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                RxBus.getDefault().post(new FileLoadingBean(totalBytesRead,contentLength()));
                return bytesRead;
            }
        });
    }
}
