package com.mindex.challenge.service;

import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.exceptions.NotFoundException;

public interface EmployeeService {
    /**
     * Creates the given employee, implementations are responsible for assigning employeeId
     *
     * @param employee the Employee to create
     * @return the created Employee, with employeeId set
     */
    Employee create(Employee employee);

    /**
     * Finds an Employee by the given id
     *
     * @param id the id of the Employee to find
     * @return the Employee found with id
     * @throws NotFoundException if no employees could be found with id
     */
    Employee read(String id) throws NotFoundException;

    /**
     * Updates the given Employee.
     *
     * @param employee the Employee to update
     * @return the updated Employee
     */
    Employee update(Employee employee);

    /**
     * Builds the ReportingStructure for the Employee with the given id
     *
     * @param id the id of the Employee to get the ReportingStructure for
     * @return a ReportingStructure for the Employee with id
     * @throws NotFoundException if the Employee with id, or any of its reports, could not be found
     */
    ReportingStructure getReportingStructure(String id) throws NotFoundException;
}
