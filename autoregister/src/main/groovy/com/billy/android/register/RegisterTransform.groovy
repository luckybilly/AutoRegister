package com.billy.android.register

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project


/**
 *
 * @author billy.qi
 * @since 17/3/21 11:48
 */
class RegisterTransform extends Transform {

    Project project
    AutoRegisterConfig config

    RegisterTransform(Project project) {
        this.project = project
    }


    @Override
    String getName() {
        return "auto-register"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs
                   , Collection<TransformInput> referencedInputs
                   , TransformOutputProvider outputProvider
                   , boolean isIncremental) throws IOException, TransformException, InterruptedException {
        project.logger.warn("start auto-register transform...")
        project.logger.warn(config.toString())

        long time = System.currentTimeMillis()
        boolean leftSlash = File.separator == '/'

        def closeJarCache = config.closeJarCache

        Map<String, String> jarMap
        File jarManagerfile
        Gson gson
        project.logger.warn "-------------------config.closeJarCache--------------------" + closeJarCache

        File interfaceFile
        Map<String, JarConfigInfo> interfaceMap
        if (!closeJarCache) { //不关闭 JarCache 扫描标识

            gson = new Gson()

            jarManagerfile = AutoRegisterHelper.getJarInterfaceConfigFile(project)
            if (!jarManagerfile.exists()) {
                jarManagerfile.createNewFile()
                jarMap = new HashMap<>()
            } else {
                def text = jarManagerfile.text
                if (text == "" || text == null) {
                    jarMap = new HashMap<>()
                } else {
                    jarMap = gson.fromJson(text, new TypeToken<HashMap<String, String>>() {
                    }.getType())
                }

            }

            interfaceFile = AutoRegisterHelper.getsaveInterfaceConfigFile(project)

            interfaceMap = AutoRegisterHelper.getsaveInterfaceConfigMap(interfaceFile)

        }


        CodeScanProcessor scanProcessor = new CodeScanProcessor(config.list, jarMap, interfaceMap)
        // 遍历输入文件
        inputs.each { TransformInput input ->
            // 遍历jar
            input.jarInputs.each { JarInput jarInput ->

                scanJar(jarInput, outputProvider, scanProcessor, jarMap)


            }

            // 遍历目录
            input.directoryInputs.each { DirectoryInput directoryInput ->
                // 获得产物的目录
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                String root = directoryInput.file.absolutePath
                if (!root.endsWith(File.separator))
                    root += File.separator
                //遍历目录下的每个文件
                directoryInput.file.eachFileRecurse { File file ->
                    def path = file.absolutePath.replace(root, '')
                    if (file.isFile()) {
                        def entryName = path
                        if (!leftSlash) {
                            entryName = entryName.replaceAll("\\\\", "/")
                        }
                        scanProcessor.checkInitClass(entryName, new File(dest.absolutePath + File.separator + path))
                        if (scanProcessor.shouldProcessClass(entryName)) {
                            scanProcessor.scanClass(file)
                        }
                    }
                }
                project.logger.info "Copying\t${directoryInput.file.absolutePath} \nto\t\t${dest.absolutePath}"
                // 处理完后拷到目标文件
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }




        if (jarMap != null) {

            if (jarManagerfile != null && jarMap.size() > 0 && gson != null) {
                def json = gson.toJson(jarMap)
                jarManagerfile.write(json)
            }

        }

        if(interfaceMap!=null){
            if(interfaceFile!=null&&interfaceMap.size() > 0&& gson != null){
                def json = gson.toJson(interfaceMap)
                interfaceFile.write(json)
            }

        }


        def scanFinishTime = System.currentTimeMillis()
        project.logger.error("register scan all class cost time: " + (scanFinishTime - time) + " ms")

        config.list.each { ext ->
            if (ext.fileContainsInitClass) {
                println("insert register code to file:" + ext.fileContainsInitClass.absolutePath)
                if (ext.classList.isEmpty()) {
                    project.logger.error("No class implements found for interface:" + ext.interfaceName)
                } else {
                    ext.classList.each {
                        println(it)
                    }
                    println('')
                    CodeInsertProcessor.insertInitCodeTo(ext)//遍历 config list 注入代码
                }
            } else {
                project.logger.error("The specified register class not found:" + ext.registerClassName)
            }
        }
        def finishTime = System.currentTimeMillis()
        project.logger.error("register insert code cost time: " + (finishTime - scanFinishTime) + " ms")
        project.logger.error("register cost time --: " + (finishTime - time) + " ms")
    }


    void scanJar(JarInput jarInput, TransformOutputProvider outputProvider,
                 CodeScanProcessor scanProcessor, Map<String, String> jarMap) {

/*        def destName = jarInput.name
        // 重名名输出文件,因为可能同名,会覆盖
        def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath)
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4)
        }
        // 获得输出文件
        File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)*/

        // 获得输入文件
        File src = jarInput.file
        File dest
        //遍历jar的字节码类文件，找到需要自动注册的component
        if (scanProcessor.shouldProcessPreDexJar(src.absolutePath)) {

            if (jarMap == null) {
                dest = getDestFile(jarInput, outputProvider)
                scanProcessor.scanJar(src, dest, "")
            } else {

                def fileMd5 = AutoRegisterHelper.getFileKey(src)
                if (!jarMap.containsKey(fileMd5)) {
                    dest = getDestFile(jarInput, outputProvider)
                    scanProcessor.scanJar(src, dest, fileMd5)
                }

            }


        }
        if (dest != null) {
            project.logger.info "Copying\t${src.absolutePath} \nto\t\t${dest.absolutePath}"
            FileUtils.copyFile(src, dest)

        }


    }

   static File getDestFile(JarInput jarInput, TransformOutputProvider outputProvider) {
        def destName = jarInput.name
        // 重名名输出文件,因为可能同名,会覆盖
        def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath)
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4)
        }
        // 获得输出文件
        File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)

        return dest
    }
}

