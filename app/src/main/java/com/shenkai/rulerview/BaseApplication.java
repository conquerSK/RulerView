package com.shenkai.rulerview;

import android.app.Application;
import android.content.Context;

/**
 * Created by ShenKai on 2020/6/6 23:02
 * Desc:
 */
public class BaseApplication extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext() {
        return context;
    }
}
