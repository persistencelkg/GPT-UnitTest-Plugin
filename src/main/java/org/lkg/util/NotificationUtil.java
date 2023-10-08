package org.lkg.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * @description: 通知工具类
 * @author: 李开广
 * @date: 2023/5/5 8:30 PM
 */
public class NotificationUtil {

    private static final Logger log =  Logger.getLogger(NotificationUtil.class.getSimpleName());
    // 获取通知组管理器
//    private static final NotificationGroupManager manager = NotificationGroupManager.getInstance();

    // 获取注册的通知组
//    private static final NotificationGroup balloon = manager.getNotificationGroup("usecase.notification.balloon");

    private static final NotificationGroup balloon = NotificationGroup.balloonGroup("usecase.notification.balloon");


    public static void info(String msg) {
        Notification notification = balloon.createNotification(msg, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
        log.info(msg);
    }

    public static void warning(String msg) {
        Notification notification = balloon.createNotification(msg, NotificationType.WARNING);
        Notifications.Bus.notify(notification);
        log.warning(msg);
    }

    public static void warning(String msg, BiConsumer<String, Integer> consumer) {
        Notification notification = balloon.createNotification(msg, NotificationType.WARNING);
        Notifications.Bus.notify(notification);
        log.warning(msg);
        consumer.accept(null, null);
    }

    public static void error(String msg) {
        Notification notification = balloon.createNotification(msg, NotificationType.ERROR);
        Notifications.Bus.notify(notification);
        log.warning(msg);
    }

}
