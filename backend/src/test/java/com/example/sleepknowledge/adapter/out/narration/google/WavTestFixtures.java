package com.example.sleepknowledge.adapter.out.narration.google;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

final class WavTestFixtures {

    private WavTestFixtures() {
    }

    static byte[] wav(int sampleRate, byte[] pcm, boolean extendedFormat, boolean extraChunks) {
        try {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write("WAVE".getBytes(StandardCharsets.US_ASCII));
            if (extraChunks) {
                writeChunk(body, "JUNK", new byte[]{1, 2, 3});
            }

            ByteBuffer format = ByteBuffer.allocate(extendedFormat ? 18 : 16).order(ByteOrder.LITTLE_ENDIAN);
            format.putShort((short) 1);
            format.putShort((short) 1);
            format.putInt(sampleRate);
            format.putInt(sampleRate * 2);
            format.putShort((short) 2);
            format.putShort((short) 16);
            if (extendedFormat) {
                format.putShort((short) 0);
            }
            writeChunk(body, "fmt ", format.array());
            if (extraChunks) {
                writeChunk(body, "LIST", new byte[]{4, 5, 6, 7, 8});
            }
            writeChunk(body, "data", pcm);

            ByteArrayOutputStream riff = new ByteArrayOutputStream();
            riff.write("RIFF".getBytes(StandardCharsets.US_ASCII));
            writeLittleEndianInt(riff, body.size());
            riff.write(body.toByteArray());
            return riff.toByteArray();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void writeChunk(ByteArrayOutputStream output, String id, byte[] data) throws IOException {
        output.write(id.getBytes(StandardCharsets.US_ASCII));
        writeLittleEndianInt(output, data.length);
        output.write(data);
        if ((data.length & 1) == 1) {
            output.write(0);
        }
    }

    private static void writeLittleEndianInt(ByteArrayOutputStream output, int value) throws IOException {
        output.write(ByteBuffer.allocate(Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array());
    }
}
