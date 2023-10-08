package org.lkg.cache;

import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Objects;

/**
 * 过滤请求中的脏数据
 */
public class RequestCache {

    private final static HashSet<String> PARAM_RETURN_SET = new HashSet<>();
    private final static HashSet<String> PARAM_RETURN_TYPE_SET = new HashSet<>();

    public static void put(String str) {
        PARAM_RETURN_SET.add(str.trim());
    }

    public static void putType(String str) {
        PARAM_RETURN_TYPE_SET.add(str.trim());
    }


    public static boolean contains(String str) {
        // 无参 无需校验
        if (CollectionUtils.isEmpty(PARAM_RETURN_SET)) {
            return true;
        }
        if (Objects.isNull(str) || str.length() == 0) {
            System.out.println("1.本次请求不合理: -> " + str);
            return false;
        }
        // 答案和预期答案匹配度
        for (String s : PARAM_RETURN_SET) {
            int mid = s.length() >> 1;
            String prefix = s.substring(0, mid);
            String suffix = s.substring(mid);
            if (str.contains(s) || str.contains(s.toLowerCase()) || str.contains(prefix) || str.contains(suffix)) {
                return true;
            }
        }
        System.out.println("2.本次请求不合理: -> " + PARAM_RETURN_SET);
        return false;
    }


    public static boolean isIntegerType() {
        for (String text : PARAM_RETURN_TYPE_SET) {
           if (text.contains("int") || text.contains("Integer") || text.contains("long") || text.contains("Long")) {
               return true;
           }
        }
        return false;
    }


    public static void clear() {
        PARAM_RETURN_SET.clear();
        PARAM_RETURN_TYPE_SET.clear();
    }

}
