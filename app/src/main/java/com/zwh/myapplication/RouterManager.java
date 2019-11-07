package com.zwh.myapplication;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.Map;

import androidx.annotation.NonNull;


/**
 * 路由加载管理器
 */
public final class RouterManager {

    // 路由详细路径
    private String path;
    private static RouterManager instance;
    // APT生成的路由源文件后缀名
    private static final String GROUP_FILE_PREFIX_NAME = "$$ARouter";
    private Map<String, Class> pathCache;

    // 单例方式，全局唯一
    public static RouterManager getInstance() {
        if (instance == null) {
            synchronized (RouterManager.class) {
                if (instance == null) {
                    instance = new RouterManager();
                }
            }
        }
        return instance;
    }

    private RouterManager() {

    }

    public RouterManager build(String path) {
        if (TextUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new IllegalArgumentException("未按规范配置，如：/app/MainActivity");
        }
        this.path = path;

        return this;
    }

    /**
     * 开始跳转
     *
     * @param context       上下文
     * @return 普通跳转可以忽略，用于跨模块CALL接口
     */
    void navigation(@NonNull Context context) {

            try {
                if(pathCache == null){
                    String classname = context.getPackageName()+".ARouter$$Path";
                    Class<?> arouterPath = Class.forName(classname);
                    Method method = arouterPath.getMethod("loadPath");
                    pathCache = (Map<String, Class>) method.invoke(null);
                }
                Intent intent = new Intent(context, pathCache.get(path));
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
