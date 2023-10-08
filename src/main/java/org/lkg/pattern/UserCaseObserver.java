package org.lkg.pattern;

/**
 * @description: 用例观察者
 * @author: 李开广
 * @date: 2023/5/6 5:01 PM
 */
public interface UserCaseObserver {
    void update(String text, Integer type);
}
