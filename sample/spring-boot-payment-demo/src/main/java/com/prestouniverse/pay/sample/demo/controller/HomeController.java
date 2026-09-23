package com.prestouniverse.pay.sample.demo.controller;

import com.prestouniverse.pay.sample.demo.config.AppProperties;
import com.prestouniverse.pay.sample.demo.config.PrestoPayProperties;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AppProperties appProperties;
    private final PrestoPayProperties prestoPayProperties;
    private final PaymentActivityStore activityStore;

    public HomeController(AppProperties appProperties, PrestoPayProperties prestoPayProperties,
            PaymentActivityStore activityStore) {
        this.appProperties = appProperties;
        this.prestoPayProperties = prestoPayProperties;
        this.activityStore = activityStore;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("notifyUrl", appProperties.notifyUrl());
        model.addAttribute("redirectUrl", appProperties.redirectUrl());
        model.addAttribute("prestoMid", prestoPayProperties.getMid());
        model.addAttribute("prestoMrn", prestoPayProperties.getMrn());
        model.addAttribute("prestoEnv", prestoPayProperties.getEnvironment());
        model.addAttribute("recentWebhooks", activityStore.recentWebhooks());
        return "index";
    }
}
