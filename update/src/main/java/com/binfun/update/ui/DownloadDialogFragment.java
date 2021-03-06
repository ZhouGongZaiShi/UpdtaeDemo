package com.binfun.update.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.binfun.update.event.CancelDialogEvent;
import com.binfun.update.rxbus.RxBus;


/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/22 16:01
 */
public class DownloadDialogFragment extends DialogFragment {
private static final String TAG = "ProgressDialogFragment";

    public static DownloadDialogFragment newInstance(){
        DownloadDialogFragment fragment = new DownloadDialogFragment();
        fragment.setCancelable(false);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setTitle("正在下载中...");
        dialog.setCancelable(false);
        dialog.setMax(100);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        RxBus.getDefault().post(new CancelDialogEvent());
        super.onDismiss(dialog);
    }
}
