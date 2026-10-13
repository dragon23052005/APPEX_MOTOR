package com.apexmotors.service;

import com.apexmotors.dto.ContactForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    public String submit(ContactForm form) {
        String reference = "APX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Demo enquiry received: reference={}, interest={}, email={}",
            reference, sanitize(form.getInterest()), sanitize(form.getEmail()));
        return reference;
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]", "");
    }
}
