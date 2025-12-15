package com.kakarote.admin.oceanengine.auth;

import com.kakarote.admin.entity.PO.AdminMenu;
import com.kakarote.admin.service.IAdminMenuService;
import com.kakarote.core.common.SystemCodeEnum;
import com.kakarote.core.exception.CrmException;
import com.kakarote.core.servlet.ApplicationContextHolder;
import com.kakarote.core.utils.UserUtil;

import java.util.Collections;
import java.util.List;

/**
 * OceanEngine 接口权限校验入口。
 */
public final class OeAuthChecker {

    private OeAuthChecker() {
    }

    /**
     * 校验当前用户是否具备指定的接口权限码。
     *
     * @param permission 权限码
     */
    public static void checkPermission(String permission) {
        if (UserUtil.isAdmin()) {
            return;
        }
        Long userId = UserUtil.getUserId();
        IAdminMenuService adminMenuService = ApplicationContextHolder.getBean(IAdminMenuService.class);
        List<AdminMenu> authedMenus = adminMenuService.queryMenuList(userId);
        if (authedMenus == null) {
            authedMenus = Collections.emptyList();
        }
        boolean hasPermission = authedMenus.stream().anyMatch(menu -> permission.equals(menu.getRealmUrl()));
        if (!hasPermission) {
            throw new CrmException(SystemCodeEnum.SYSTEM_NO_AUTH);
        }
    }
}
