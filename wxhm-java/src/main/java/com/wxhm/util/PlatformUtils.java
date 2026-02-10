package com.wxhm.util;

/**
 * 根据 User-Agent 解析平台
 */
public final class PlatformUtils {

    private PlatformUtils() {
    }

    public static String parsePlatform(String userAgent) {
        if (userAgent == null) {
            return "Other";
        }
        String ua = userAgent.toLowerCase();
        // 移动设备
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipad")) {
            if (ua.contains("iphone") || ua.contains("ipad")) {
                return "iOS";
            }
            return "Android";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (userAgent.contains("Macintosh")) {
            return "Mac";
        }
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return "Other";
    }
}
