package com.billy.app_lib;

import java.util.ArrayList;
import java.util.List;

/**
 * @author billy.qi
 * @since 17/9/21 19:08
 */
public class OtherManager {

    static final List<IOther> LIST = new ArrayList<>();

    private static void registerOther(IOther other) {
        if (other != null) {
            LIST.add(other);
        }
    }

    public static List<IOther> getAll() {
        return LIST;
    }
}
