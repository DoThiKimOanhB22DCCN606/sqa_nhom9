package com.example.user_service.service;

import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.user_service.dto.Meta;
import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.model.Permission;
import com.example.user_service.model.User;
import com.example.user_service.repository.PermissionRepository;
import com.example.user_service.repository.UserRepository;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public PermissionService(PermissionRepository permissionRepository, UserRepository userRepository,
            CacheManager cacheManager) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    public boolean isPermissionExistsByName(String name) {
        return permissionRepository.existsByName(name);
    }

    public Permission findById(Long id) {
        return permissionRepository.findById(id).orElse(null);
    }

    public Permission create(Permission p) {
        if (!p.isActive()) {
            p.setActive(true);
        }
        return permissionRepository.save(p);
    }

    public Permission update(Long id, Permission p) {
        Permission permission = findById(id);

        if (permission != null) {
            permission.setName(p.getName());
            permission.setActive(p.isActive());
            return permissionRepository.save(permission);
        }
        return null;
    }

    public PaginationDTO getAllWithFilters(Boolean isActive, String keyword, Pageable pageable) {
        Page<Permission> page = this.permissionRepository.findByFilters(isActive, keyword, pageable);
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(page.getNumber() + 1);
        mt.setPageSize(page.getSize());
        mt.setPages(page.getTotalPages());
        mt.setTotal(page.getTotalElements());
        rs.setMeta(mt);
        rs.setResult(page.getContent());
        return rs;
    }

    public List<Permission> getAllWithFiltersNoPage(Boolean isActive, String keyword) {
        List<Permission> page = this.permissionRepository.findByFiltersNoPage(isActive, keyword);
        return page;
    }

    @Cacheable(cacheNames = "permCheck", key = "#userId + ':' + #name")
    public boolean check(String name, Long userId) {
        if (userId == null) {
            return false;
        }

        User user = this.userRepository.findById(userId).orElse(null);
        if (user == null || user.getRole() == null) {
            return false;
        }

        boolean allowed = permissionRepository.existsByRoles_IdAndNameAndActiveTrue(
                user.getRole().getId(), name);
        return allowed;
    }

    public void delete(Long id) {

        Permission permission = permissionRepository.findById(id).orElse(null);
        if (permission != null) {
            permission.getRoles().forEach(role -> role.getPermissions().remove(permission));
        }
        this.permissionRepository.delete(permission);
    }

    public List<Permission> findByIds(List<Long> ids) {
        return permissionRepository.findByIdIn(ids);
    }

    // public List<Boolean> batchCheck(BatchPermissionCheckRequest request) {
    // return request.getChecks().stream()
    // .map(item -> this.check(item.getName()))
    // .collect(Collectors.toList());
    // }

    // public String getCacheKey(String name) {
    // Long userId = getCurrentUserId();
    // return (userId == null ? "anon" : String.valueOf(userId)) + ":" + name;
    // }

    /**
     * Evict cache for all users with a specific role
     * This is called when role permissions are updated
     * Note: Currently clears all cache entries since Caffeine doesn't support
     * pattern matching.
     * This is acceptable because cache TTL is short (10s) and ensures realtime
     * updates.
     */
    public void evictCacheForRole(Long roleId) {
        Cache cache = cacheManager.getCache("permCheck");
        if (cache != null) {
            // Evict all cache entries to ensure realtime permission updates
            // Since we can't pattern match cache keys, we clear all entries
            // This is acceptable because cache TTL is short (10s)
            cache.clear();
        }
    }
}
