package com.example.user_service.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.user_service.dto.Meta;
import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.dto.role.CreateRoleDTO;
import com.example.user_service.model.Role;
import com.example.user_service.repository.RoleRepository;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    public RoleService(RoleRepository roleRepository,
            PermissionService permissionService) {
        this.roleRepository = roleRepository;
        this.permissionService = permissionService;
    }

    public Role getById(Long id) {
        return roleRepository.findById(id)
                .orElse(null);
    }

    public PaginationDTO getAllWithFilters(Boolean isActive, String keyword, Pageable pageable) {
        Page<Role> pageRole = this.roleRepository.findByFilters(isActive, keyword, pageable);
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pageRole.getNumber() + 1);
        mt.setPageSize(pageRole.getSize());
        mt.setPages(pageRole.getTotalPages());
        mt.setTotal(pageRole.getTotalElements());
        rs.setMeta(mt);
        rs.setResult(pageRole.getContent());
        return rs;
    }

    public Role create(CreateRoleDTO createRoleDTO) {
        Role role = new Role();
        role.setName(createRoleDTO.getName());
        role.setDescription(createRoleDTO.getDescription());
        if (createRoleDTO.getIsActive() != null) {
            role.set_active(createRoleDTO.getIsActive());
        } else {
            role.set_active(true);
        }
        role.setPermissions(new HashSet<>(permissionService.findByIds(createRoleDTO.getPermissionIds())));

        return roleRepository.save(role);
    }

    public Role update(Long id, CreateRoleDTO createRoleDTO) {
        Role role = this.getById(id);
        role.setName(createRoleDTO.getName());
        role.setDescription(createRoleDTO.getDescription());
        if (createRoleDTO.getIsActive() != null) {
            role.set_active(createRoleDTO.getIsActive());
        }
        if (createRoleDTO.getPermissionIds() != null) {
            role.setPermissions(new HashSet<>(permissionService.findByIds(createRoleDTO.getPermissionIds())));
        }
        return roleRepository.save(role);
    }

    public void delete(Long id) {

        roleRepository.deleteById(id);
    }

    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }

    public List<Role> getByIds(List<Long> ids) {
        return this.roleRepository.findAllById(ids);
    }
}
