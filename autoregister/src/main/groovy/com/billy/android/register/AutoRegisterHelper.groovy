package com.billy.android.register

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.Project

import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES

/**
 * 文件操作辅助类
 * @author zhangkb
 * @since 2018/04/13
 */
class AutoRegisterHelper {

    /**
     *  获取文件 absolutePath
     * @param file 文件
     * @return absolutePath
     */
    static String getFileKey(File file) {

        if (file != null) {
            return file.absolutePath //以前是以文件md5为key
        }
        return null
    }


    final static def autoregisterDir = "auto-register"
    /**
     * 保存 RegisterInfo
     * @param project
     * @return file
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
     * @return File
     */
    static File getJarInterfaceConfigFile(Project project) {

        String baseDir = project.getBuildDir().absolutePath + File.separator + FD_INTERMEDIATES + File.separator + autoregisterDir + File.separator

        if (mkdirs(baseDir)) {
            return new File(baseDir + "jarInterfaceConfig.json")
        } else {
            throw new FileNotFoundException("Not found  path:" + baseDir)
        }


    }

    /**
     * 保存扫描到的jar
     * @param project
     * @return File
     */
    static File getsaveInterfaceConfigFile(Project project) {

        String baseDir = project.getBuildDir().absolutePath + File.separator + FD_INTERMEDIATES + File.separator + autoregisterDir + File.separator

        if (mkdirs(baseDir)) {
            return new File(baseDir + "saveInterfaceConfig.json")

        } else {
            throw new FileNotFoundException("Not found  path:" + baseDir)
        }
    }

    /**
     * 创建 Map
     * @param file
     * @return map
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

    /**
     * 创建文件夹
     * @param dirPath
     * @return boolean
     */
    static boolean mkdirs(String dirPath) {
        def baseDirFile = new File(dirPath)
        def isSuccess = true
        if (!baseDirFile.isDirectory()) {
            isSuccess = baseDirFile.mkdirs()
        }
        return isSuccess
    }

}