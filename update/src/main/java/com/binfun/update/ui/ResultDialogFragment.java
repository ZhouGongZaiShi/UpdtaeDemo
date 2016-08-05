package com.binfun.update.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.binfun.update.utils.NetUtil;

/**
 * 描述 :
 * 作者 : 周鑫
 * 创建日期 : 2016/7/25 17:57
 */
public class ResultDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String INFO = "info";
    private static final String RESULT_CODE = "result_code";

    public static final int NOUPDATE = 0;
    public static final int UPDATE = 1;
    public static final int FORCE = 2;
    public static final int ERROR = 3;
    public static final int NONET = 4;
    public static final int CHCEKING = 5;
    private int resultCode = NOUPDATE;

    public static ResultDialogFragment newInstance(String info, int code) {
        ResultDialogFragment fragment = new ResultDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(INFO, info);
        bundle.putInt(RESULT_CODE, code);
        fragment.setArguments(bundle);
        return fragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String info = null;
        int code = 0;
        Bundle arguments = getArguments();
        if (arguments != null) {
            info = arguments.getString(INFO);
            code = arguments.getInt(RESULT_CODE);
            resultCode = code;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("检查更新");
        builder.setMessage(info);

        switch (code) {
            case NOUPDATE:
                builder.setNegativeButton("确认", this);
                break;
            case UPDATE:
                builder.setPositiveButton("立即更新", this);
                builder.setNegativeButton("以后再说", this);
                break;
            case FORCE:
                builder.setMessage(info);
                setCancelable(false);
                builder.setPositiveButton("立即更新", this);
                break;
            case ERROR:
                builder.setNegativeButton("确认", this);
                break;
            case NONET:
                builder.setPositiveButton("设置网络",this);
                break;
            case CHCEKING:
                builder.setNegativeButton("确认",this);
                break;
            default:
                break;
        }
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {

            case DialogInterface.BUTTON_POSITIVE:
                if (resultCode == NONET){
                    //无网络连接
                    NetUtil.openSetting(getActivity());
                }else{
                    //下载文件
//                    RxBus.getDefault().post(new UpdateEvent());
                }
//                if (ResultDialogFragment.this != null) {
//                    dismiss();
//                }
                break;

            case DialogInterface.BUTTON_NEGATIVE:
//                if (ResultDialogFragment.this != null) {
//                    dismiss();
//                }
                break;
            default:
                break;
        }
    }
}
