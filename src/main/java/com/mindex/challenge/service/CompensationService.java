package com.mindex.challenge.service;

import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.exceptions.EmployeeNotFoundException;

public interface CompensationService {

    /**
     * Creates a new Compensation entity
     * @param compensation the Compensation to create
     * @return the created Compensation
     */
    Compensation create(Compensation compensation);

    /**
     * Finds the most recent Compensation for a given employeeId
     * @param employeeId the id of the employee to find a Compensation for
     * @return the most recent Compensation for the employee with employeeId
     * @throws EmployeeNotFoundException if no Compensation is found for employeeId
     */
    Compensation findByEmployeeId(String employeeId) throws EmployeeNotFoundException;
}
