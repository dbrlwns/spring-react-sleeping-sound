package com.example.sleepknowledge.application.port.in;

import com.example.sleepknowledge.domain.model.Episode;

import java.util.List;
import java.util.UUID;

/** 콘텐츠 목록과 상세를 조회하는 inbound port입니다. */
public interface BrowseContentUseCase {

    List<Episode> listContents();

    Episode getContent(UUID contentId);
}
