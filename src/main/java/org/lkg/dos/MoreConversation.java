package org.lkg.dos;

/**
 * @description: 多轮对话能力
 * @author: 李开广
 * @date: 2023/5/9 5:00 PM
 */
public class MoreConversation {

    private String role;
    private String content;


    public MoreConversation(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "MoreConversation{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
