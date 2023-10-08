package org.lkg.dos;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import java.util.LinkedHashMap;
import java.util.Map;

public class PsiMethodDo {

    /**
     * 枚举key： 引用文本， value ，枚举类
     * ps：Test.A.getCode  -> Test
     */
    private LinkedHashMap<String, PsiClass> classMap;

    /**
     * 本项目中依赖的方法： key 引用文本， value：具体依赖的方法
     * ps：testService.getUser(int a) ->  testService impl method
     */
    private Map<String, PsiMethod> projectReferenceMethod;


    public LinkedHashMap<String, PsiClass> getClassMap() {
        return classMap;
    }


    public Map<String, PsiMethod> getProjectReferenceMethod() {
        return projectReferenceMethod;
    }

    public void setProjectReferenceMethod(Map<String, PsiMethod> projectReferenceMethod) {
        this.projectReferenceMethod = projectReferenceMethod;
    }

    public void setClassMap(LinkedHashMap<String, PsiClass> classMap) {
        this.classMap = classMap;
    }
}
