package com.binfun.update.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

/**
 * 网络相关辅助类
 */
public class NetUtil {

	private NetUtil() {
        /* cannot be instantiated */
		throw new UnsupportedOperationException("cannot be instantiated");
	}

	/**
	 * 判断网络是否连接
	 *
	 * @param context
	 * @return
	 */
	public static boolean isConnected(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (null != connectivityManager) {
			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
//			if (networkInfo != null && networkInfo.isConnected()) {
//				if (networkInfo.getState() == NetworkInfo.State.CONNECTING) {
//					return true;
//				}
//			}
			if (networkInfo!=null){
				return networkInfo.isAvailable();
			}
		}

		return false;
	}

	/**
	 * 判断是否是WIFI连接
	 *
	 * @param context
	 * @return
	 */
	public static boolean isWIFI(Context context) {

		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager == null)
			return false;
		return connectivityManager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;
	}

	/**
	 * 打开网络设置界面
	 */
	public static void openSetting(Activity activity) {
//		Intent intent = new Intent("/");
//		ComponentName cm = new ComponentName("com.android.settings",
//				"com.android.settings.WirelessSettings");
//		intent.setComponent(cm);
//		intent.setAction("android.intent.action.VIEW");
//		activity.startActivityForResult(intent, 0);
		Intent intent=null;
		//判断手机系统的版本  即API大于10 就是3.0或以上版本
		if(android.os.Build.VERSION.SDK_INT > 10){
			intent = new Intent(Settings.ACTION_SETTINGS);
		}else{
			intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
//			intent = new Intent();
//			ComponentName component = new ComponentName("com.android.settings","com.android.settings.WirelessSettings");
//			intent.setComponent(component);
//			intent.setAction("android.intent.action.VIEW");
		}
		activity.startActivity(intent);
//		try{
//			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//		}catch(ActivityNotFoundException e){
////			ToastUtil.createXToast(getContext(), "请进入系统设置，打开网络！").show();
//		}
	}

}