# 自动注册插件

最新版本:[![Download](https://api.bintray.com/packages/hellobilly/android/AutoRegister/images/download.svg)](https://bintray.com/hellobilly/android/AutoRegister/_latestVersion)

[原理说明](https://juejin.im/post/5a2b95b96fb9a045284669a9)

## 前言

此插件要解决的问题是：

- 工厂模式中工厂创建的产品需要注册到工厂类中，每新增一个产品要修改一次工厂类的代码
- 策略模式中所有策略需要注册到管理类中，每新增一种策略需要修改一次管理类的代码
- 接口及其管理类在基础库中定义，其实现类在不同的module中，需要在主工程中进行注册，每新增一个实现类注册需要手动添加
    - 比如在做组件化开发时，组件注册到主app中需要在主app的代码中import组件类进行注册，代码侵入性强

使用此插件后，在编译期（代码混淆之前）扫描所有打到apk包中的类，将`符合条件`的类收集起来，并生成注册代码到指定的类的static块中，自动完成注册

## 名词解释

- scanInterface         : (必须)字符串，接口名（完整类名），所有直接实现此接口的类将会被收集
- codeInsertToClassName : (必须)字符串，类名（完整类名），通过编译时生成代码的方式将收集到的类注册到此类的codeInsertToMethodName方法中
- codeInsertToMethodName: 字符串，方法名，注册代码将插入到此方法中。若未指定，则默认为static块,(方法名为：<clinit>)
- registerMethodName    : (必须)字符串，方法名，静态方法，方法的参数为 scanInterface
- scanSuperClasses      : 字符串或字符串数组，类名（完整类名），所有直接继承此类的子类将会被收集
- include               : 数组，需要扫描的类(正则表达式，包分隔符用/代替，如： com/billy/android/.*)，默认为所有的类
- exclude               : 数组，不需要扫描的类(正则表达式，包分隔符用/代替，如： com/billy/android/.*)，

## 功能简介

在apk打包过程中，对编译后的所有class（包含jar包中的class）进行扫描，将 scanInterface的实现类 或 scanSuperClasses的子类扫描出来，并在 codeInsertToClassName 类的 static块 中生成注册代码，如demo中：
```java
public class CategoryManager {
  static
  {
    CategoryManager.register(new CategoryA()); //scanInterface的实现类
    CategoryManager.register(new CategoryB()); //scanSuperClasses的子类
  }
}
```
    扫描的类包含：通过maven依赖的库、module依赖的library、aar包、jar包、AIDL编译后的类及当前module中的java类

要点：

- 生成的代码为： registerClassName.registerMethodName(scanInterface)
- 扫描到的类不能为接口或抽象类，并且提供public的无参构造方法（abstract类或scanInterface的子接口需要添加到exclude中）
- 如无特殊需求，codeInsertToClassName尽量与registerClassName是同一个类（registerClassName不配置则默认为codeInsertToClassName）

## 使用方式

- 在工程根目录的build.gradle中添加依赖：
```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.0.0'
        classpath 'com.billy.android:autoregister:x.x.x'
    }
}
```
    
 - 在application的build.gradle中添加配置信息：
 ```groovy
apply plugin: 'auto-register'
autoregister {
    registerInfo = [
        [
            'scanInterface'             : 'com.billy.app_lib_interface.ICategory'
            , 'scanSuperClasses'        : ['com.billy.android.autoregister.demo.BaseCategory']
            , 'codeInsertToClassName'   : 'com.billy.app_lib_interface.CategoryManager'
            //未指定codeInsertToMethodName，默认插入到static块中，故此处register必须为static方法
            , 'registerMethodName'      : 'register' //
            , 'exclude'                 : [
                //排除的类，支持正则表达式（包分隔符需要用/表示，不能用.）
                'com.billy.android.autoregister.demo.BaseCategory'.replaceAll('\\.', '/') //排除这个基类
            ]
        ],
        [
            'scanInterface'             : 'com.billy.app_lib.IOther'
            , 'codeInsertToClassName'   : 'com.billy.app_lib.OtherManager'
            , 'codeInsertToMethodName'  : 'init' //非static方法
            , 'registerMethodName'      : 'registerOther' //非static方法
        ]
    ]
}
```
更新日志：

### 2018-05-18 V1.3.0

修复gradlew build命令同时打debug和release包时，release包中注册的代码重复的问题

### 2018-03-12 V1.2.0

解决同时编译多个application module(点击Build -> Rebuild/Build APKs)时出现错误注册的问题

### 2018-01-15 V1.1.3

解决windows的兼容性问题

### 2018-01-09 V1.1.0

兼容java8

### 2017-12-17 V1.0.5
1. 从本仓库中删除为[ARouter](https://github.com/alibaba/ARouter)定制的路由自动注册插件，已提供ARouter专用精简版路由自动注册插件并提交[PR](https://github.com/alibaba/ARouter/pull/254)到ARouter仓库
2. 解决使用继承后，scanSuperClasses指定的类本身不会被自动注册的bug，并升级为V1.0.5
        
        
        scanSuperClasses 参数指定的父类不需要自动添加到exclude中
        抽象类不会被添加到自动注册列表。

### 2017-12-06 

新增[ARouter](https://github.com/alibaba/ARouter)定制的路由自动注册插件:auto-register-for-arouter

        classpath 'com.billy.android:auto-register-for-arouter:1.0.0'
        apply plugin: 'auto-register-for-arouter'

### 2017-11-21 V1.0.4
    生成的注册代码不再局限于static块中，可以在任意方法(codeInsertToMethodName)中
    需要注意： codeInsertToMethodName 与 registerMethodName 必须同时为static或非static
 
 
    