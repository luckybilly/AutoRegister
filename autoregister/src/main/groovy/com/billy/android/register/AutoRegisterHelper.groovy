package com.billy.android.register

import org.apache.commons.codec.digest.DigestUtils

/**
 * 文件 key
 * @author zkb
 * @since 2018/04/13
 */
class AutoRegisterHelper {

    static String getFileKey(String filePath) {
        return getFileKey(new File(filePath))
    }

    static String getFileKey(File file) {

        //速度主要慢在这里。
        if (file != null)
            return DigestUtils.md5Hex(file.newInputStream())


  //     return file.hashCode()+file.absolutePath

    }
}