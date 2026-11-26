package vn.haqua.shop.util;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import vn.haqua.shop.exception.ApiException;

/** Giới hạn tần suất theo khóa (cửa sổ trượt, trong bộ nhớ). Nhiều instance → chuyển sang Redis/bucket4j. */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public void check(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > window.toMillis()) {
                q.pollFirst();
            }
            if (q.size() >= maxRequests) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Thao tác quá nhanh, vui lòng thử lại sau 1 phút");
            }
            q.addLast(now);

        }
    }
}
