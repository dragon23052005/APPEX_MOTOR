package com.apexmotors.controller;

import com.apexmotors.dto.VehicleQuery;
import com.apexmotors.dto.VehicleQuery.SortOption;
import com.apexmotors.model.Vehicle;
import com.apexmotors.service.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;

@Controller
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/collection")
    public String collection(@RequestParam(defaultValue = "") String q,
                             @RequestParam(defaultValue = "") String manufacturer,
                             @RequestParam(defaultValue = "FEATURED") SortOption sort,
                             Model model) {
        VehicleQuery query = new VehicleQuery(q, manufacturer, sort);
        model.addAttribute("vehicles", vehicleService.search(query));
        model.addAttribute("manufacturers", vehicleService.getManufacturers());
        model.addAttribute("sortOptions", Arrays.asList(SortOption.values()));
        model.addAttribute("query", query);
        model.addAttribute("activePage", "collection");
        return "collection";
    }

    @GetMapping("/cars/{slug}")
    public String details(@PathVariable String slug, Model model) {
        Vehicle vehicle = vehicleService.getBySlug(slug);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("activePage", "collection");
        return "car-details";
    }
}
