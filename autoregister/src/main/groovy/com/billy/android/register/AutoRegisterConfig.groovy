package com.billy.android.register

import org.gradle.api.Project

/**
 * aop的配置信息
 * @author billy.qi
 * @since 17/3/28 11:48
 */
class AutoRegisterConfig {

    public ArrayList<Map<String, Object>> registerInfo = []

    ArrayList<RegisterInfo> list = new ArrayList<>()

    Project project
    def closeJarCache = false

    AutoRegisterConfig() {}

    void convertConfig() {
        registerInfo.each { map ->
            RegisterInfo info = new RegisterInfo()
            info.interfaceName = map.get('scanInterface')
            def superClasses = map.get('scanSuperClasses')
            if (!superClasses) {
                superClasses = new ArrayList<String>()
            } else if (superClasses instanceof String) {
                ArrayList<String> superList = new ArrayList<>()
                superList.add(superClasses)
                superClasses = superList
            }
            info.superClassNames = superClasses
            info.initClassName = map.get('codeInsertToClassName') //代码注入的类
            info.initMethodName = map.get('codeInsertToMethodName') //代码注入的方法（默认为static块）
            info.registerMethodName = map.get('registerMethodName') //生成的代码所调用的方法
            info.registerClassName = map.get('registerClassName') //注册方法所在的类
            info.include = map.get('include')
            info.exclude = map.get('exclude')
            info.init()
            if (info.validate())
                list.add(info)
            else {
                project.logger.error('auto register config error: scanInterface, codeInsertToClassName and registerMethodName should not be null\n' + info.toString())
            }

        }

        if (!closeJarCache) {
            checkRegisterInfo()
        }
    }

    private void checkRegisterInfo() {

        def registerInfo = AutoRegisterHelper.getRegisterInfoFile(project)

        def listInfo = list.toString()

        if (!registerInfo.exists()) {
            registerInfo.createNewFile()
            if (registerInfo.canRead() && registerInfo.canWrite()) {
                registerInfo.write(listInfo)
            } else {
                project.logger.error('------wirte registerInfo error--------')
            }

        } else {
            def info = registerInfo.text
            if (info != listInfo) {

                def jarInterfaceConfigFile = AutoRegisterHelper.getJarInterfaceConfigFile(project)
                def saveInterfaceConfigFile = AutoRegisterHelper.getsaveInterfaceConfigFile(project)
                if (jarInterfaceConfigFile.exists()) {
                    //registerInfo 配置有改动就删除  jarInterfaceConfig.json
                    jarInterfaceConfigFile.delete()
                }

                if (saveInterfaceConfigFile.exists()) {
                    //registerInfo 配置有改动就删除  saveInterfaceConfigFile.json
                    saveInterfaceConfigFile.delete()
                }
                registerInfo.write(listInfo)


            }
        }
    }

    void reset() {
        list.each { info ->
            info.reset()
        }
    }

    @Override
    String toString() {
        StringBuilder sb = new StringBuilder(RegisterPlugin.EXT_NAME).append(' [\n')
        def size = list.size()
        for (int i = 0; i < size; i++) {
            sb.append('\t' + list.get(i).toString().replaceAll('\n', '\n\t'))
            if (i < size - 1)
                sb.append(',\n')
        }
        sb.append('\n]')
        return sb.toString()
    }
}