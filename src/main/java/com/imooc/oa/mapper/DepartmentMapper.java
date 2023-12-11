package com.imooc.oa.mapper;

import com.imooc.oa.entity.Department;

public interface DepartmentMapper {
    Department selectById(Long departmentId);
}