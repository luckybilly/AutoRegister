package com.billy.android.register


import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Pattern


/**
 *
 * @author billy.qi
 * @since 17/3/20 11:48
 */
class CodeScanProcessor {

    ArrayList<RegisterInfo> infoList

    HashMap<String, String> jarMap
    Map<String, JarConfigInfo> interfaceMap

    CodeScanProcessor(ArrayList<RegisterInfo> infoList, HashMap<String, String> jarMap,
                      Map<String, JarConfigInfo> interfaceMap) {
        this.infoList = infoList
        this.jarMap = jarMap
        this.interfaceMap = interfaceMap

    }

    /**
     * 扫描jar包
     * @param jarFile 来源jar包文件
     * @param destFile transform后的目标jar包文件
     */
    void scanJar(File jarFile, File destFile, String fileMd5) {
        if (!jarFile) return

        if(interfaceMap!=null){
            if (interfaceMap.containsKey(fileMd5)) {

                JarConfigInfo jarConfigInfo = interfaceMap.get(fileMd5)

                infoList.each { ext ->

                    jarConfigInfo.classLists.each { list ->

                        if (list.isManagerClass) {
                            if (ext.initClassName == list.className) {

                                ext.fileContainsInitClass = new File(list.jarFilePath)
                            }

                        } else if (ext.interfaceName == list.interfaceName) {

                            ext.classList.add(list.className)

                        }

                        for (int i = 0; i < ext.superClassNames.size(); i++) {

                            if (ext.superClassNames.get(i) == list.interfaceName) {

                                ext.classList.add(list.className)
                            }

                        }

                    }


                }


                return
            }
        }


        def file = new JarFile(jarFile)
        Enumeration enumeration = file.entries()

        boolean inFindManagerClass = false
        boolean isFindInterface = false


        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            String entryName = jarEntry.getName()
            //support包不扫描
            if (entryName.startsWith("android/support"))
                break

            if (checkInitClass(entryName, destFile, fileMd5)) {
                inFindManagerClass = true//扫描到ManagerClass

            }

            //是否要过滤这个类，这个可配置
            if (shouldProcessClass(entryName)) {
                InputStream inputStream = file.getInputStream(jarEntry)

                if (scanClass(inputStream, fileMd5, jarFile.absolutePath)) {

                    isFindInterface = true //扫描到接口
                }

                inputStream.close()
            }

        }
        file.close()


        if (!inFindManagerClass && !isFindInterface && jarMap != null && jarFile.absolutePath.endsWith(".jar")) {

            if (!jarMap.containsKey(fileMd5)) {
                jarMap.put(fileMd5, jarFile.absolutePath)
                //println("没有找到需要把对象注入到管理的类， path--" + filePath + "___" + jarMap.size())
            }

        }

    }


    boolean checkInitClass(String entryName, File file) {
        return checkInitClass(entryName, file, "")
    }
    /**
     * 检查此entryName是不是被注入注册代码的类，如果是则记录此文件（class文件或jar文件）用于后续的注册代码注入
     * @param entryName
     * @param file
     */
    boolean checkInitClass(String entryName, File file, String fileMd5) {
        if (entryName == null || !entryName.endsWith(".class"))
            return


        entryName = entryName.substring(0, entryName.lastIndexOf('.'))

        def isFind = false

        infoList.each { ext ->
            if (ext.initClassName == entryName) {
                //（检查是不是 codeInsertToClassName 配置 要插入class文件或jar文件）
                ext.fileContainsInitClass = file//用于后面注入用   这里也应该记录一下。

                if (file.name.endsWith(".jar")) {
                    addManagerMap(fileMd5,file.absolutePath,entryName)
                    isFind = true
                }

            }
        }


        return isFind

    }

    static boolean shouldProcessPreDexJar(String path) {
        return !path.contains("com.android.support") && !path.contains("/android/m2repository")
    }

    // file in folder like these
    //com/billy/testplugin/Aop.class
    //com/billy/testplugin/BuildConfig.class
    //com/billy/testplugin/R$attr.class
    //com/billy/testplugin/R.class
    // entry in jar like these
    //android/support/v4/BuildConfig.class
    //com/lib/xiwei/common/util/UiTools.class
    boolean shouldProcessClass(String entryName) {
//        println('classes:' + entryName)
        if (entryName == null || !entryName.endsWith(".class"))
            return false
        entryName = entryName.substring(0, entryName.lastIndexOf('.'))
        def length = infoList.size()
        for (int i = 0; i < length; i++) {
            if (shouldProcessThisClassForRegister(infoList.get(i), entryName))
                return true
        }
        return false
    }

    /**
     * 过滤器进行过滤
     * @param info
     * @param entryName
     * @return
     */
    private static boolean shouldProcessThisClassForRegister(RegisterInfo info, String entryName) {
        if (info != null) {
            def list = info.includePatterns
            if (list) {
                def exlist = info.excludePatterns
                Pattern pattern, p
                for (int i = 0; i < list.size(); i++) {
                    pattern = list.get(i)
                    if (pattern.matcher(entryName).matches()) {
                        if (exlist) {
                            for (int j = 0; j < exlist.size(); j++) {
                                p = exlist.get(j)
                                if (p.matcher(entryName).matches())
                                    return false
                            }
                        }
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 处理class的注入
     * @param file class文件
     * @return 修改后的字节码文件内容
     */

    boolean scanClass(File file) {
        return scanClass(file.newInputStream(), "", file.absolutePath)
    }

    //refer hack class when object init
    boolean scanClass(InputStream inputStream, String fileMd5, String filePath) {

        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)

        ScanClassVisitor cv = new ScanClassVisitor(Opcodes.ASM5, cw, fileMd5, filePath)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        inputStream.close()

        return cv.isFind


    }

    class ScanClassVisitor extends ClassVisitor {

        String fileMd5
        String filePath

        ScanClassVisitor(int api, ClassVisitor cv, String fileMd5, String filePath) {
            super(api, cv)

            this.fileMd5 = fileMd5
            this.filePath = filePath

        }

        boolean is(int access, int flag) {
            return (access & flag) == flag
        }

        boolean getIsFind() {
            return isFind
        }
        def isFind = false

        void visit(int version, int access, String name, String signature,
                   String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)
            //抽象类、接口、非public等类无法调用其无参构造方法
            if (is(access, Opcodes.ACC_ABSTRACT)
                    || is(access, Opcodes.ACC_INTERFACE)
                    || !is(access, Opcodes.ACC_PUBLIC)
            ) {
                return
            }


            infoList.each { ext ->
                if (shouldProcessThisClassForRegister(ext, name)) {
                    //判断没有父类的
                    if (superName != 'java/lang/Object' && !ext.superClassNames.isEmpty()) {
                        for (int i = 0; i < ext.superClassNames.size(); i++) {
                            if (ext.superClassNames.get(i) == superName) {
                                //    println("superClassNames--------"+name)
                                ext.classList.add(name) //需要把对象注入到管理类 就是fileContainsInitClass
                                isFind = true

                                addInterfaceMap(fileMd5, superName, name, filePath)
                                return
                            }
                        }
                    }
                    if (ext.interfaceName && interfaces != null) {
                        interfaces.each { itName ->
                            if (itName == ext.interfaceName) {

                                ext.classList.add(name)//需要把对象注入到管理类  就是fileContainsInitClass

                                addInterfaceMap(fileMd5, itName, name, filePath)

                                isFind = true
                            }
                        }
                    }
                }
            }


        }
    }


    private void addManagerMap(String fileMd5,String fileAbsolutePath,String entryName){


        if (interfaceMap==null) return

        if (interfaceMap.containsKey(fileMd5)) {
            def jarInfo = interfaceMap.get(fileMd5)

            JarConfigInfo.ClassList classInfo = new JarConfigInfo.ClassList()

            classInfo.setJarMd5(fileMd5)
            classInfo.setJarFilePath(fileAbsolutePath)
            classInfo.setIsManagerClass(true)
            classInfo.setInterfaceName(entryName)
            classInfo.setClassName(entryName)

            jarInfo.classLists.add(classInfo)


        } else {

            def jarInfo = new JarConfigInfo()

            JarConfigInfo.ClassList classInfo = new JarConfigInfo.ClassList()

            classInfo.setJarMd5(fileMd5)
            classInfo.setJarFilePath(fileAbsolutePath)
            classInfo.setIsManagerClass(true)
            classInfo.setInterfaceName(entryName)
            classInfo.setClassName(entryName)

            List<JarConfigInfo.ClassList> list = new ArrayList<>()

            list.add(classInfo)

            jarInfo.setClassLists(list)

            interfaceMap.put(fileMd5, jarInfo)
        }
    }


    private void addInterfaceMap(String fileMd5, String interfaceName, String name, String filePath) {

        if (!filePath.endsWith(".jar")||interfaceMap==null) return
        def jarConfigInfo
        if (interfaceMap.containsKey(fileMd5)) {
            jarConfigInfo = interfaceMap.get(fileMd5)


            JarConfigInfo.ClassList classInfo = new JarConfigInfo.ClassList()

            classInfo.setJarMd5(fileMd5)
            classInfo.setJarFilePath(filePath)
            classInfo.setIsManagerClass(false)
            classInfo.setInterfaceName(interfaceName)
            classInfo.setClassName(name)

            jarConfigInfo.classLists.add(classInfo)

        } else {

            jarConfigInfo = new JarConfigInfo()

            JarConfigInfo.ClassList classInfo = new JarConfigInfo.ClassList()

            classInfo.setJarMd5(fileMd5)
            classInfo.setJarFilePath(filePath)
            classInfo.setIsManagerClass(false)
            classInfo.setInterfaceName(interfaceName)
            classInfo.setClassName(name)


            List<JarConfigInfo.ClassList> list = new ArrayList<>()
            list.add(classInfo)

            jarConfigInfo.setClassLists(list)

            interfaceMap.put(fileMd5, jarConfigInfo)
        }


    }

}