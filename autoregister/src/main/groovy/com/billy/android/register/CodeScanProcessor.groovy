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
    void scanJar(File jarFile, File destFile) {
        if (!jarFile) return

        //检查是否存在缓存，有就添加class list 和 设置fileContainsInitClass
        if (checkClassListCache(jarFile, destFile)) return

        def fileKey = AutoRegisterHelper.getFileKey(jarFile)
        boolean inFindManagerClass = false
        boolean isFindInterface = false
        def file = new JarFile(jarFile)
        Enumeration enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            String entryName = jarEntry.getName()
            //support包不扫描
            if (entryName.startsWith("android/support"))
                break

            if (checkInitClass(entryName, destFile, fileKey)) {
                inFindManagerClass = true//扫描到ManagerClass
            }
            //是否要过滤这个类，这个可配置
            if (shouldProcessClass(entryName)) {
                InputStream inputStream = file.getInputStream(jarEntry)

                if (scanClass(inputStream, jarFile.absolutePath)) {
                    isFindInterface = true //扫描到接口
                }
                inputStream.close()
            }
        }
        if (null != file) {
            file.close()
        }
        if (!inFindManagerClass && !isFindInterface && jarMap != null && jarFile.absolutePath.endsWith(".jar")) {

            if (!jarMap.containsKey(jarFile.absolutePath)) {
                jarMap.put(jarFile.absolutePath, jarFile.absolutePath)
                //  println("没有找到需要把对象注入到管理的类， path--" + jarFile.absolutePath + "___" + jarMap.size())
            }
        }
    }
    /**
     * 检查此entryName是不是被注入注册代码的类，如果是则记录此文件（class文件或jar文件）用于后续的注册代码注入
     * @param entryName
     * @param file
     */
    boolean checkInitClass(String entryName, File file) {
        checkInitClass(entryName, file, "")
    }

    boolean checkInitClass(String entryName, File file, String srcFilePath) {
        if (entryName == null || !entryName.endsWith(".class"))
            return
        entryName = entryName.substring(0, entryName.lastIndexOf('.'))
        def isFind = false
        infoList.each { ext ->
            // println("------entryName:----"+entryName)
            if (ext.initClassName == entryName) {
                ext.fileContainsInitClass = file
                if (file.name.endsWith(".jar")) {
                    println(srcFilePath + "--find manager class:" + entryName)
                    addManagerMap(srcFilePath, entryName)
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
        return scanClass(file.newInputStream(), file.absolutePath)
    }

    //refer hack class when object init
    boolean scanClass(InputStream inputStream, String filePath) {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)
        ScanClassVisitor cv = new ScanClassVisitor(Opcodes.ASM5, cw, filePath)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        inputStream.close()

        return cv.isFind
    }

    class ScanClassVisitor extends ClassVisitor {
        private String filePath
        private def isFind = false

        ScanClassVisitor(int api, ClassVisitor cv, String filePath) {
            super(api, cv)
            this.filePath = filePath
        }

        boolean is(int access, int flag) {
            return (access & flag) == flag
        }

        boolean getIsFind() {
            return isFind
        }

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
                    if (superName != 'java/lang/Object' && !ext.superClassNames.isEmpty()) {
                        for (int i = 0; i < ext.superClassNames.size(); i++) {
                            if (ext.superClassNames.get(i) == superName) {
                                //  println("superClassNames--------"+name)
                                ext.classList.add(name) //需要把对象注入到管理类 就是fileContainsInitClass
                                isFind = true
                                addInterfaceMap(superName, name, filePath)
                                return
                            }
                        }
                    }
                    if (ext.interfaceName && interfaces != null) {
                        interfaces.each { itName ->
                            if (itName == ext.interfaceName) {
                                ext.classList.add(name)//需要把对象注入到管理类  就是fileContainsInitClass
                                println("find interface:" + name)
                                addInterfaceMap(itName, name, filePath)
                                isFind = true
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * 添加扫描到的ManagerClass到map
     * @param fileAbsolutePath
     * @param entryName
     */
    private void addManagerMap(String fileAbsolutePath, String entryName) {
        if (interfaceMap == null) return
        if (interfaceMap.containsKey(fileAbsolutePath)) {
            def jarInfo = interfaceMap.get(fileAbsolutePath)
            JarConfigInfo.ClassList classInfo = new JarConfigInfo.ClassList()
            classInfo.setJarFilePath(fileAbsolutePath)
            classInfo.setIsManagerClass(true)
            classInfo.setInterfaceName(entryName)
            classInfo.setClassName(entryName)

            jarInfo.classLists.add(classInfo)

        } else {
            def jarInfo = new JarConfigInfo()
            JarConfigInfo.ClassList classInfo = new JarConfigInfo.ClassList()

            classInfo.setJarFilePath(fileAbsolutePath)
            classInfo.setIsManagerClass(true)
            classInfo.setInterfaceName(entryName)
            classInfo.setClassName(entryName)

            List<JarConfigInfo.ClassList> list = new ArrayList<>()
            list.add(classInfo)
            jarInfo.setClassLists(list)

            interfaceMap.put(fileAbsolutePath, jarInfo)
        }
    }
    /**
     * 添加扫描到的接口类到map
     * @param interfaceName
     * @param name
     * @param filePath
     */
    private void addInterfaceMap(String interfaceName, String name, String filePath) {

        if (!filePath.endsWith(".jar") || interfaceMap == null) return
        def jarConfigInfo
        if (interfaceMap.containsKey(filePath)) {
            jarConfigInfo = interfaceMap.get(filePath)

            JarConfigInfo.ClassList classInfo = new JarConfigInfo.ClassList()
            classInfo.setJarFilePath(filePath)
            classInfo.setIsManagerClass(false)
            classInfo.setInterfaceName(interfaceName)
            classInfo.setClassName(name)

            jarConfigInfo.classLists.add(classInfo)

        } else {

            jarConfigInfo = new JarConfigInfo()

            JarConfigInfo.ClassList classInfo = new JarConfigInfo.ClassList()
            classInfo.setJarFilePath(filePath)
            classInfo.setIsManagerClass(false)
            classInfo.setInterfaceName(interfaceName)
            classInfo.setClassName(name)

            List<JarConfigInfo.ClassList> list = new ArrayList<>()
            list.add(classInfo)
            jarConfigInfo.setClassLists(list)

            interfaceMap.put(filePath, jarConfigInfo)
        }
    }

    /**
     * 检查是否存在缓存，有就添加class list 和 设置fileContainsInitClass
     * @param jarFile
     * @param destFile
     * @return 是否存在缓存
     */
    boolean checkClassListCache(File jarFile, File destFile) {
        def fileKey = AutoRegisterHelper.getFileKey(jarFile)
        if (interfaceMap != null) {
            if (interfaceMap.containsKey(fileKey)) {
                JarConfigInfo jarConfigInfo = interfaceMap.get(fileKey)
                infoList.each { ext ->
                    jarConfigInfo.classLists.each { list ->
                        //       println("----list-------"+list.className)
                        if (list.isManagerClass) {
                            if (ext.initClassName == list.className) {
                                ext.fileContainsInitClass = destFile
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
                return true
            }
        }
        return false
    }
}