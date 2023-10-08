package org.lkg.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.lkg.dos.MoreConversation;
import org.lkg.ui.CaseWindow;
import org.lkg.util.NotificationUtil;
import org.lkg.util.PromptHelper;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.logging.Logger;
//import java.util.logging.Logger;

/**
 * @author likaiguang
 * @date 2023/4/23 1:06 下午
 */

public class RequestService {

    private static final Logger log = Logger.getLogger(RequestService.class.getSimpleName());
    @Deprecated
    private static final String key = "";

    //已过期
    @Deprecated
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private static final String STOP = "finish_reason";

    /**
     * https
     */
    private static final String PUBLIC_URL = "";

    /**
     * http方式
     */
    private static final String HTTP_PUBLIC_URL = "";

    /**
     * V1.0
     *
     * @param req
     * @return
     */
    public static String submitWithPublic(String req) {
        RestTemplate restTemplate = new RestTemplate();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(HTTP_PUBLIC_URL).queryParam("message", req);
        String forEntity = restTemplate.getForObject(builder.build().toUri(), String.class);
        if (forEntity != null) {
            JSONObject jsonObject = JSONObject.parseObject(forEntity);
            return jsonObject.getString("response");
        }
        return null;
    }

    /**
     * V2.0
     *
     * @param moreConversations
     * @return
     */
    public static String submitWithPublicV2(Object moreConversations, boolean isPreheat) {
        RestTemplate restTemplate = new RestTemplate(new HttpsClientRequestFactory());

        restTemplate.getMessageConverters().add(new MyHttpMessageConverter());

        Map<String, Object> mapForV2 = getMapForObject(moreConversations);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(mapForV2);
        try {
            ResponseEntity<JSONObject> forEntity = restTemplate.postForEntity(PUBLIC_URL, httpEntity, JSONObject.class);
            if (forEntity != null && forEntity.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonObject = forEntity.getBody();
                log.info("请求结果" + jsonObject.toJSONString());
                String err = jsonObject.getString("error");
                if (StringUtils.hasText(err)) {
                    return jsonObject.getString("msg");
                }
                return jsonObject.getString("response");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!isPreheat) {
                NotificationUtil.error("网络异常请稍后再试");
            } else {
                log.warning("原始请求:" + httpEntity.toString());
            }
            // TODO 收集信息，存入文档

        }

        return null;
    }

    public static String submitWithPublicV2(String user) {

        ArrayList<MoreConversation> list = new ArrayList<>();
        List<MoreConversation> preConversation = PromptHelper.getSystemConversation();
        if (CaseWindow.isFour()) {
            MoreConversation system = new MoreConversation("system", PromptHelper.getUserPrefix(false));
            list.add(system);
            list.addAll(preConversation);
            list.add(new MoreConversation("user", user));
            return submitWithPublicV2(list, false);
        } else {
            MoreConversation userConversation = preConversation.get(0);
            MoreConversation assist = preConversation.get(1);
            StringBuilder sb = new StringBuilder(PromptHelper.getUserPrefix(false));
            sb.append("\n下面是一个示例可供参考，注意后续的回答中不能包含这个样例.\n")
                    .append(userConversation.getContent()).append("\n")
                    .append(assist.getContent()).append("\n\n\n如下是用户提供的文本，请根据上下文提供测试用例:\n")
                    .append(user);
//            list.add(new MoreConversation("system", sb.toString()));
//            list.add(new MoreConversation("user", "\n\n\n如下是用户提供的文本，请根据上下文提供测试用例:\n" + user));
            return submitWithPublicV2(sb.toString(), false);
        }


    }


    @Deprecated
    private static JSONObject submit(ArrayList<MoreConversation> moreConversations) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHost proxy = new HttpHost("127.0.0.1", 7890);
        HttpClient httpClient = HttpClientBuilder.create().setRoutePlanner(new DefaultProxyRoutePlanner(proxy)).build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(60_000);
        requestFactory.setConnectTimeout(60_000);
        requestFactory.setConnectionRequestTimeout(30_000);


        restTemplate.setRequestFactory(requestFactory);


        //创建请求头
        HttpHeaders httpHeaders = new HttpHeaders();
        //此处相当于在Authorization里头添加Bear token参数信息
        httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + key);
        //此处相当于在header里头添加content-type等参数
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        Map<String, Object> map = getMap(moreConversations);
        //创建请求体并添加数据
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(map, httpHeaders);
        //此处三个参数分别是请求地址、请求体以及返回参数类型
        ResponseEntity<JSONObject> forEntity = restTemplate.postForEntity(CHAT_URL, httpEntity, JSONObject.class);
        if (forEntity.getStatusCode() == HttpStatus.OK) {
            JSONObject body = forEntity.getBody();
            Assert.notNull(body, "返回值为空");
            JSONArray choices = body.getJSONArray("choices");
            return choices.getJSONObject(0);
        }
        return null;
    }


    @Deprecated
    public static String submit(String originInput) {
        log.info("请求：" + originInput);
        ArrayList<MoreConversation> moreConversations = new ArrayList<>();
        moreConversations.add(new MoreConversation("user", originInput));
        //
        JSONObject data = submit(moreConversations);
        if (Objects.isNull(data)) {
            NotificationUtil.warning("插件调用网络异常，请稍后再试");
            return null;
        }
        log.info("原始返回数据" + data);
//        MoreConversation moreConversation = new MoreConversation("user", originInput);
//        moreConversations.add(moreConversation);
        int count = 2;
        getConversation(moreConversations, data);
        while (!isEnd(data) && --count >= 0) {
//            log.info("again:" + moreConversation);
            data = submit(moreConversations);
            if (Objects.isNull(data)) {
                log.info("二次请求异常");
                break;
            }
            getConversation(moreConversations, data);
        }
        if (!isEnd(data)) {
            NotificationUtil.warning("文本回答超出限制，目前不具备太深的记忆能力，请缩小范围再试");
        }
        StringBuilder sb = new StringBuilder();
        moreConversations.stream().filter(ref -> ref.getRole().equals("assistant")).forEach(ref -> sb.append(ref.getContent()));
        return filter(sb.toString());
    }

    private static void getConversation(ArrayList<MoreConversation> moreConversations, JSONObject data) {
        String current = data.getJSONObject("message").getString("content");
        moreConversations.add(new MoreConversation("assistant", current));
        moreConversations.add(new MoreConversation("user", "请继续"));
    }

    private static boolean isEnd(JSONObject data) {
        return "stop".equals(data.getString(STOP));
    }

    private static String filter(String string) {
        return string.replaceAll("\n\n", "\n");
    }


    private static Map<String, Object> getMap(ArrayList<MoreConversation> moreConversations) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("model", "gpt-3.5-turbo-0613"); //gpt-3.5-turbo-0613  gpt-3.5-turbo
        // 值越高 生成的文本重复词汇少
        map.put("frequency_penalty", 2);
        // 值越高，内容越丰富
//        map.put("presence_penalty", 2);
        // 值越小，回复越稳定
        map.put("temperature", 0);
        if (Objects.isNull(moreConversations)) {
            moreConversations = new ArrayList<MoreConversation>() {{
                add(new MoreConversation("user", "hello chat gpt!"));
            }};
        }
        map.put("messages", moreConversations);
        return map;
    }

    private static Map<String, Object> getMapForObject(Object obj) {
        HashMap<String, Object> map = buildBaseGptControlParams();
        if (obj instanceof List) {
            map.put("message", JSONObject.toJSONString(obj));
        } else {
            map.put("message", obj.toString());
        }
        if ( CaseWindow.isFour()) {
            log.info("选中gpt-4");
            map.put("gpt_4", 1);
        } else {
            log.info("选中gpt3.5");
        }
        return map;
    }


    private static HashMap<String, Object> buildBaseGptControlParams() {
        HashMap<String, Object> map = new HashMap<>();
        // 3.5 采用官方默认模型
        if (!CaseWindow.isFour()) {
            return map;
        }
        // 实验数据表面 改参数会影响prompt的规范化模版，因为其本意就是灵活，因此不适合规范化场景使用
//        map.put("frequency_penalty", 2);
//      越高，输出越稳定
        map.put("presence_penalty", 2);
        // 值越小，回复越稳定
        map.put("top_p", 0);
        return map;
    }

    /**
     * 自定义消息转换器
     */
    public static class MyHttpMessageConverter extends MappingJackson2HttpMessageConverter {
        MyHttpMessageConverter() {
            List<MediaType> mediaTypes = new ArrayList<>();
            mediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
            setSupportedMediaTypes(mediaTypes);
        }
    }

    public static void main(String[] args) {

        log.info(submitWithPublicV2("你好gpt"));
    }


}
