package org.lkg.cache;

public class PsiCache {

    private Object value;

    private long expiredTime;

    public PsiCache(Object value, long expiredTime) {
        this.value = value;
        this.expiredTime = expiredTime;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(long expiredTime) {
        this.expiredTime = expiredTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - expiredTime > 0;
    }
}
