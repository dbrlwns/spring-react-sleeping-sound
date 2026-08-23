package com.example.sleepknowledge.adapter.out.narration.google;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 각 LINEAR16 응답의 RIFF chunks를 검증하고 PCM data만 하나의 WAV로 합칩니다. */
final class Linear16Wav {

    private static final int CANONICAL_HEADER_BYTES = 44;
    private static final int PCM_FORMAT = 1;
    private static final int LINEAR16_BITS = 16;

    private Linear16Wav() {
    }

    static int dataSize(byte[] wavFile) {
        return parse(wavFile).dataSize();
    }

    static byte[] merge(List<byte[]> wavFiles, int maxAudioBytes) {
        if (wavFiles == null || wavFiles.isEmpty()) {
            throw new IllegalArgumentException("at least one WAV is required");
        }
        if (maxAudioBytes <= 0 || maxAudioBytes > Integer.MAX_VALUE - CANONICAL_HEADER_BYTES) {
            throw new IllegalArgumentException("maxAudioBytes is outside the supported WAV range");
        }

        List<ParsedWav> parsedWavs = new ArrayList<>(wavFiles.size());
        WavFormat expectedFormat = null;
        long totalDataBytes = 0;
        for (byte[] wavFile : wavFiles) {
            ParsedWav parsed = parse(wavFile);
            if (expectedFormat == null) {
                expectedFormat = parsed.format();
            } else if (!expectedFormat.equals(parsed.format())) {
                throw new IllegalArgumentException("WAV formats do not match");
            }

            totalDataBytes += parsed.dataSize();
            if (totalDataBytes > maxAudioBytes || totalDataBytes > Integer.MAX_VALUE - CANONICAL_HEADER_BYTES) {
                throw new IllegalArgumentException("merged WAV exceeds maxAudioBytes");
            }
            parsedWavs.add(parsed);
        }

        int dataLength = Math.toIntExact(totalDataBytes);
        byte[] merged = new byte[CANONICAL_HEADER_BYTES + dataLength];
        writeCanonicalHeader(merged, expectedFormat, dataLength);
        int offset = CANONICAL_HEADER_BYTES;
        for (ParsedWav parsed : parsedWavs) {
            for (DataChunk dataChunk : parsed.dataChunks()) {
                System.arraycopy(
                        dataChunk.source(),
                        dataChunk.offset(),
                        merged,
                        offset,
                        dataChunk.length()
                );
                offset += dataChunk.length();
            }
        }
        return merged;
    }

    private static ParsedWav parse(byte[] wav) {
        if (wav == null || wav.length < 12 || !hasId(wav, 0, "RIFF") || !hasId(wav, 8, "WAVE")) {
            throw new IllegalArgumentException("invalid RIFF/WAVE header");
        }

        long riffSize = readUnsignedInt(wav, 4);
        long declaredEnd = 8L + riffSize;
        if (declaredEnd != wav.length) {
            throw new IllegalArgumentException("RIFF size does not match payload");
        }

        WavFormat format = null;
        List<DataChunk> dataChunks = new ArrayList<>();
        long dataSize = 0;
        long offset = 12;
        while (offset < declaredEnd) {
            if (declaredEnd - offset < 8) {
                throw new IllegalArgumentException("truncated WAV chunk header");
            }

            int chunkOffset = Math.toIntExact(offset);
            long chunkSize = readUnsignedInt(wav, chunkOffset + 4);
            long chunkDataStart = offset + 8;
            long chunkDataEnd = chunkDataStart + chunkSize;
            if (chunkDataEnd < chunkDataStart || chunkDataEnd > declaredEnd) {
                throw new IllegalArgumentException("WAV chunk exceeds RIFF bounds");
            }

            if (hasId(wav, chunkOffset, "fmt ")) {
                if (format != null) {
                    throw new IllegalArgumentException("duplicate fmt chunk");
                }
                format = parseFormat(wav, Math.toIntExact(chunkDataStart), chunkSize);
            } else if (hasId(wav, chunkOffset, "data")) {
                if (chunkSize > Integer.MAX_VALUE || dataSize + chunkSize > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("WAV data is too large");
                }
                dataChunks.add(new DataChunk(
                        wav,
                        Math.toIntExact(chunkDataStart),
                        Math.toIntExact(chunkSize)
                ));
                dataSize += chunkSize;
            }

            offset = chunkDataEnd + (chunkSize & 1L);
            if (offset > declaredEnd) {
                throw new IllegalArgumentException("missing WAV chunk padding");
            }
        }

        if (format == null || dataChunks.isEmpty() || dataSize == 0) {
            throw new IllegalArgumentException("WAV must contain fmt and non-empty data chunks");
        }
        if (dataSize % format.blockAlign() != 0) {
            throw new IllegalArgumentException("WAV data is not aligned to PCM frames");
        }

        return new ParsedWav(format, List.copyOf(dataChunks), Math.toIntExact(dataSize));
    }

    private static WavFormat parseFormat(byte[] wav, int offset, long chunkSize) {
        if (chunkSize < 16 || chunkSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid fmt chunk size");
        }

        int audioFormat = readUnsignedShort(wav, offset);
        int channels = readUnsignedShort(wav, offset + 2);
        long sampleRate = readUnsignedInt(wav, offset + 4);
        long byteRate = readUnsignedInt(wav, offset + 8);
        int blockAlign = readUnsignedShort(wav, offset + 12);
        int bitsPerSample = readUnsignedShort(wav, offset + 14);
        if (audioFormat != PCM_FORMAT || channels <= 0 || sampleRate <= 0 || bitsPerSample != LINEAR16_BITS) {
            throw new IllegalArgumentException("WAV is not PCM LINEAR16");
        }

        long expectedBlockAlign = (long) channels * LINEAR16_BITS / Byte.SIZE;
        long expectedByteRate = sampleRate * expectedBlockAlign;
        if (sampleRate > Integer.MAX_VALUE
                || byteRate > Integer.MAX_VALUE
                || blockAlign != expectedBlockAlign
                || byteRate != expectedByteRate) {
            throw new IllegalArgumentException("inconsistent PCM format fields");
        }
        return new WavFormat(channels, Math.toIntExact(sampleRate), Math.toIntExact(byteRate), blockAlign);
    }

    private static void writeCanonicalHeader(byte[] target, WavFormat format, int dataLength) {
        ByteBuffer buffer = ByteBuffer.wrap(target).order(ByteOrder.LITTLE_ENDIAN);
        putId(buffer, "RIFF");
        buffer.putInt(36 + dataLength);
        putId(buffer, "WAVE");
        putId(buffer, "fmt ");
        buffer.putInt(16);
        buffer.putShort((short) PCM_FORMAT);
        buffer.putShort((short) format.channels());
        buffer.putInt(format.sampleRate());
        buffer.putInt(format.byteRate());
        buffer.putShort((short) format.blockAlign());
        buffer.putShort((short) LINEAR16_BITS);
        putId(buffer, "data");
        buffer.putInt(dataLength);
    }

    private static boolean hasId(byte[] bytes, int offset, String expected) {
        if (offset < 0 || offset + 4 > bytes.length) {
            return false;
        }
        byte[] id = expected.getBytes(StandardCharsets.US_ASCII);
        for (int index = 0; index < id.length; index++) {
            if (bytes[offset + index] != id[index]) {
                return false;
            }
        }
        return true;
    }

    private static long readUnsignedInt(byte[] bytes, int offset) {
        requireRange(bytes, offset, Integer.BYTES);
        return Integer.toUnsignedLong(ByteBuffer.wrap(bytes, offset, Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt());
    }

    private static int readUnsignedShort(byte[] bytes, int offset) {
        requireRange(bytes, offset, Short.BYTES);
        return Short.toUnsignedInt(ByteBuffer.wrap(bytes, offset, Short.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getShort());
    }

    private static void requireRange(byte[] bytes, int offset, int length) {
        if (offset < 0 || length < 0 || offset > bytes.length - length) {
            throw new IllegalArgumentException("truncated WAV field");
        }
    }

    private static void putId(ByteBuffer buffer, String id) {
        buffer.put(id.getBytes(StandardCharsets.US_ASCII));
    }

    private record WavFormat(int channels, int sampleRate, int byteRate, int blockAlign) {
    }

    private record DataChunk(byte[] source, int offset, int length) {
    }

    private record ParsedWav(WavFormat format, List<DataChunk> dataChunks, int dataSize) {
    }
}
