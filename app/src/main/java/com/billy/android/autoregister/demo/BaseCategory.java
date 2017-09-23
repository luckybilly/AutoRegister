package com.billy.android.autoregister.demo;

import android.util.Log;

import com.billy.app_lib_interface.ICategory;

/**
 * @author billy.qi
 * @since 17/9/22 13:31
 */
public abstract class BaseCategory implements ICategory {

    public void doSth() {
        Log.i("baseCategory", "do something else");
    }
}
