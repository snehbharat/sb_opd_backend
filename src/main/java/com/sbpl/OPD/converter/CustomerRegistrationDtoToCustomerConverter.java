package com.sbpl.OPD.converter;

import com.sbpl.OPD.dto.customer.request.CustomerRegistrationDto;
import com.sbpl.OPD.model.Customer;
import org.springframework.stereotype.Component;

/**
 * Converts CustomerRegistrationDto to Customer entity.
 *
 * @author Rahul Kumar
 */
@Component
public class CustomerRegistrationDtoToCustomerConverter {

    public Customer convert(CustomerRegistrationDto dto) {

        Customer customer = new Customer();
        customer.setPrefix(dto.getPrefix());
        customer.setFirstName(dto.getFirstName());
        customer.setLastName(dto.getLastName());
        customer.setPhoneNumber(dto.getPhoneNumber());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());

        return customer;
    }
}
