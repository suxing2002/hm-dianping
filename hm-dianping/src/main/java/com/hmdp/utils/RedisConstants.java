package com.hmdp.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 60L;
    public static final String LOGIN_USER_KEY = "user:token:";
    public static final Long LOGIN_USER_TTL = 1800L;
    public static final Long CACHE_NULL_TTL = 2L;
    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 5L;
    public static final Long LOCK_VOUCHER_TTL = 5L;
    public static final String VOUCHER = "voucher:";
    public static final Long CACHE_SHOP_TYPE_TTL = 30L;
    public static final String CACHE_SHOP_TYPE_KEY = "cache:shop_type:";
    public static final Long LOCK_HOLDER_TTL = 10L;
    public static final Long CACHE_SHOP_LOGIC_EXPIRE = 30L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
    public static final String SECVOUCHER_STOCK = "secKillVoucher_stock:";
    public static final String SHOP_SECKILL_VOUCHER = "Shop:SecKillVoucher:";
    public static final String LOCK_SECKILL_VOUCHER_ORDER = "lock:secKill_voucher:order:";
    public static final String CACHE_BLOG_KEY = "cache:blog:";
    public static final long CACHE_BLOG_TTL = 5L;
    public static final String ZSET_BLOG_LIKE = "sortSet:blog:like:";
    public static final String BLOG_LIKE_DEFAULT = "";
    public static final double BLOG_LIKE_ZSET_START = 0;
    public static final long BLOG_LIKE_COUNT = 5;
    public static final long BLOG_LIKE_OFFSET = 0;
    public static final String SET_FOLLOW_KEY = "set:follow:";
    public static final String ZSET_BLOG_MAILBOX_KEY = "sortSet:mailBox:";
    public static final String GEO_SHOP_KEY = "geo:shop:";
    public static final double GEO_SHOP_RADIUS = 5000;
}
