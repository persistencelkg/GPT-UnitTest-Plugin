package org.lkg.pattern;

import java.util.ArrayList;
import java.util.Objects;

/**
 * @description: 用例主题
 * @author: 李开广
 * @date: 2023/5/6 4:58 PM
 */
public class UseCaseSubject {

    private static final UseCaseSubject INSTANCE = new UseCaseSubject();
    // 观察者集合
    public final ArrayList<UserCaseObserver> CASE_OBSERVERS = new ArrayList<UserCaseObserver>();

    private String selectedText;
    private Integer type;

    public void addObserver(UserCaseObserver observer) {
        CASE_OBSERVERS.add(observer);
    }

    public void notifyObservers() {
        for (UserCaseObserver caseObserver : CASE_OBSERVERS) {
            caseObserver.update(selectedText, type);
        }
    }

    public void onChange(String selectedText, Integer type) {
        this.selectedText = selectedText;
        this.type = type;
        notifyObservers();
    }

    public static UseCaseSubject getInstance() {
        if (Objects.isNull(INSTANCE)) {
            return new UseCaseSubject();
        }
        return INSTANCE;
    }
}
