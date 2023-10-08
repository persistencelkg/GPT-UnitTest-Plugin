package org.lkg.util;

import org.lkg.cache.PsiCacheManager;
import org.lkg.dos.MoreConversation;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author： likaiguang
 */
public class PromptHelper {


    private static final String PROMPT_CACHE_KEY = "Prompt:main";
    private static final String PROMPT_PREPARE_CACHE_KEY = "Prompt:prepare";
    private static final String PROMPT_SYSTEM_SAMPLE = "Prompt:sample";
    /**
     * 测试专用
     */
    private static final String PROMPT_TMP_CACHE_KEY = "Prompt:temp";

    private static final Map<String, String> FILE_MAP = new HashMap<String, String>() {{
        put(PROMPT_CACHE_KEY, "/prompt.txt");
        put(PROMPT_PREPARE_CACHE_KEY, "/prepare.txt");
//        put(PROMPT_TMP_CACHE_KEY, "/temp.txt");
        put(PROMPT_SYSTEM_SAMPLE, "/system-sample.txt");

    }};

    private static String getOrginPrompt(String cacheName) {
        String prefix = null;
        try {
            prefix = FileUtil.readFile(FILE_MAP.get(cacheName));
            PsiCacheManager.add(cacheName, prefix);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prefix;
    }

    public static List<MoreConversation> getSystemConversation() {
        ArrayList<MoreConversation> list = new ArrayList<>();
        String prefix = getPrefix(PROMPT_SYSTEM_SAMPLE);
        // 解析多轮对话，目前最多两轮，过多浪费token
        String tag = "'''";
        int userStart = prefix.indexOf(tag);
        int userEnd = prefix.substring(userStart + 1).indexOf(tag);
        String userPart = prefix.substring(userStart, userStart + userEnd + tag.length() + 1).trim();

        int assitantStart = userStart + userEnd + tag.length() + 1;
        int assitantEnd = prefix.substring(assitantStart).indexOf(tag);
        String assistantPart = prefix.substring(assitantStart, assitantStart + assitantEnd).trim();

        list.add(new MoreConversation("user", userPart));
        list.add(new MoreConversation("assistant", assistantPart));

//        System.out.println(assitantStart + "-" + assitantEnd + " " + prefix.length());
        // 第二轮
        userStart = assitantStart + assitantEnd;
        userEnd = prefix.substring(userStart + 1).indexOf(tag);
        userPart = prefix.substring(userStart, userStart + userEnd + tag.length() + 1);


        assitantStart = userStart + userEnd + tag.length() + 1;
        assistantPart = prefix.substring(assitantStart + 1).trim();
        if (!userPart.trim().isEmpty() && !assistantPart.trim().isEmpty()) {
            list.add(new MoreConversation("user", userPart));
            list.add(new MoreConversation("assistant", assistantPart));
        }
        return list;
    }


    public static List<MoreConversation> getSystemConversationForPreHeat() {
        ArrayList<MoreConversation> list = new ArrayList<>();
        MoreConversation system = new MoreConversation("system", "Please Ignore context");
        MoreConversation user = new MoreConversation("user", MessageFormat.format("Before answer question, You should know that current system time is:{0}", getLocalDateTimeStr()));
        list.add(system);
        list.add(user);
        return list;
    }

    public static String getUserPrefix(boolean isTest) {
        if (isTest) {
            return PromptHelper.getUserPrefix(PROMPT_TMP_CACHE_KEY);
        }
        String prepare = PromptHelper.getUserPrefix(PROMPT_PREPARE_CACHE_KEY);
        String prompt = PromptHelper.getUserPrefix(PROMPT_CACHE_KEY);
        return prepare + prompt;

    }

    private static String getUserPrefix(String str) {
        return getPrefix(str);
    }

    private static String getPrefix(String cacheKey) {
        Object o = PsiCacheManager.get(cacheKey);
        String obj;
        if (Objects.isNull(o)) {
            obj = getOrginPrompt(cacheKey);
        } else {
            obj = ((String) o);
        }
        if (PROMPT_PREPARE_CACHE_KEY.equals(cacheKey)) {
            obj = obj.replace("{current_time}", getLocalDateTimeStr());
        }
        return obj;
    }

    private static String getLocalDateTimeStr() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return dateTimeFormatter.format(LocalDateTime.now());
    }


    public static void main(String[] args) {
        System.out.println(getSystemConversation());
    }
}
