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

    CodeScanProcessor(ArrayList<RegisterInfo> infoList, HashMap<String, String> jarMap) {
        this.infoList = infoList
        this.jarMap = jarMap

    }

    /**
     * 扫描jar包
     * @param jarFile 来源jar包文件
     * @param destFile transform后的目标jar包文件
     */
    void scanJar(File jarFile, File destFile,String fileMd5) {
        if (jarFile) {
            def file = new JarFile(jarFile)
            Enumeration enumeration = file.entries()

            boolean isFind = false
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                //support包不扫描
                if (entryName.startsWith("android/support"))
                    break
                //   println('----------entryName:' + entryName)

                if (checkInitClass(entryName, destFile)) {
                    isFind = true
                }

                //是否要过滤这个类，这个可配置
                if (shouldProcessClass(entryName)) {
                    InputStream inputStream = file.getInputStream(jarEntry)

                    scanClass(inputStream, jarFile.absolutePath,fileMd5)

                    inputStream.close()
                }

            }
            file.close()

            if (!isFind || jarMap == null||jarMap.size() < 0) return

           // String md5 = DigestUtils.md5Hex(jarFile.newInputStream())
            jarMap.remove(fileMd5)
        }
    }
    /**
     * 检查此entryName是不是被注入注册代码的类，如果是则记录此文件（class文件或jar文件）用于后续的注册代码注入
     * @param entryName
     * @param file
     */
    boolean checkInitClass(String entryName, File file) {
        if (entryName == null || !entryName.endsWith(".class"))
            return


        entryName = entryName.substring(0, entryName.lastIndexOf('.'))

        def isFind = false

        infoList.each { ext ->
            // println("file name----"+file.name+"----entryName----"+entryName+"----ext.initClassName---"+ext.initClassName)
            if (ext.initClassName == entryName) { //（检查是不是codeInsertToClassName 配置 要插入class文件或jar文件）
                ext.fileContainsInitClass = file//用于后面注入用   这里也应该记录一下。

                if (file.name.endsWith(".jar")) {
                    isFind = true
                }

            }
        }

        if (!isFind && file.name.endsWith(".jar")) {

            //jarMap.put()
            //     println("不codeInsertToClassName---" + file.absolutePath + "--entryName--" + entryName)
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

    void scanClass(File file) {
        scanClass(file,"")
    }
    void scanClass(File file,String fileMd5) {
        scanClass(new FileInputStream(file), file.absolutePath,fileMd5)
    }

    //refer hack class when object init
    void scanClass(InputStream inputStream, def filePath, String fileMd5) {

       // println("-------------------scanClass------------------------")
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)

        ScanClassVisitor cv = new ScanClassVisitor(Opcodes.ASM5, cw, inputStream, filePath,fileMd5)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        inputStream.close()


    }

    class ScanClassVisitor extends ClassVisitor {

        InputStream inputStream
        String filePath
        String fileMd5
        ScanClassVisitor(int api, ClassVisitor cv, InputStream inputStream, String filePath, String fileMd5) {
            super(api, cv)
            this.inputStream = inputStream
            this.filePath = filePath
            this.fileMd5 = fileMd5
        }

        boolean is(int access, int flag) {
            return (access & flag) == flag
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
           // println("-------------------visit------------------------")
            boolean isFind = false
            infoList.each { ext ->
                if (shouldProcessThisClassForRegister(ext, name)) {
                    //判断没有父类的
                    if (superName != 'java/lang/Object' && !ext.superClassNames.isEmpty()) {
                        for (int i = 0; i < ext.superClassNames.size(); i++) {
                            if (ext.superClassNames.get(i) == superName) {
                                //       println("superClassNames--------"+name)
                                ext.classList.add(name) //需要把对象注入到管理类 就是fileContainsInitClass
                                isFind = true
                                return
                            }
                        }
                    }
                    if (ext.interfaceName && interfaces != null) {
                        interfaces.each { itName ->
                            if (itName == ext.interfaceName) {
                                //      println("interfaceName--------"+name)
                                ext.classList.add(name)//需要把对象注入到管理类  就是fileContainsInitClass
                                isFind = true
                            }
                        }
                    }
                }
            }

         //   println("jar map != null--"+(jarMap != null)+"--"+"!isFind--"+(!isFind)+"---"+(filePath.endsWith(".jar")))

            if (!isFind && filePath.endsWith(".jar") && jarMap != null) {
                def md5 = fileMd5
             //   def md5= AutoRegisterHelper.getFileKey(filePath)
              //  println("jar map size--" + jarMap.size())
                if(!jarMap.containsKey(md5)){
                    jarMap.put(md5, filePath)
                 //   println("没有找到需要把对象注入到管理的类， path--" + filePath + "___" + jarMap.size())
                }



            } /*else if (filePath.endsWith(".jar")) {

                println("注入到管理的类， path--" + filePath)
            }*/

        }
    }

}