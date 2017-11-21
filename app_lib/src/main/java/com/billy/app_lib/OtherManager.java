package com.billy.app_lib;

import java.util.ArrayList;
import java.util.List;

/**
 * @author billy.qi
 * @since 17/9/21 19:08
 */
public class OtherManager {

    private final List<IOther> LIST = new ArrayList<>();

    public OtherManager() {
        init();
    }

    private void init () {

    }

    private void registerOther(IOther other) {
        if (other != null) {
            LIST.add(other);
        }
    }

    public List<IOther> getAll() {
        return LIST;
    }
}
