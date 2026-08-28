package org.example.common.util;

import java.util.HashMap;
import java.util.Map;

/**
 * ThreadLocal 工具类（统一管理用户上下文）
 */
public final class ThreadLocalUtil {

    private static final ThreadLocal<Map<String, Object>> TL = ThreadLocal.withInitial(HashMap::new);

    private ThreadLocalUtil() {
    }

    public static Map<String, Object> get() {
        return TL.get();
    }

    public static void set(Map<String, Object> value) {
        TL.set(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) TL.get().get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T set(String key, Object value) {
        return (T) TL.get().put(key, value);
    }

    public static void remove() {
        TL.remove();
    }
}
