package com.imooc.oa.mapper;

import com.imooc.oa.entity.Employee;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface EmployeeMapper {
    public Employee selectById(Long employeeId);

    public List<Employee> selectByParams(Map params);
}
