package com.billy.app_lib;

import com.billy.app_lib_interface.ICategory;

/**
 * @author billy.qi
 * @since 17/9/21 19:06
 */
public class CategoryA implements ICategory {
    @Override
    public String getName() {
        return "CategoryA";
    }
}
