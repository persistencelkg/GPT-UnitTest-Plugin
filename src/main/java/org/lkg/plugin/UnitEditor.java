package org.lkg.plugin;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.util.PsiTreeUtil;
import org.lkg.cache.RequestCache;
import org.lkg.constant.UseCaseConstants;
import org.lkg.core.FindMethodService;
import org.lkg.core.RequestService;
import org.lkg.dos.PsiMethodDo;
import org.lkg.pattern.UseCaseSubject;
import org.lkg.util.DomUtil;
import org.lkg.util.NotificationUtil;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.lkg.constant.UseCaseConstants.MATH_EXPRESSION_TAG;

/**
 * @description: 单元测试
 * @author: 李开广
 * @date: 2023/5/4 2:48 PM
 */

public class UnitEditor extends AnAction {

    private final Logger log = Logger.getLogger(this.getClass().getSimpleName());

    private UseCaseSubject useCaseSubject = UseCaseSubject.getInstance();

    private static final String suffix = "";

    private static final String METHOD_DETAIL = "\n\nThe method depends on object info:\n";

    private static final String ENUM_CLASS_TITLE = "\n\nThe follow is `{enum_class}` info:\n";

    private static final String NEST_METHOD_TITLE = "\n\nThe follow is `{nest_express}` method body:\n";

    private static final Integer LAYER_COUNT = 7;

    private Project project;

    private String packageName;


    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (Objects.isNull(editor)) {
            return;
        }
        if (Objects.isNull(psiFile)) {
            return;
        }
        int offset = editor.getCaretModel().getOffset();

        // 当前选中的文本
        PsiElement psiElement = psiFile.findElementAt(offset);
        if (Objects.isNull(psiElement)) {
            return;
        }

        log.info("选中文本" + psiElement.getText());

        // 调用处选中-解析文本
        PsiReference psiReference = psiFile.findReferenceAt(offset);

        AtomicReference<PsiMethod> psiMethod = new AtomicReference<>();
        String requestTxt;
        // 全选
        if (Objects.isNull(psiReference)) {
            String selectedText = editor.getSelectionModel().getSelectedText();
            if (Objects.isNull(selectedText) || baseJudge(selectedText.length(), 15)) {
                NotificationUtil.info("无效选中，具体的操作流程可见插件主页");
                return;
            }
            if (!selectedText.contains("(") || !selectedText.contains(")")) {
                NotificationUtil.info("被调用方法请从修饰符(如public xx)开始到参数结束");
                return;
            }
            // 判断是否被调用处开始选中,则直接获取文本，进行解析
            PsiMethod method = directParseSelectText(psiFile, selectedText);

            // 参照 https://victorchow.github.io/2020/07/24/IDEA%E6%8F%92%E4%BB%B6%E5%BC%80%E5%8F%912.html
            if (Objects.isNull(method)) {
                NotificationUtil.info("改类没有可以穷举的用例");
                return;
            }
            psiMethod.set(method);
        } else {
            // 调用处文本解析
            PsiElement resolve = psiReference.resolve();
            if (Objects.isNull(resolve) || baseJudge(resolve.getText().length(), 15)) {
                NotificationUtil.info("无效选中，具体的操作流程可见插件主页..");
                return;
            }
            // 自上而下
            resolve.accept(new JavaRecursiveElementVisitor() {
                @Override
                public void visitMethod(PsiMethod method) {
                    super.visitMethod(method);
                    psiMethod.set(method);
                }
            });
        }
        requestTxt = deepParseComplexObject(psiMethod.get());
        if (Objects.isNull(requestTxt)) {
            return;
        }
        log.info("请求内容：\n" + requestTxt + '\n');
        // 处理请求
//        request(requestTxt);
    }


    private String deepParseComplexObject(PsiMethod method) {
        PsiMethodDo psiMethodDo = prepareParseMethod(method);
        if (Objects.isNull(psiMethodDo)) {
            return null;
        }
        method = FindMethodService.findImplMethodByInterfaceDeclare(method, project);
        StringBuilder sb = new StringBuilder();
        String text = method.getText();
        sb.append("'''\n").append(text).append("\n'''");
//        sb.append(METHOD_DETAIL);
        // 解析方法中引用的全局常量
        String globalUseConst = parseGlobalEnumConst(psiMethodDo.getClassMap(), text);
        if (globalUseConst.length() != 0) {
            sb.append(globalUseConst).append("\n");
        }

        // 处理本类定义的全局常量
        PsiClass containingClass = method.getContainingClass();
        if (Objects.nonNull(containingClass)) {
            PsiField[] allFields = containingClass.getAllFields();
            StringBuilder globalFields = new StringBuilder();
            for (PsiField pp : allFields) {
                // 仅仅加载需要用的常量
                if (text.contains(pp.getName())) {
                    globalFields.append(pp.getText());
                }
            }
            if (globalFields.length() > 0) {
                sb.append(globalFields.toString()).append("\n");
            }
        }

//        PsiParameterList parameterList = method.getParameterList();
//        // 无参
//        if (parameterList.isEmpty()) {
//            processReturnValue(method, sb);
//            return sb.toString();
//        }
//
//        // 处理参数
//        PsiParameter[] parameters = parameterList.getParameters();
//        StringBuilder paramBuild = new StringBuilder();
//        for (PsiParameter parameter : parameters) {
//            // 解析泛型里的参数 Map<Person, Data>
//            paramBuild.append(parseGenericObject(parameter.getTypeElement(), parameter.getType(), parameter.getName())).append('\n');
//        }
//
//        String trim = paramBuild.toString().trim();
//        if (trim.length() != 0) {
//            sb.append(MessageFormat.format("- param: \n{0}", trim)).append("\n");
//        }
//        // 处理返回值
//        processReturnValue(method, sb);

        // 处理嵌套方法
        Map<String, String> getMethodName = parseNestMethodWithRecursive(psiMethodDo.getProjectReferenceMethod(), method.getContainingClass());
        if (Objects.isNull(getMethodName)) {
            return null;
        }
        log.info("本次解析的自定义嵌套方法 " + getMethodName.keySet());

        getMethodName.forEach((k, v) -> {
            if (globalUseConst.contains(k)) {
                return;
            }
            sb.append(NEST_METHOD_TITLE.replace("{nest_express}", k)).append(v).append("\n");
        });
        return sb.toString();
    }

    private PsiMethodDo prepareParseMethod(PsiMethod method) {
        PsiClass psiClass = method.getContainingClass();
        if (Objects.nonNull(psiClass) && Objects.nonNull(psiClass.getQualifiedName())) {
            // 取二级前缀
            String qualifiedName = psiClass.getQualifiedName();
            if (UseCaseConstants.isContainNativeMethodStr(qualifiedName)) {
                NotificationUtil.warning("不是一个合法的包名，拒绝测试用例执行");
                return null;
            }

            String[] split = qualifiedName.split("\\.");
            this.packageName = String.join(".", split[0], split.length > 1 ? split[1] : "");
        }
        PsiMethodDo psiMethodDo = new PsiMethodDo();
        // key 常量引用，  value 常量类
        LinkedHashMap<String, PsiClass> classMap = new LinkedHashMap<>();
        LinkedHashMap<String, PsiMethod> psiMethodMap = new LinkedHashMap<>(64);

        LinkedHashMap<String, Set<String>> samePrefixMap = new LinkedHashMap<>();

        method.accept(new JavaRecursiveElementVisitor() {
            /**
             * 获取到所有定义的类，不包括引用类，比如 A a = new A()中等号左边内容， 或者是泛型参数、返回值
             * @param type
             */
            @Override
            public void visitTypeElement(PsiTypeElement type) {
                super.visitTypeElement(type);
                PsiType psiType = type.getType();
                if (((psiType instanceof PsiPrimitiveType) || isNativeObject(psiType))) {
                    return;
                }
                if (psiType instanceof PsiClassType) {
                    PsiClassType classType = (PsiClassType) psiType;
                    classMap.put(classType.getName(), classType.resolve());
                }
            }


            /**
             * 获取所有引用
             * 1. 分类收集信息
             * @param expression
             */
            @Override
            public void visitReferenceExpression(PsiReferenceExpression expression) {
                super.visitReferenceExpression(expression);
                String canonicalText = expression.getCanonicalText();
                PsiElement resolve = expression.resolve();

                if (UseCaseConstants.isContainNativeMethodStr(canonicalText)) {
                    return;
                }
                // 收集所有类
                if (Objects.nonNull(resolve) && (resolve instanceof PsiClass)) {
                    classMap.put(canonicalText.replaceAll(packageName + ".", ""), (PsiClass) resolve);

                }
                // 收集所有本类的常量引用
                if (UseCaseConstants.isContainConstStr(canonicalText) || canonicalText.contains(packageName)) {
                    // 构造相同key前缀
                    String entryKey;
                    if (canonicalText.contains(".")) {
                        int end = canonicalText.lastIndexOf(".");
                        if (canonicalText.contains(packageName)) {
                            entryKey = canonicalText.substring(end + 1);
                            // 舍弃掉包名部分
                            canonicalText = entryKey;
                        } else {
                            entryKey = canonicalText.substring(0, canonicalText.indexOf("."));
                        }
                    } else {
                        entryKey = canonicalText;
                    }
                    samePrefixMap.computeIfAbsent(entryKey, k -> new LinkedHashSet<>()).add(canonicalText);
                }
            }

            /**
             * 获取所有方法引用，处理嵌套函数的核心，比较粗糙，只能解析带括号内容 a()
             * @param expression
             */
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                // 不需要二次resolve 但是 使用PsiTreeUtil 是必须使用的
                PsiReferenceExpression methodExpression = expression.getMethodExpression();
                PsiExpressionList typeArguments = expression.getArgumentList();
                PsiMethod psiMethod = FindMethodService.findMethodByName(methodExpression.getReferenceName(), typeArguments.getExpressions(), project);
                if (Objects.nonNull(psiMethod)) {
                    psiMethodMap.putIfAbsent(methodExpression.getCanonicalText(), psiMethod);
                }
            }

        });


        // 对同一个key前缀做 按长度排序，剔除短的引用
        HashSet<String> maxLenPrefixSet = new HashSet<>();
        samePrefixMap.forEach((k, v) -> {
            String s = v.stream().max(Comparator.comparingInt(String::length)).orElse(null);
            maxLenPrefixSet.add(s);
        });

        // 过滤掉重叠部分
        classMap.entrySet().removeIf(ref -> maxLenPrefixSet.contains(ref.getKey()));

        LinkedHashMap<String, PsiClass> finalMap = new LinkedHashMap<>();
        for (Map.Entry<String, PsiClass> entry : classMap.entrySet()) {
            PsiClass val = entry.getValue();
            if (Objects.isNull(val.getName())) {
                continue;
            }
            finalMap.put(entry.getKey(), val);
            // Test.A.getCode : Test -> Test.A -> Test.A.getCode
            maxLenPrefixSet.forEach(prefix -> {
                if (prefix.contains(val.getName())) {
                    finalMap.put(prefix, val);
                }
            });


        }

        // 过滤方法引用 和 所有引用重叠部分
        psiMethodMap.entrySet().removeIf(next -> finalMap.keySet().contains(next.getKey()));

        psiMethodDo.setClassMap(finalMap);
        psiMethodDo.setProjectReferenceMethod(psiMethodMap);
        return psiMethodDo;
    }


    private void processReturnValue(PsiMethod method, StringBuilder sb) {
        // 处理返回值
        String returnValue = doProcessReturnValue(method).trim();
        if (returnValue.length() == 0) {
            // do nothing
        } else if (returnValue.equalsIgnoreCase("void")) {
            sb.append("- return value: void").append("\n");
        } else {
            // 基本类型的不用换行
            sb.append("- return value:").append(returnValue.length() > 7 ? "\n" + returnValue : returnValue);
        }
    }

    private LinkedHashMap<String, String> parseNestMethodWithRecursive(Map<String, PsiMethod> map, PsiClass codeReferenceClass) {
        LinkedHashMap<String, String> methodMap = new LinkedHashMap<>(16);

        Set<Map.Entry<String, PsiMethod>> entries = map.entrySet();
        for (Map.Entry<String, PsiMethod> entry : entries) {

            PsiMethod val = entry.getValue();
            // 如果是一个方法不是本类、枚举类那么就需要进行获取实现类的方法
            if (!UseCaseConstants.isContainConstStr(entry.getKey()) ||
                    !Objects.requireNonNull(codeReferenceClass.getName()).contains(Objects.requireNonNull(Objects.requireNonNull(val.getContainingClass()).getName()))) {
                val = FindMethodService.findImplMethodByInterfaceDeclare(val, project);
            }
            // 第一层
            methodMap.putIfAbsent(val.getName() + val.getParameterList().getText(), val.getText());
            LinkedHashMap<String, String> nestMap;
            try {
                nestMap = parseNestMethodWithRecursive(0, val, methodMap);
                if (Objects.isNull(nestMap)) {
                    continue;
                }
                // 嵌套子层
                methodMap.putAll(nestMap);
            } catch (Exception ex) {
                NotificationUtil.warning(ex.getMessage());
                return null;
            }

        }
        return methodMap;
    }

    private LinkedHashMap<String, String> parseNestMethodWithRecursive(int layerCount, PsiMethod val, LinkedHashMap<String, String> methodMap) {
        if (layerCount > LAYER_COUNT) {
            throw new RuntimeException("嵌套函数超过" + layerCount + "层，超过部分不再继续解析");
        }
        // 这种方式遍历的目标结果还不是实际引用，因此需要通过resolve 去深度解析
        Collection<PsiMethodCallExpression> children = PsiTreeUtil.findChildrenOfType(val, PsiMethodCallExpression.class);
        if (children.size() == 0) {
            return null;
        }
        for (PsiMethodCallExpression methodCallExpression : children) {
            PsiReferenceExpression referenceExpression = methodCallExpression.getMethodExpression();
            PsiElement target = referenceExpression.resolve();
            if (Objects.nonNull(target) && target instanceof PsiMethod) {
                PsiMethod pp = (PsiMethod) target;
                PsiClass containingClass = pp.getContainingClass();
                // 只关注本项目的方法
                if (Objects.nonNull(containingClass) && isTheSameProject(containingClass.getQualifiedName())) {
                    methodMap.putIfAbsent(pp.getName() + pp.getParameterList().getText(), pp.getText());
                    parseNestMethodWithRecursive(++layerCount, pp, methodMap);
                }

            }
        }
        return methodMap;

    }

    private boolean isTheSameProject(String qualifiedName) {
        if (Objects.isNull(qualifiedName)) {
            return false;
        }
        return qualifiedName.startsWith(packageName) || qualifiedName.equalsIgnoreCase(packageName);
    }



    private String doProcessReturnValue(PsiMethod method) {
        PsiTypeElement returnTypeElement = method.getReturnTypeElement();
        PsiType returnType = method.getReturnType();
        if (PsiType.VOID.equals(returnType)) {
            return "void";
        }
        return parseGenericObject(returnTypeElement, returnType, null);
    }

    /**
     * 解析项目的枚举类、常量类
     * 1. 常量类必须解析类的字段信息，常量值，常量
     * 2. 遍历类的字段和引用类型数据
     *
     * @param enumClassSet
     * @return
     */
    private String parseGlobalEnumConst(LinkedHashMap<String, PsiClass> enumClassSet, String methodTxt) {
        StringBuilder sb = new StringBuilder();
        enumClassSet.forEach((k, v) -> {
            sb.append(ENUM_CLASS_TITLE.replace("{enum_class}", k));
            // 非常量类 处理
            if (!UseCaseConstants.isContainConstStr(k)) {
                sb.append(dealWithNotConstClass(v, methodTxt));
                return;
            }
            sb.append("public ").append(v.isEnum() ? "enum" : "class").append(" ").append(v.getName()).append("{\n\n");
            PsiField[] fields = v.getFields();

            for (PsiField field : fields) {
                String text = field.getText();
                if (Objects.isNull(text)) {
                    log.warning(field.getName() + "--> k" + k + "null");
                    continue;
                }
                if (v.isEnum()) {
                    if (field.hasModifier(JvmModifier.PRIVATE)) {
                        sb.append(text).append("\n");
                        continue;
                    }
                }
                // 定位是那个目标常量值
                String[] split = k.split("\\.");
                // 没有通过显示调用枚举，就要全部告诉模型
                if (split.length == 0) {
                    sb.append(text).append("\n");
                    continue;
                }
                for (String s : split) {
                    if (field.getText().contains(s)) {
                        sb.append(text).append("\n");
                        break;
                    }
                }
            }

            if (v.isEnum()) {
                PsiMethod[] constructors = v.getConstructors();
                for (PsiMethod constructor : constructors) {
                    sb.append(constructor.getText()).append("\n");
                }
            }
            sb.append("}").append("\n");
        });
        return sb.toString();

    }

    private String dealWithNotConstClass(PsiClass psiClass, String methodTxt) {
        StringBuilder sb = new StringBuilder("public class " + psiClass.getName() + " {\n");
        PsiMethod[] methods = psiClass.getMethods();
        PsiField[] fields = psiClass.getFields();
        for (PsiField field : fields) {
            String name = field.getName();
            if (methodTxt.contains(name)
                    || methodTxt.contains(Character.toUpperCase(name.charAt(0)) + name.substring(1)))
                sb.append(field.getText()).append("\n");
        }
        for (PsiMethod method : methods) {
            // 不关心object | 没用的方法
            if (UseCaseConstants.isObjectMethod(method.getName()) || !methodTxt.contains(method.getName())) {
                continue;
            }
            sb.append(method.getText()).append("\n");
        }
        sb.append("}");
        log.info("自定义类的打印内容" + sb.toString());
        return sb.toString();
    }

    private String parseGenericObject(PsiTypeElement typeElement, PsiType type, String psiName) {
        StringBuilder sb = new StringBuilder();
        // 处理泛型自定义对象
        if (Objects.nonNull(typeElement)) {
            PsiJavaCodeReferenceElement javaCode = typeElement.getInnermostComponentReferenceElement();
            if (Objects.nonNull(javaCode)) {
                PsiReferenceParameterList psiReferenceParameterList = javaCode.getParameterList();
                if (Objects.nonNull(psiReferenceParameterList)) {
                    PsiType[] typeArguments = psiReferenceParameterList.getTypeArguments();
                    if (typeArguments.length != 0) {
                        for (PsiType argument : typeArguments) {
                            // 内置的方法、三方包不参与深度解析
                            if (isNativeObject(argument)) {
                                continue;
                            }
                            String canonicalText = argument.getCanonicalText();
                            if (Objects.nonNull(psiName)) {
                                RequestCache.put(psiName);
                            }
                            RequestCache.putType(canonicalText);
//                            sb.append(" ").append(canonicalText).append(";");
                            buildComplexNestedObject(sb, argument, null);
                        }
                        return sb.toString();
                    }
                }
            }
        }

        // 内置跳过
        if ((type instanceof PsiPrimitiveType) || isNativeObject(type)) {
            if (Objects.nonNull(psiName)) {
//                sb.append(psiName);
                RequestCache.put(psiName);
            }
            RequestCache.putType(type.getCanonicalText());
//            sb.append(" ").append(type.getCanonicalText()).append(";");
            return sb.toString();
        }
        // 泛型普通包装类 + 普通类
        return buildComplexNestedObject(sb, type, psiName);
    }

    /**
     * 1. java基础对象
     * 2. 枚举常量应该体现在使用时，不需要直接给出
     *
     * @param argument 目标参数
     * @return 是否是简单对象
     */
    private boolean isNativeObject(PsiType argument) {
        if (Objects.isNull(argument)) {
            return false;
        }
        //  Map<String, Person>
        return UseCaseConstants.isContainNativeMethodStr(argument.getCanonicalText());
    }

    private PsiMethod directParseSelectText(PsiFile psiFile, String selectText) {
//        String name = psiFile.getName();
//        name = name.substring(0, name.lastIndexOf('.'));
//        PsiClass[] classesByName = PsiShortNamesCache.getInstance(psiProject).getClassesByName(name, GlobalSearchScope.projectScope(psiProject));
        PsiClass[] classesByName = ((PsiJavaFileImpl) psiFile).getClasses();
        if (classesByName.length == 0) {
            return null;
        }
        PsiClass psiClasses = classesByName[0];
        PsiMethod[] methods = psiClasses.getMethods();
        if (methods.length == 0) {
            return null;
        }
        // 重载方法处理
        PsiMethod method = null;
        String overLoadMethodName = getOverLoadMethodName(selectText);


        for (PsiMethod psiMethod : methods) {
            if (overLoadMethodName.equals(psiMethod.getName())) {
                method = psiMethod;
                break;
            }
        }
        return method;

    }

    private String getOverLoadMethodName(String selectText) {
        String[] split = selectText.split("\\(");
        String withOutParamSignature = split[0];
        int len = withOutParamSignature.length();
        char[] chars = withOutParamSignature.toCharArray();
        while ((baseJudge(0, len)) && (chars[len - 1] == ' ')) {
            len--;
        }
        String str = withOutParamSignature.substring(0, len);
        String[] s = str.split(" ");
        return s[s.length - 1];

    }

    private void request(String ret) {

        useCaseSubject.onChange("", UseCaseConstants.TYPE_START_PROGRESS);


        Thread thread = new Thread(() -> {
            try {

                String submit = RequestService.submitWithPublicV2(ret);
                submit = afterRequest(submit);
                if (Objects.isNull(submit)) {
                    NotificationUtil.warning("模型推断异常, 请换个方法后再尝试本方法", (String txt, Integer type) -> useCaseSubject.onChange(txt, type));
                    return;
                }
                useCaseSubject.onChange(submit, UseCaseConstants.TYPE_SHOW_CASE);
            } finally {
                RequestCache.clear();
            }
        });

        thread.start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ignored) {
        }
    }

    private String beforeRequest(String string) {
        return suffix + "'''" + string.replaceAll("\n", " ") + "'''";
    }

    private String afterRequest(String resp) {
        // 避免答非所问
        if (!RequestCache.contains(resp)) {
            return null;
        }
        // 解析计算类函数
        if (resp.contains(MATH_EXPRESSION_TAG)) {
            return DomUtil.rebuildCalcResp(resp);
        }

        return resp;
    }

    private String buildComplexNestedObject(StringBuilder sb, PsiType psiType, String psiName) {
        PsiClass psiClass;
        if (psiType instanceof PsiClassReferenceType) {
            psiClass = ((PsiClassReferenceType) psiType).resolve();
        } else {
            return sb.toString();
        }
        if (Objects.isNull(psiClass)) {
            return sb.toString();
        }
        // 是否需要考虑继承的属性，当前并未考虑
        PsiField[] fields = psiClass.getFields();
        log.info(Arrays.toString(psiClass.getAllFields()));
        // 检测是否有属性
        if (baseJudge(fields.length, 1) || !detectField(fields)) {
            log.info(MessageFormat.format("{0}无有效字段", psiClass));
            return sb.toString();
        }
        if (Objects.nonNull(psiName)) {
            RequestCache.put(psiName);
        }
        sb.append(psiClass.getText());
        return sb.toString();
    }

    private boolean detectField(PsiField[] fields) {
        for (PsiField field : fields) {
            if (!UseCaseConstants.IGNORED_FIELDS.contains(field.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean baseJudge(int length, int i) {
        return length < i;
    }


    @Override
    public void update(AnActionEvent e) {
        // 确保项目能正确打开编辑器，并获取到它的实例再去展示
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile != null);
    }


}



