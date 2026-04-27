package com.shortify.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController("/new")
public class NewController {
    @GetMapping()
    public ResponseEntity<Void> redirect() {

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
