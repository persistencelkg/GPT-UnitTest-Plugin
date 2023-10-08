package org.lkg.constant;

import java.util.ArrayList;

/**
 * @description: 用例常量类
 * @author: 李开广
 * @date: 2023/5/6 1:52 PM
 */
public class UseCaseConstants {

    public static final Integer TYPE_START_PROGRESS = 1;

    public static final Integer TYPE_SHOW_CASE = 9;

    public static final String MATH_EXPRESSION_TAG = "calc";


    public static final ArrayList<Character> FOUR_ARITHMETIC_OPERATE_ESCAPE_QUOT_LIST = new ArrayList<Character>() {{
        add('(');
        add(')');
        add('+');
        add('*');
        add('^');
        add('{');
        add('[');
    }};

    public static final ArrayList<String> JAVA_PACKAGE_PREFIX_LIST = new ArrayList<String>() {{
        add("java.");
        add("org.springframework");
        add("org.apache");
        add("io.netty");
        add("com.alibaba");
    }};

    public static final ArrayList<String> IGNORED_FIELDS = new ArrayList<String>() {
        {
            add("serialVersionUID");
            add("id");
            add("createTime");
            add("updateTime");
            add("createDate");
            add("updateDate");
        }
    };


    // 筛选结尾包括 Consts. Constants. Constant Type Status Enums Enum.

    private static final ArrayList<String> CONST_TAG = new ArrayList<String>() {
        {
            add("Const");
            add("Consts");
            add("Constants");
            add("Constant");
            add("Type");
            add("Status");
            add("Enums");
            add("Enum");
        }
    };


    private static final ArrayList<String> OBJECT_METHOD = new ArrayList<String>() {
        {
            add("build");
            add("toString");
            add("hashCode");
            add("equals");
            add("wait");
            add("notify");
            add("notifyAll");
        }
    };

    public static boolean isObjectMethod(String text) {
        return OBJECT_METHOD.stream().anyMatch(text::startsWith);
    }

    public static boolean isContainNativeMethodStr(String text) {
        return JAVA_PACKAGE_PREFIX_LIST.stream().anyMatch(text::startsWith);
    }

    public static boolean isContainConstStr(String statement) {
        for (String s : CONST_TAG) {
            if (statement.contains(s)) {
                return true;
            }
        }
        return false;
    }
}
