package com.apexmotors.controller;

import com.apexmotors.dto.ContactForm;
import com.apexmotors.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    private final ContactService contactService;

    public PageController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("activePage", "about");
        return "about";
    }

    @GetMapping("/contact")
    public String contact(@RequestParam(required = false) String interest, Model model) {
        if (!model.containsAttribute("contactForm")) {
            ContactForm form = new ContactForm();
            if (interest != null && !interest.isBlank()) {
                form.setInterest("Private viewing");
                form.setMessage("I would like to arrange a private viewing of the " + interest + ".");
            }
            model.addAttribute("contactForm", form);
        }
        model.addAttribute("activePage", "contact");
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContact(@Valid @ModelAttribute("contactForm") ContactForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!form.isPrivacyAccepted()) {
            bindingResult.rejectValue("privacyAccepted", "required", "Please accept the privacy notice.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "contact");
            return "contact";
        }

        String reference = contactService.submit(form);
        redirectAttributes.addFlashAttribute("successMessage",
            "Thank you, " + form.getName().trim() + ". Your private consultation request has been received.");
        redirectAttributes.addFlashAttribute("reference", reference);
        return "redirect:/contact";
    }
}
