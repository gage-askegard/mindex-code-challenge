package com.mindex.challenge.service.impl;

import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.exceptions.NotFoundException;
import com.mindex.challenge.service.EmployeeService;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Employee create(Employee employee) {
        LOG.debug("Creating employee [{}]", employee);

        employee.setEmployeeId(UUID.randomUUID().toString());
        employeeRepository.insert(employee);

        return employee;
    }

    @Override
    public Employee read(String id) throws NotFoundException {
        LOG.debug("Finding employee with id [{}]", id);

        Employee employee = employeeRepository.findByEmployeeId(id);

        if (employee == null) {
            throw new NotFoundException("Invalid employeeId: " + id);
        }

        return employee;
    }

    @Override
    public Employee update(Employee employee) {
        LOG.debug("Updating employee [{}]", employee);

        return employeeRepository.save(employee);
    }

    @Override
    public ReportingStructure getReportingStructure(String id) throws NotFoundException {
        LOG.debug("Finding reporting structure for employee with id [{}]", id);

        Employee employee = read(id);
        int numberOfReports = getNumberOfReports(employee);

        return new ReportingStructure(employee, numberOfReports);
    }

    /**
     * Recursively gets the number of reports for the given Employee by getting their direct reports and all the
     * report's reports
     *
     * @param employee the Employee to get the number of reports for
     * @return the total number of reports for the employee
     */
    private int getNumberOfReports(Employee employee) throws NotFoundException {
        List<Employee> directReports = ObjectUtils.defaultIfNull(employee.getDirectReports(), emptyList());
        int numberOfReports = directReports.size();
        for (Employee directReport : directReports) {
            numberOfReports += getNumberOfReports(read(directReport.getEmployeeId()));
        }

        return numberOfReports;
    }
}
