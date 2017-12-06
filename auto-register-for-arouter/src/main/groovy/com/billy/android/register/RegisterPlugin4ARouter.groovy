package com.billy.android.register

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * 自动注册插件入口
 * 专为ARouter自动注册路由使用的插件
 * @author billy.qi
 * @since 17/12/06 15:35
 */
public class RegisterPlugin4ARouter implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        /**
         * 注册transform接口
         */
        def isApp = project.plugins.hasPlugin(AppPlugin)
        if (isApp) {
            println 'project(' + project.name + ') apply auto-register-for-arouter plugin'
            def android = project.extensions.getByType(AppExtension)
            def transformImpl = new RegisterTransform(project)

            init(project)
            android.registerTransform(transformImpl)
        }
    }

    static void init(Project project) {
        //专为ARouter定制的参数
        AutoRegisterConfig config = new AutoRegisterConfig()
        config.registerInfo.add(buildInfo('IRouteRoot', 'registerRouteRoot'))
        config.registerInfo.add(buildInfo('IInterceptorGroup', 'registerInterceptor'))
        config.registerInfo.add(buildInfo('IProviderGroup', 'registerProvider'))

        config.project = project
        config.convertConfig()
        RegisterTransform.infoList = config.list
    }

    private static HashMap<String, Object> buildInfo(String interfaceName, String registerMethod) {
        Map<String, Object> info = new HashMap<>()
        info.put("scanInterface", 'com.alibaba.android.arouter.facade.template.' + interfaceName)
        info.put("codeInsertToClassName", "com.alibaba.android.arouter.core.LogisticsCenter")
        info.put("codeInsertToMethodName", "loadRootElement")
        info.put("registerMethodName", registerMethod)
        info.put("include", ["com/alibaba/android/arouter/routes/.*"])
        return info
    }

}
