package com.example.sleepknowledge.adapter.out.narration.google;

import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

/** Cloud TTS의 요청 크기를 넘지 않도록 Unicode code point 경계에서 원문을 나눕니다. */
final class Utf8TextChunker {

    private Utf8TextChunker() {
    }

    static List<String> split(String text, int maxBytes) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("text must not be empty");
        }
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }

        BitSet sentenceBoundaries = sentenceBoundaries(text);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int cursor = start;
            int usedBytes = 0;
            int lastSentenceBoundary = -1;
            int lastWhitespaceBoundary = -1;

            while (cursor < text.length()) {
                int codePoint = text.codePointAt(cursor);
                int next = cursor + Character.charCount(codePoint);
                int codePointBytes = text.substring(cursor, next).getBytes(StandardCharsets.UTF_8).length;
                if (usedBytes + codePointBytes > maxBytes) {
                    break;
                }

                usedBytes += codePointBytes;
                cursor = next;
                if (sentenceBoundaries.get(cursor)) {
                    lastSentenceBoundary = cursor;
                }
                if (Character.isWhitespace(codePoint)) {
                    lastWhitespaceBoundary = cursor;
                }
            }

            int end = chooseEnd(text.length(), start, cursor, lastSentenceBoundary, lastWhitespaceBoundary);
            if (end == start) {
                throw new IllegalArgumentException("a Unicode code point exceeds maxBytes");
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return List.copyOf(chunks);
    }

    private static int chooseEnd(
            int textLength,
            int start,
            int cursor,
            int lastSentenceBoundary,
            int lastWhitespaceBoundary
    ) {
        if (cursor == textLength) {
            return textLength;
        }
        if (lastSentenceBoundary > start) {
            return lastSentenceBoundary;
        }
        if (lastWhitespaceBoundary > start) {
            return lastWhitespaceBoundary;
        }
        return cursor;
    }

    private static BitSet sentenceBoundaries(String text) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.KOREAN);
        iterator.setText(text);
        BitSet boundaries = new BitSet(text.length() + 1);
        for (int boundary = iterator.first(); boundary != BreakIterator.DONE; boundary = iterator.next()) {
            if (boundary > 0) {
                boundaries.set(boundary);
            }
        }
        return boundaries;
    }
}
