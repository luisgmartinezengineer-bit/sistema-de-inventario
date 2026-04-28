package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.Customer;
import lombok.Data;

@Data
public class CustomerResponse {
    private Long id;
    private String name;
    private String document;
    private String email;
    private String phone;
    private String address;
    private boolean active;

    public static CustomerResponse from(Customer c) {
        CustomerResponse r = new CustomerResponse();
        r.id = c.getId();
        r.name = c.getName();
        r.document = c.getDocument();
        r.email = c.getEmail();
        r.phone = c.getPhone();
        r.address = c.getAddress();
        r.active = c.isActive();
        return r;
    }
}
