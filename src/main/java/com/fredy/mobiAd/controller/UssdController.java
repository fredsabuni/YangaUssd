package com.fredy.mobiAd.controller;

import com.fredy.mobiAd.service.UssdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UssdController {
    @Autowired
    private UssdService ussdService;

    @RequestMapping(value = "/", method = {RequestMethod.GET, RequestMethod.POST})
    public String ussdCallback(@RequestParam Map<String, String> requestParams) {
        String sessionId = requestParams.get("sessionId");
        String phoneNumber = requestParams.get("phoneNumber");
        String text = requestParams.get("text");
        return ussdService.handleUssdRequest(sessionId,phoneNumber,text);
    }
}
