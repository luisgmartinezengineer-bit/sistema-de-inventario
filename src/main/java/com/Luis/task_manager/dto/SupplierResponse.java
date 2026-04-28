package com.Luis.task_manager.dto;

import com.Luis.task_manager.entity.Supplier;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SupplierResponse {
    private Long id;
    private String name;
    private String nit;
    private String contactName;
    private String email;
    private String phone;
    private String city;
    private String address;
    private Integer paymentTermsDays;
    private Integer leadTimeDays;
    private BigDecimal rating;
    private String notes;
    private boolean active;
    private int productCount;

    public static SupplierResponse from(Supplier s) {
        SupplierResponse r = new SupplierResponse();
        r.id = s.getId(); r.name = s.getName(); r.nit = s.getNit();
        r.contactName = s.getContactName(); r.email = s.getEmail();
        r.phone = s.getPhone(); r.city = s.getCity(); r.address = s.getAddress();
        r.paymentTermsDays = s.getPaymentTermsDays(); r.leadTimeDays = s.getLeadTimeDays();
        r.rating = s.getRating(); r.notes = s.getNotes(); r.active = s.isActive();
        return r;
    }
}
