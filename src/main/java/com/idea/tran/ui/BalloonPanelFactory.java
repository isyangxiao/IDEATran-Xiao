/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 */

package com.idea.tran.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 气泡提示工厂类
 *
 * 职责:
 * - 创建并显示 IDEA 气泡通知
 * - 提供不同级别的提示（信息、警告、错误）
 *
 * @author xiao
 */
public class BalloonPanelFactory {

    /**
     * 创建气泡通知
     *
     * @param title   标题
     * @param content 内容
     * @param type    通知类型
     * @param project 项目（可为空）
     * @return 通知对象
     */
    public static Notification createBalloon(@NotNull String title, @NotNull String content, NotificationType type, Project project) {
        Notification notification = new Notification(
            "IdeaTran",
            title,
            content,
            type
        );

        if (project != null) {
            Notifications.Bus.notify(notification, project);
        } else {
            notification.notify(null);
        }

        return notification;
    }

    /**
     * 显示信息提示
     *
     * @param title   标题
     * @param content 内容
     * @param project 项目
     */
    public static void showInfo(@NotNull String title, @NotNull String content, Project project) {
        createBalloon(title, content, NotificationType.INFORMATION, project);
    }

    /**
     * 显示警告提示
     *
     * @param title   标题
     * @param content 内容
     * @param project 项目
     */
    public static void showWarning(@NotNull String title, @NotNull String content, Project project) {
        createBalloon(title, content, NotificationType.WARNING, project);
    }

    /**
     * 显示错误提示
     *
     * @param title   标题
     * @param content 内容
     * @param project 项目
     */
    public static void showError(@NotNull String title, @NotNull String content, Project project) {
        createBalloon(title, content, NotificationType.ERROR, project);
    }
}
