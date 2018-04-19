package com.billy.android.register

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES

/**
 *
 * @author zhangkb
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

    }


    final static def autoregisterDir = "auto-register"
    /**
     * 保存 RegisterInfo
     * @param project
     * @return
     */
    static File getRegisterInfoFile(Project project) {

        String baseDir = project.getBuildDir().absolutePath + File.separator + FD_INTERMEDIATES + File.separator + autoregisterDir + File.separator

        if (mkdirs(baseDir)) {
            return new File(baseDir + "RegisterInfo.config")
        } else {

            throw new FileNotFoundException("Not found  path:" + baseDir)
        }
    }

    /**
     * 保存不需要扫描的jar ,没有扫描到接口和需要注入的。
     * @param project
     * @return
     */
    static File getJarInterfaceConfigFile(Project project) {

        String baseDir = project.getBuildDir().absolutePath + File.separator + FD_INTERMEDIATES + File.separator + autoregisterDir + File.separator

        if (mkdirs(baseDir)) {
            return new File(baseDir + "jarInterfaceConfig.json")
        } else {

            throw new FileNotFoundException("Not found  path:" + baseDir)
        }


    }

    static File getsaveInterfaceConfigFile(Project project) {

        String baseDir = project.getBuildDir().absolutePath + File.separator + FD_INTERMEDIATES + File.separator + autoregisterDir + File.separator

        if (mkdirs(baseDir)) {
            return new File(baseDir + "saveInterfaceConfig.json")

        } else {

            throw new FileNotFoundException("Not found  path:" + baseDir)
        }
    }

    /**
     * 保存扫扫描到接口的jar
     * @param project
     * @return
     */
    static Map<String, JarConfigInfo> getsaveInterfaceConfigMap(File file) {

        Map<String, JarConfigInfo> interfaceMap

        if (!file.exists()) {

            file.createNewFile()
            interfaceMap = new HashMap<String, JarConfigInfo>()
        } else {

            def text = file.text
            if (text == "" || text == null) {
                interfaceMap = new HashMap<String, JarConfigInfo>()
            } else {
                interfaceMap = new Gson().fromJson(text, new TypeToken<HashMap<String, JarConfigInfo>>() {
                }.getType())
            }

        }

        return interfaceMap
    }





static boolean mkdirs(String dirPath) {
    def baseDirFile = new File(dirPath)
    def isSuccess = true
    if (!baseDirFile.isDirectory()) {
        isSuccess = baseDirFile.mkdirs()
    }

    return isSuccess
}

}