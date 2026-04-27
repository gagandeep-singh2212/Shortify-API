package com.shortify.api.controller;


import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class VideoController {

    private final String VIDEO_PATH = "src/main/resources/videos/sample.mp4";

    @GetMapping("/video")
    public ResponseEntity<Resource> streamVideo(
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) throws IOException {

        File file = new File(VIDEO_PATH);
        long fileLength = file.length();

        long start = 0;
        long end = fileLength - 1;

        if (rangeHeader != null) {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            start = Long.parseLong(ranges[0]);

            if (ranges.length > 1) {
                end = Long.parseLong(ranges[1]);
            }
        }

        long contentLength = end - start + 1;

        FileInputStream inputStream = new FileInputStream(file);
        inputStream.skip(start);

        InputStreamResource resource = new InputStreamResource(inputStream);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        headers.add("Accept-Ranges", "bytes");
        headers.setContentLength(contentLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .contentType(MediaTypeFactory.getMediaType(file.getName())
                        .orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(resource);
    }
}
