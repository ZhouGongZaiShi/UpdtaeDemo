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
import android.view.Window;

import com.binfun.update.event.CancelDialogEvent;
import com.binfun.update.rxbus.RxBus;


/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/22 16:01
 */
public class ProgressDialogFragment extends DialogFragment {
private static final String TAG = "ProgressDialogFragment";



    public static ProgressDialogFragment newInstance(){
        return new ProgressDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage("正在检查更新...");
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        RxBus.getDefault().post(new CancelDialogEvent());
        super.onDismiss(dialog);
    }
}
