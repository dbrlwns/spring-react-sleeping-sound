package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.adapter.in.web.dto.NarrationRequest;
import com.example.sleepknowledge.adapter.in.web.dto.VoicesResponse;
import com.example.sleepknowledge.application.port.in.GenerateNarrationUseCase;
import com.example.sleepknowledge.application.port.in.ListNarrationVoicesUseCase;
import com.example.sleepknowledge.domain.model.AudioContent;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 내레이션은 콘텐츠에 종속된 보조 API로 노출합니다. */
@RestController
@RequestMapping("/api/v1")
public class NarrationController {

    private final GenerateNarrationUseCase generateNarrationUseCase;
    private final ListNarrationVoicesUseCase listNarrationVoicesUseCase;

    public NarrationController(
            GenerateNarrationUseCase generateNarrationUseCase,
            ListNarrationVoicesUseCase listNarrationVoicesUseCase
    ) {
        this.generateNarrationUseCase = generateNarrationUseCase;
        this.listNarrationVoicesUseCase = listNarrationVoicesUseCase;
    }

    @GetMapping("/narration/voices")
    public VoicesResponse listVoices() {
        return VoicesResponse.from(listNarrationVoicesUseCase.listVoices());
    }

    @PostMapping(
            value = "/contents/{contentId}/narration",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = AudioContent.WAV_MEDIA_TYPE
    )
    public ResponseEntity<byte[]> generateNarration(
            @PathVariable UUID contentId,
            @Valid @RequestBody NarrationRequest request
    ) {
        AudioContent audio = generateNarrationUseCase.generateNarration(contentId, request.toOptions());
        byte[] body = audio.bytes();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.mediaType()))
                .contentLength(body.length)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename("narration-" + contentId + ".wav")
                                .build()
                                .toString()
                )
                .body(body);
    }
}
