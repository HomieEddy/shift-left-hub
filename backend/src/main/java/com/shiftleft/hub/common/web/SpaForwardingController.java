package com.shiftleft.hub.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

    /**
     * Forwards all frontend routes to the Angular index.html for SPA routing.
     *
     * @return the forward path to index.html
     */
    @GetMapping(value = {
        "/chat/**", "/knowledge-base/**", "/tickets/**",
        "/agent/**", "/admin/**", "/login", "/register",
        "/landing", "/"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
