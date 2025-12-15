package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.entity.PO.AdminDept;
import com.kakarote.admin.entity.PO.AdminMenu;
import com.kakarote.admin.entity.PO.AdminUser;
import com.kakarote.admin.oceanengine.entity.PO.OeUserMapping;
import com.kakarote.admin.oceanengine.mapper.OeUserMappingMapper;
import com.kakarote.admin.oceanengine.service.OeDashboardScopeService;
import com.kakarote.admin.service.IAdminDeptService;
import com.kakarote.admin.service.IAdminMenuService;
import com.kakarote.admin.service.IAdminUserService;
import com.kakarote.core.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 解析 OceanEngine 看板数据范围。
 */
@Service
@RequiredArgsConstructor
public class OeDashboardScopeServiceImpl implements OeDashboardScopeService {

    private static final String PERM_DASHBOARD_DEPT = "oceanengine:dashboard:dept";
    private static final String PERM_DASHBOARD_TEAM = "oceanengine:dashboard:team";

    private final IAdminMenuService adminMenuService;
    private final IAdminDeptService adminDeptService;
    private final IAdminUserService adminUserService;
    private final OeUserMappingMapper userMappingMapper;

    @Override
    public List<Long> resolveEffectiveSaleUserIds(Long requestSaleUserId) {
        if (UserUtil.isAdmin()) {
            if (requestSaleUserId == null) {
                return null;
            }
            List<Long> list = new ArrayList<>();
            list.add(requestSaleUserId);
            return list;
        }
        Long currentUserId = UserUtil.getUserId();
        if (currentUserId == null) {
            return new ArrayList<>();
        }

        List<AdminMenu> authedMenus = adminMenuService.queryMenuList(currentUserId);
        boolean hasDeptScope = hasScopePermission(authedMenus, PERM_DASHBOARD_DEPT);
        boolean hasTeamScope = hasScopePermission(authedMenus, PERM_DASHBOARD_TEAM);

        if (!hasDeptScope && !hasTeamScope) {
            return mapUsersToSaleIds(java.util.Collections.singletonList(currentUserId));
        }

        List<Integer> rootDeptIds = findOwnedDeptIds(currentUserId);
        if (rootDeptIds.isEmpty()) {
            AdminUser adminUser = adminUserService.getById(currentUserId);
            if (adminUser != null && adminUser.getDeptId() != null) {
                rootDeptIds.add(adminUser.getDeptId());
            }
        }
        if (rootDeptIds.isEmpty()) {
            return mapUsersToSaleIds(java.util.Collections.singletonList(currentUserId));
        }

        Set<Integer> deptIds = collectDeptTree(rootDeptIds);
        if (deptIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<AdminUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.select(AdminUser::getUserId)
                .in(AdminUser::getDeptId, deptIds)
                .eq(AdminUser::getIsDel, 0)
                .ne(AdminUser::getStatus, 0);
        List<Long> adminUserIds = adminUserService.listObjs(userWrapper, o -> (Long) o);
        if (adminUserIds == null || adminUserIds.isEmpty()) {
            return new ArrayList<>();
        }

        return mapUsersToSaleIds(adminUserIds);
    }

    private boolean hasScopePermission(List<AdminMenu> menus, String permission) {
        if (menus == null || menus.isEmpty()) {
            return false;
        }
        return menus.stream().anyMatch(menu ->
                permission.equals(menu.getRealm()) || permission.equals(menu.getRealmUrl()));
    }

    private List<Integer> findOwnedDeptIds(Long currentUserId) {
        List<AdminDept> all = adminDeptService.list();
        if (all == null || all.isEmpty()) {
            return new ArrayList<>();
        }
        return all.stream()
                .filter(dept -> Objects.equals(dept.getOwnerUserId(), currentUserId))
                .map(AdminDept::getDeptId)
                .collect(Collectors.toList());
    }

    private Set<Integer> collectDeptTree(List<Integer> rootIds) {
        List<AdminDept> all = adminDeptService.list();
        if (all == null || all.isEmpty()) {
            return new HashSet<>();
        }
        Map<Integer, List<Integer>> childrenMap = new HashMap<>();
        for (AdminDept dept : all) {
            Integer pid = dept.getPid();
            childrenMap.computeIfAbsent(pid == null ? 0 : pid, k -> new ArrayList<>())
                    .add(dept.getDeptId());
        }
        Set<Integer> result = new HashSet<>();
        Queue<Integer> queue = new ArrayDeque<>(rootIds);
        while (!queue.isEmpty()) {
            Integer current = queue.poll();
            if (current == null || result.contains(current)) {
                continue;
            }
            result.add(current);
            List<Integer> children = childrenMap.get(current);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return result;
    }

    private List<Long> mapUsersToSaleIds(List<Long> adminUserIds) {
        if (adminUserIds == null || adminUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<OeUserMapping> mappings = userMappingMapper.selectList(new LambdaQueryWrapper<OeUserMapping>()
                .in(OeUserMapping::getAdminUserId, adminUserIds));
        if (mappings == null || mappings.isEmpty()) {
            return new ArrayList<>();
        }
        return mappings.stream()
                .map(OeUserMapping::getOeSaleUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}
