package org.lkg.util;

import org.lkg.cache.RequestCache;
import org.lkg.constant.UseCaseConstants;
import org.springframework.util.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

import static org.lkg.constant.UseCaseConstants.MATH_EXPRESSION_TAG;

public class DomUtil {

    private static final Logger log = Logger.getLogger(DomUtil.class.getSimpleName());


    public static List<String> listData(String txt, String tag) {
        if (Objects.isNull(tag) || !txt.contains(tag)) {
            return null;
        }
        List<String> adaptData = new ArrayList<>();
        adaptData.add(tag);
        //| Err |
        String spaceTag = tag.charAt(0) + " " + tag.substring(1, tag.length() - 1) + " " + tag.charAt(tag.length() - 1);
        String s1 = tag.charAt(0) + " " + tag.substring(1, tag.length() - 1);
        String s2 =  " " + tag.charAt(tag.length() - 1);
        if (txt.contains(spaceTag) || txt.contains(s1) || txt.contains(s2)) {
            adaptData.add(spaceTag);
            adaptData.add(s1);
            adaptData.add(s2);
        }
        List<String> list = new ArrayList<>();
        for (String adaptDatum : adaptData) {
            tag = adaptDatum;
            int start = txt.indexOf(tag);
            int end = txt.substring(start + 1).indexOf(tag);
            boolean flag = start != -1 && end != -1;
            if (!flag) {
                continue;
            }
            while (start != -1 && end != -1) {
                String trim = txt.substring(start, start + end + tag.length() + 1);
                list.add(trim);
                int assitantStart = start + end + tag.length() + 1;
                int assitantEnd = txt.substring(assitantStart).indexOf(tag);
                // 连续内容 分组，不连续只关注标签内部数据
//            String assistantPart = txt.substring(assitantStart, assitantStart + assitantEnd).trim();
                start = assitantStart + assitantEnd;
                end = txt.substring(start + 1).indexOf(tag);
            }
        }

        return list;
    }


    private static Set<String> parseWithTag(String originText, String tag) {
        Set<String> list = new LinkedHashSet<>();
        if (StringUtils.isEmpty(originText)) {
            return list;
        }
        String prefix = getPrefixTag(tag);
        String suffix = getSuffixTag(tag);
        int start = originText.indexOf(prefix);
        int end = originText.indexOf(suffix);
        String temp = originText;
        while (start != -1 && end != -1) {
            list.add(temp.substring(start + prefix.length(), end));
            int minEnd = Math.min(end + suffix.length(), temp.length());
            temp = temp.substring(minEnd);
            start = temp.indexOf(prefix);
            end = temp.indexOf(suffix);
        }
        return list;
    }

    private static String getPrefixTag(String tag) {
        boolean startsWith = tag.startsWith("<");
        boolean endWith = tag.endsWith(">");
        if (startsWith ^ endWith) {
            throw new RuntimeException("not valid prefix tag:" + tag);
        }
        if (tag.startsWith("</")) {
            return tag.replace("</", "<");
        }
        if (startsWith) {
            return tag;
        }
        return MessageFormat.format("<{0}>", tag);
    }

    private static String getSuffixTag(String tag) {
        boolean startsWith = tag.startsWith("</");
        boolean endWith = tag.endsWith(">");
        if (startsWith ^ endWith) {
            throw new RuntimeException("not valid suffix tag:" + tag);
        }
        if (startsWith) {
            return tag;
        }
        return MessageFormat.format("</{0}>", tag);
    }

    private static Object calculate(String code) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");

        Object result = null;
        try {
            result = engine.eval(code);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String rebuildCalcResp(String resp) {
        Set<String> mathExpressionSet = DomUtil.parseWithTag(resp, MATH_EXPRESSION_TAG);
        log.info(mathExpressionSet.toString());
        for (String val : mathExpressionSet) {
            Object calculate = DomUtil.calculate(val);
            log.info(val + "计算结果：" + calculate);

            if (Objects.isNull(calculate)) {
                continue;
            }
            String res = calculate.toString();
            // fix js var type result is float
            if (RequestCache.isIntegerType()) {
                res = res.replace(".0", "");
            }
            String key = MessageFormat.format("{0}{1}{2}", getPrefixTag(MATH_EXPRESSION_TAG), escapseChar(val), getSuffixTag(MATH_EXPRESSION_TAG));
            resp = resp.replaceAll(key, res);
        }
        return resp;
    }

    private static String escapseChar(String val) {
        char[] chars = val.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char ch : chars) {
            if (UseCaseConstants.FOUR_ARITHMETIC_OPERATE_ESCAPE_QUOT_LIST.contains(ch)) {
                sb.append("\\").append(ch);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }


    public static void main(String[] args) {
//
//        log.info("".replaceAll("\\(1 \\+ - >> = << \\* / \\{ }; \\. 7\\) \\^ > < var res  ", "1"));
//
//        log.info(rebuildCalcResp("<calc>var f1= 1 + 0 - 8; var f2 = f1 + 1 - 5; var f3 = f2 + 2 - 4; var res = f3; res;</calc>"));
        Set<String> aa = parseWithTag("<calc>aaa</calc>lkg", "calc");
        log.info(aa.toString());
    }

    private static void test1() {
        String test = "<calc> var res = (1 * Math.pow(7, 2)) + (2 * Math.pow(9, 2)) + (3 * Math.pow(10, 2)) + (4 * Math.pow(5, 2)) + (5 * Math.pow(8, 2)) + (6 * Math.pow(4, 2)) + (7 * Math.pow(2, 2)) + (8 * Math.pow(1, 2)) + (9 * Math.pow(3, 2)); res;</calc>  <calc> var len = [1,24,4]; var a = len.length; a;</calc>";
        String test2 = test;
        Set<String> aa = parseWithTag(test, "calc");
        log.info(aa + "");
        for (String s : aa) {
            String s1 = escapseChar(s);
            log.info(s1);
            test = test.replaceAll(s1, calculate(s).toString());
        }
        log.info(test);

    }
}
