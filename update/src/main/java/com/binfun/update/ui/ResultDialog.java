package com.binfun.update.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v7.app.AlertDialog;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/8/2 18:26
 */
public class ResultDialog extends AlertDialog{
    protected ResultDialog(@NonNull Context context) {
        super(context);
    }

    protected ResultDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    protected ResultDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }
}
