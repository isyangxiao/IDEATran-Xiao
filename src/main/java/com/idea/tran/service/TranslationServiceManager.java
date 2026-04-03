/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 */

package com.idea.tran.service;

import lombok.extern.slf4j.Slf4j;

/**
 * 翻译服务单例管理器
 *
 * 职责:
 * - 管理 TranslationService 的生命周期
 * - 提供全局访问点
 * - 实现延迟初始化和线程安全
 *
 * @author xiao
 */
@Slf4j
public class TranslationServiceManager {

    /** 单例实例（volatile 保证可见性） */
    private static volatile TranslationService instance;

    /**
     * 获取翻译服务单例实例
     * 双重检查锁定实现延迟初始化和线程安全
     *
     * @return 翻译服务实例
     */
    public static TranslationService getInstance() {
        if (instance == null) {
            synchronized (TranslationServiceManager.class) {
                if (instance == null) {
                    try {
                        instance = new TranslationService();
                        log.info("翻译服务单例初始化完成");
                    } catch (Exception e) {
                        log.error("翻译服务单例初始化失败", e);
                        return null;
                    }
                }
            }
        }
        return instance;
    }

    /**
     * 关闭翻译服务
     * 终止 Python 子进程，释放资源
     */
    public static void shutdown() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
