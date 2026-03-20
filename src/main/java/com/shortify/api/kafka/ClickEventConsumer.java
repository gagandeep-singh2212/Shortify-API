package com.shortify.api.kafka;

import com.shortify.api.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class ClickEventConsumer {

    private final UrlRepository urlRepository;
    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    public ClickEventConsumer(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleClickEvent(String shortCode) {
        urlRepository.incrementClickCount(shortCode);
        log.info("Click count updated for shortCode: {}", shortCode);
    }
}