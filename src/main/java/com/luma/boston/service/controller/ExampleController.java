package com.luma.boston.service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleController {
    @GetMapping("/status")
    public ResponseEntity getStatus(){
      return ResponseEntity.ok().build();
    }
}
