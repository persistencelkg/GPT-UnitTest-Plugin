package org.lkg.core;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FindMethodService {

    /**
     * 查询接口实现类方法
     * @param psiMethod
     * @param project
     * @return
     */
    public static PsiMethod findImplMethodByInterfaceDeclare(PsiMethod psiMethod, Project project) {
        PsiClass parentOfType = PsiTreeUtil.getParentOfType(psiMethod, PsiClass.class);
        // 不存在
        if (Objects.isNull(parentOfType)) {
            return psiMethod;
        }
        // 非接口
        if (!parentOfType.isInterface()) {
            return psiMethod;
        }
        PsiClass[] classes = PsiShortNamesCache.getInstance(project).getClassesByName(
                Objects.requireNonNull(parentOfType.getName()), GlobalSearchScope.allScope(project));
        // 静态代码无法解决多态问题
        if (classes.length > 1) {
            return psiMethod;
        }
        for (PsiClass aClass : classes) {
            if (InheritanceUtil.isInheritor(aClass, Objects.requireNonNull(parentOfType.getQualifiedName()))) {
                // 实现类找茶渣接口方法引用
                PsiMethod[] methodsByName = aClass.findMethodsByName(psiMethod.getName(), false);
                return findTargetMethod(psiMethod, methodsByName);
            }
        }
        return psiMethod;
    }



    public static PsiMethod findMethodByName(String referenceName, PsiExpression[] typeArguments, Project project) {
        PsiMethod[] methods = PsiShortNamesCache.getInstance(project).getMethodsByName(referenceName, GlobalSearchScope.projectScope(project));
        if (methods.length > 0) {
            PsiMethod psiMethod = findTargetMethod(typeArguments, methods);
            if (psiMethod != null) {
                return psiMethod;
            }
        }
        return null;
    }

    private static PsiMethod findTargetMethod(PsiMethod target, PsiMethod[] methods) {
        PsiParameterList targetParameterList = target.getParameterList();
        PsiParameter[] targetParameterListParameters = targetParameterList.getParameters();
        for (PsiMethod method : methods) {
            // 找到的类型 和 引用类型比对
            PsiParameterList parameterList = method.getParameterList();
            PsiParameter[] parameters = parameterList.getParameters();
            if (parameters.length != targetParameterListParameters.length) {
                continue;
            }
            boolean flag = true;
            for (int i = 0; i < parameters.length; i++) {
                if (!parameters[i].getType().equals(targetParameterListParameters[i].getType())) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                return method;
            }
            System.out.println("未找到目标实现类------》");
        }
        return target;
    }

    @Nullable
    private static PsiMethod findTargetMethod(PsiExpression[] typeArguments, PsiMethod[] methods) {
        for (PsiMethod psiMethod : methods) {
            // 找到的类型 和 引用类型比对
            PsiParameterList parameterList = psiMethod.getParameterList();
            PsiParameter[] parameters = parameterList.getParameters();
            if (parameters.length == typeArguments.length) {
                boolean match = true;
                for (int i = 0; i < typeArguments.length; i++) {
                    PsiType findType = parameters[i].getType();
                    PsiType type = typeArguments[i].getType();
                    // 引用传递null 符合数量一致即可
                    if (Objects.nonNull(type) && Objects.equals("null", type.getCanonicalText())) {
                        break;
                    }
                    if (!findType.equals(type)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return psiMethod;
                }
            }
        }
        return null;
    }
}
