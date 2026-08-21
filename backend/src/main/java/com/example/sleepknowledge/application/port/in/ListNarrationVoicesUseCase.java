package com.example.sleepknowledge.application.port.in;

import com.example.sleepknowledge.domain.model.Voice;

import java.util.List;

public interface ListNarrationVoicesUseCase {

    List<Voice> listVoices();
}
