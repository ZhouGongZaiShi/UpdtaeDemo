package com.binfun.update.manager.fileload;

import com.binfun.update.rxbus.RxBus;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

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
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(mResponseBody.source()));
        }
        return bufferedSource;
    }

    public Source source(Source source) {
        return new ForwardingSource(source) {
            long totalBytesRead = 0L;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                RxBus.getDefault().post(new FileLoadingBean(contentLength(), bytesRead));
                return bytesRead;
            }
        };
    }
}
