package com.upc.common.utils;

/**
 * 用户上下文工具类
 * 用于在服务间传递用户信息
 */
public class UserContext {

    private static final ThreadLocal<Integer> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();

    public static void setCurrentUserId(Integer userId) {
        USER_ID.set(userId);
    }

    public static Integer getCurrentUserId() {
        return USER_ID.get();
    }

    public static void setCurrentUserName(String username) {
        USERNAME.set(username);
    }

    public static String getCurrentUserName() {
        return USERNAME.get();
    }

    public static void setCurrentUserRole(Integer role) {
        ROLE.set(role);
    }

    public static Integer getCurrentUserRole() {
        return ROLE.get();
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
        ROLE.remove();
    }
}
