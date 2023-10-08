package org.lkg.cache;

import java.lang.ref.SoftReference;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PsiCacheManager {

    private static final Logger log = Logger.getLogger(PsiCacheManager.class.getSimpleName());

    private static final long EXPIRED_TIMES = TimeUnit.MINUTES.toMillis(10);

    private static ConcurrentHashMap<String, SoftReference<PsiCache>> CACHE = new ConcurrentHashMap<>();

    public  PsiCacheManager() {
        Thread thread = new Thread(() -> {
            while(true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(EXPIRED_TIMES);
                    CACHE.entrySet().removeIf(ref -> Optional.ofNullable(ref.getValue()).map(SoftReference::get).map(PsiCache::isExpired).orElse(false));
                } catch (Exception err) {
                    err.printStackTrace();
                    log.warning(err.getMessage());
                }
            }

        });

        thread.setDaemon(true);
        thread.start();
    }

    public static void add(String key, Object value) {
        add(key, value, EXPIRED_TIMES);
    }

    public static void add(String key, Object value, long expiredTimeMills) {
        if (Objects.isNull(key)) {
            return;
        }
        if (Objects.isNull(value)) {
            CACHE.remove(key);
            return;
        }
        CACHE.put(key, new SoftReference<>(new PsiCache(value, System.currentTimeMillis() + expiredTimeMills)));
    }

    public static Object get(String key) {
        if (Objects.isNull(key)) {
            return null;
        }
        SoftReference<PsiCache> psiCacheSoftReference = CACHE.get(key);
        if (Objects.isNull(psiCacheSoftReference)) {
            return null;
        }
        PsiCache psiCache = psiCacheSoftReference.get();
        if (Objects.isNull(psiCache) || psiCache.isExpired()){
            CACHE.remove(key);
            return null;
        }
        return psiCache.getValue();
    }


    public void clear() {
        CACHE.clear();
    }
}
