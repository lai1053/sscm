package com.kakarote.admin.oceanengine.auth;

/**
 * OceanEngine 接口级权限码定义。
 */
public final class OeAuthConst {

    private OeAuthConst() {
    }

    /**
     * D+1 日报同步（ADS / 千川）。
     */
    public static final String OE_SYNC_DAILY = "oe:sync:daily";

    /**
     * 日报回填 / 运维手工同步。
     */
    public static final String OE_BACKFILL_DAILY = "oe:backfill:daily";

    /**
     * 老板一键同步入口。
     */
    public static final String OE_BOSS_SYNC = "oe:boss:sync";

    /**
     * 用户 ↔ 巨量销售身份绑定管理。
     */
    public static final String OE_USER_BIND = "oe:user:bind";
}
