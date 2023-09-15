package com.mindex.challenge.service;

import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.exceptions.EmployeeNotFoundException;

public interface EmployeeService {
    Employee create(Employee employee);
    Employee read(String id) throws EmployeeNotFoundException;
    Employee update(Employee employee);
    ReportingStructure getReportingStructure(String id) throws EmployeeNotFoundException;
}
