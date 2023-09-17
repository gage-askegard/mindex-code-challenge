package com.mindex.challenge.service;

import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.exceptions.EmployeeNotFoundException;

public interface CompensationService {
    Compensation create(Compensation compensation);
    Compensation findByEmployeeId(String employeeId) throws EmployeeNotFoundException;
}
