package com.shortify.api.dto;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ShortenRequest {

    @NotBlank(message = "URL cannot be blank")
    @Pattern(regexp = "^(https?://).*", message = "URL must start with http:// or https://")
    private String url;

    public String getUrl() {
        return url;
    }
}
