package com.billy.android.register
class JarConfigInfo {


    List<ClassList> getClassLists() {
        return classLists
    }

    void setClassLists(List<ClassList> classLists) {
        this.classLists = classLists
    }
    List<ClassList> classLists;

    class ClassList{

        String getJarMd5() {
            return jarMd5
        }

        void setJarMd5(String jarMd5) {
            this.jarMd5 = jarMd5
        }

        String getJarFilePath() {
            return jarFilePath
        }

        void setJarFilePath(String jarFilePath) {
            this.jarFilePath = jarFilePath
        }

        String getInterfaceName() {
            return interfaceName
        }

        void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName
        }

        boolean getIsManagerClass() {
            return isManagerClass
        }

        void setIsManagerClass(boolean isManagerClass) {
            this.isManagerClass = isManagerClass
        }

        String getClassName() {
            return className
        }

        void setClassName(String className) {
            this.className = className
        }
        String className
        String jarMd5
        String jarFilePath
        String interfaceName
        boolean  isManagerClass



    }


}