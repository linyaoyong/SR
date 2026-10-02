package com.share.rental.common.redis;

public final class RedisKey {

    public static final String REFRESH_TOKEN = "sr:auth:refresh:";
    public static final String USER_BLACKLIST = "sr:auth:blacklist:";
    public static final String ITEM_CACHE = "sr:item:";
    public static final String APPLICATION_DUPLICATE = "sr:rental:application:duplicate:";
    public static final String TIME_LOCK = "sr:rental:time-lock:";
    public static final String MESSAGE_UNREAD = "sr:message:unread:";
    public static final String WEBSOCKET_ONLINE = "sr:message:online:";
    public static final String EVENT_DEDUP = "sr:event:dedup:";
    public static final String MQ_IDEMPOTENT = "sr:mq:idempotent:";
    public static final String SCHEDULER_LOCK = "sr:scheduler:lock:";

    private RedisKey() {
    }
}
