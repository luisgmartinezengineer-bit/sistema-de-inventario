package com.Luis.task_manager.service;

import com.Luis.task_manager.dto.CustomerRequest;
import com.Luis.task_manager.dto.CustomerResponse;
import com.Luis.task_manager.entity.Customer;
import com.Luis.task_manager.exception.ResourceNotFoundException;
import com.Luis.task_manager.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(CustomerResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> search(String q) {
        return customerRepository.findByNameContainingIgnoreCaseAndActiveTrue(q).stream()
                .map(CustomerResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return CustomerResponse.from(getOrThrow(id));
    }

    public CustomerResponse create(CustomerRequest req) {
        Customer c = Customer.builder()
                .name(req.getName()).document(req.getDocument())
                .email(req.getEmail()).phone(req.getPhone())
                .address(req.getAddress()).active(true).build();
        return CustomerResponse.from(customerRepository.save(c));
    }

    public CustomerResponse update(Long id, CustomerRequest req) {
        Customer c = getOrThrow(id);
        c.setName(req.getName()); c.setDocument(req.getDocument());
        c.setEmail(req.getEmail()); c.setPhone(req.getPhone());
        c.setAddress(req.getAddress());
        return CustomerResponse.from(customerRepository.save(c));
    }

    public void delete(Long id) {
        Customer c = getOrThrow(id);
        c.setActive(false);
        customerRepository.save(c);
    }

    public Customer getOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }
}
