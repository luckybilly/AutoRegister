package com.billy.app_lib_interface;

import java.util.HashMap;
import java.util.Set;

/**
 * @author billy.qi
 * @since 17/9/20 16:56
 */
public class CategoryManager {
    private static HashMap<String, ICategory> CATEGORIES = new HashMap<>();

    static void register(ICategory category) {
        if (category != null) {
            CATEGORIES.put(category.getName(), category);
        }
    }

    public static Set<String> getCategoryNames() {
        return CATEGORIES.keySet();
    }
}
