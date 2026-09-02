package com.example.sleepknowledge.adapter.in.web;

import com.example.sleepknowledge.adapter.in.web.dto.NarrationStatusResponse;
import com.example.sleepknowledge.adapter.in.web.dto.VoicesResponse;
import com.example.sleepknowledge.application.port.in.BrowseNarrationUseCase;
import com.example.sleepknowledge.application.port.in.ListNarrationVoicesUseCase;
import com.example.sleepknowledge.domain.model.AudioContent;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 내레이션은 콘텐츠에 종속된 보조 API로 노출합니다. */
@RestController
@RequestMapping("/api/v1")
public class NarrationController {

    private final BrowseNarrationUseCase browseNarrationUseCase;
    private final ListNarrationVoicesUseCase listNarrationVoicesUseCase;

    public NarrationController(
            BrowseNarrationUseCase browseNarrationUseCase,
            ListNarrationVoicesUseCase listNarrationVoicesUseCase
    ) {
        this.browseNarrationUseCase = browseNarrationUseCase;
        this.listNarrationVoicesUseCase = listNarrationVoicesUseCase;
    }

    @GetMapping("/narration/voices")
    public VoicesResponse listVoices() {
        return VoicesResponse.from(listNarrationVoicesUseCase.listVoices());
    }

    @GetMapping(value = "/contents/{contentId}/narration/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NarrationStatusResponse> getNarrationStatus(@PathVariable UUID contentId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(NarrationStatusResponse.from(browseNarrationUseCase.getNarrationStatus(contentId)));
    }

    @GetMapping(
            value = "/contents/{contentId}/narration/audio",
            produces = AudioContent.MP3_MEDIA_TYPE
    )
    public ResponseEntity<byte[]> getNarrationAudio(@PathVariable UUID contentId) {
        AudioContent audio = browseNarrationUseCase.getNarrationAudio(contentId);
        byte[] body = audio.bytes();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.mediaType()))
                .contentLength(body.length)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename("narration-" + contentId + ".mp3")
                                .build()
                                .toString()
                )
                .body(body);
    }
}
