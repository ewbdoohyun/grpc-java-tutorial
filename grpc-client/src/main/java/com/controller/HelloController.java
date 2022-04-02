package com.controller;

import com.service.HelloService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HelloController {

    private final HelloService service;


    @GetMapping("/hello")
    public String greeting(@RequestParam String name) {
        return service.sendMessage(name);
    }
}
