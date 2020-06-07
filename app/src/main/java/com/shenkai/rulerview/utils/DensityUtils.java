package com.shenkai.rulerview.utils;

import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.shenkai.rulerview.BaseApplication;

/**
 * Created by ShenKai on 2020/6/6 23:03
 * Desc:
 */
public class DensityUtils {
    public static int dipToPx(float dip) {
        final DisplayMetrics displayMetrics = BaseApplication.getContext().getResources().getDisplayMetrics();
        return (int) (displayMetrics.density * dip + 0.5f);
    }
}
