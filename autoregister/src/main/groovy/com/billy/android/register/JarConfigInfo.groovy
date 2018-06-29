package com.billy.android.register
/**
 * 已扫描到接口或者codeInsertToClassName jar的信息
 * @author zhangkb
 * @since 2018/04/17
 */
class JarConfigInfo {
    List<ClassList> classLists
    class ClassList{
        String className
        String jarFilePath
        String interfaceName
        boolean  isManagerClass
    }
}