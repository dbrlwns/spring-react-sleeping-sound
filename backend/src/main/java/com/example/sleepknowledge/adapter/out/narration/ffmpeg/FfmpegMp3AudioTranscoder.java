package com.example.sleepknowledge.adapter.out.narration.ffmpeg;

import com.example.sleepknowledge.application.exception.AudioTranscodingException;
import com.example.sleepknowledge.application.port.out.AudioTranscodingPort;
import com.example.sleepknowledge.config.NarrationProperties;
import com.example.sleepknowledge.domain.model.AudioContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** ProcessBuilder로 FFmpeg를 격리 실행해 LINEAR16 WAV를 하나의 MP3로 인코딩합니다. */
public final class FfmpegMp3AudioTranscoder implements AudioTranscodingPort {

    private static final Logger log = LoggerFactory.getLogger(FfmpegMp3AudioTranscoder.class);
    private static final long OUTPUT_SIZE_CHECK_INTERVAL_MILLIS = 200;
    private static final int MAX_DIAGNOSTIC_BYTES = 4_096;
    private static final long AVAILABILITY_TIMEOUT_SECONDS = 10;
    private static final int PREFLIGHT_SAMPLE_RATE = 24_000;
    private static final int PREFLIGHT_PCM_BYTES = 960;

    private final NarrationProperties.Ffmpeg properties;
    private final AtomicBoolean availabilityVerified = new AtomicBoolean();

    public FfmpegMp3AudioTranscoder(NarrationProperties.Ffmpeg properties) {
        this.properties = properties;
    }

    @Override
    public void verifyAvailable() {
        if (availabilityVerified.get()) {
            return;
        }
        synchronized (availabilityVerified) {
            if (availabilityVerified.get()) {
                return;
            }

            Process process = null;
            try {
                process = new ProcessBuilder(availabilityCommand())
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                try (OutputStream standardInput = process.getOutputStream()) {
                    // 24 kHz mono PCM16 약 20 ms를 실제 libmp3lame에 통과시켜 codec까지 확인합니다.
                    standardInput.write(new byte[PREFLIGHT_PCM_BYTES]);
                }
                if (!process.waitFor(AVAILABILITY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    terminate(process);
                    throw new AudioTranscodingException("FFmpeg MP3 인코더 확인 시간이 초과되었습니다.");
                }
                if (process.exitValue() != 0) {
                    throw new AudioTranscodingException(
                            "FFmpeg의 libmp3lame MP3 인코더를 사용할 수 없습니다."
                    );
                }
                availabilityVerified.set(true);
            } catch (IOException exception) {
                throw new AudioTranscodingException(
                        "FFmpeg를 실행하지 못했습니다. 설치 상태와 인코더 설정을 확인해 주세요.",
                        exception
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                terminate(process);
                throw new AudioTranscodingException("FFmpeg 실행 확인이 중단되었습니다.", exception);
            } finally {
                terminate(process);
            }
        }
    }

    private List<String> availabilityCommand() {
        return List.of(
                properties.command(),
                "-hide_banner",
                "-loglevel", "error",
                "-f", "s16le",
                "-ar", Integer.toString(PREFLIGHT_SAMPLE_RATE),
                "-ac", "1",
                "-i", "pipe:0",
                "-codec:a", "libmp3lame",
                "-b:a", properties.bitrateKbps() + "k",
                "-f", "mp3",
                "pipe:1"
        );
    }

    @Override
    public AudioContent encodeMp3(AudioContent linear16Wav) {
        if (!AudioContent.LINEAR16_WAV_MEDIA_TYPE.equals(linear16Wav.mediaType())) {
            throw new AudioTranscodingException("MP3 인코딩 입력은 LINEAR16 WAV여야 합니다.");
        }
        verifyAvailable();

        Path workingDirectory = null;
        Path input = null;
        Path output = null;
        Path diagnostic = null;
        Process process = null;
        try {
            workingDirectory = Files.createTempDirectory("moirai-narration-");
            input = workingDirectory.resolve("input.wav");
            output = workingDirectory.resolve("output.mp3");
            diagnostic = workingDirectory.resolve("ffmpeg-error.log");
            Files.write(
                    input,
                    linear16Wav.bytes(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

            List<String> command = command(input, output);
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(diagnostic.toFile());
            try {
                // shell을 거치지 않으므로 원고나 경로가 명령으로 해석되지 않습니다.
                process = processBuilder.start();
            } catch (IOException exception) {
                throw new AudioTranscodingException(
                        "FFmpeg를 실행하지 못했습니다. 설치 상태와 인코더 설정을 확인해 주세요.",
                        exception
                );
            }

            int exitCode = waitFor(process, output);
            if (exitCode != 0) {
                logDiagnostic(exitCode, diagnostic);
                throw new AudioTranscodingException("WAV를 MP3로 인코딩하지 못했습니다.");
            }

            long outputSize = requireBoundedOutput(output);
            if (outputSize == 0) {
                throw new AudioTranscodingException("FFmpeg가 빈 MP3를 만들었습니다.");
            }
            byte[] mp3 = Files.readAllBytes(output);
            if (!containsMpegAudioFrame(mp3)) {
                throw new AudioTranscodingException("FFmpeg가 유효한 MP3를 만들지 못했습니다.");
            }
            return AudioContent.mp3(mp3);
        } catch (AudioTranscodingException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            terminate(process);
            throw new AudioTranscodingException("MP3 인코딩이 중단되었습니다.", exception);
        } catch (IOException exception) {
            terminate(process);
            throw new AudioTranscodingException("MP3 인코딩 파일을 처리하지 못했습니다.", exception);
        } finally {
            terminate(process);
            deleteQuietly(diagnostic);
            deleteQuietly(output);
            deleteQuietly(input);
            deleteQuietly(workingDirectory);
        }
    }

    private List<String> command(Path input, Path output) {
        return List.of(
                properties.command(),
                "-hide_banner",
                "-loglevel", "error",
                "-nostdin",
                "-y",
                "-i", input.toString(),
                "-map_metadata", "-1",
                "-vn",
                "-codec:a", "libmp3lame",
                "-b:a", properties.bitrateKbps() + "k",
                "-write_xing", "1",
                "-f", "mp3",
                output.toString()
        );
    }

    private int waitFor(Process process, Path output) throws IOException, InterruptedException {
        long timeoutNanos = properties.timeout().toNanos();
        long deadline = System.nanoTime() + timeoutNanos;
        while (true) {
            if (Files.exists(output) && Files.size(output) > properties.maxOutputBytes()) {
                terminate(process);
                throw new AudioTranscodingException("인코딩된 MP3가 허용된 크기를 초과했습니다.");
            }
            if (!process.isAlive()) {
                return process.exitValue();
            }

            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                terminate(process);
                throw new AudioTranscodingException("MP3 인코딩 제한 시간을 초과했습니다.");
            }
            long waitNanos = Math.min(
                    remainingNanos,
                    TimeUnit.MILLISECONDS.toNanos(OUTPUT_SIZE_CHECK_INTERVAL_MILLIS)
            );
            if (process.waitFor(waitNanos, TimeUnit.NANOSECONDS)) {
                return process.exitValue();
            }
        }
    }

    private long requireBoundedOutput(Path output) throws IOException {
        if (!Files.isRegularFile(output)) {
            throw new AudioTranscodingException("FFmpeg가 MP3 파일을 만들지 못했습니다.");
        }
        long size = Files.size(output);
        if (size > properties.maxOutputBytes()) {
            throw new AudioTranscodingException("인코딩된 MP3가 허용된 크기를 초과했습니다.");
        }
        return size;
    }

    private void logDiagnostic(int exitCode, Path diagnostic) {
        try (InputStream input = Files.newInputStream(diagnostic)) {
            byte[] bytes = input.readNBytes(MAX_DIAGNOSTIC_BYTES);
            String message = new String(bytes, StandardCharsets.UTF_8)
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .trim();
            log.warn("FFmpeg exited with code {}: {}", exitCode, message);
        } catch (IOException exception) {
            log.warn("FFmpeg exited with code {}; diagnostic output could not be read", exitCode);
        }
    }

    private static boolean containsMpegAudioFrame(byte[] bytes) {
        int start = 0;
        if (bytes.length >= 10 && bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3') {
            for (int index = 6; index <= 9; index++) {
                if ((bytes[index] & 0x80) != 0) {
                    return false;
                }
            }
            int tagSize = ((bytes[6] & 0x7f) << 21)
                    | ((bytes[7] & 0x7f) << 14)
                    | ((bytes[8] & 0x7f) << 7)
                    | (bytes[9] & 0x7f);
            long audioStart = 10L + tagSize;
            if (audioStart >= bytes.length) {
                return false;
            }
            start = Math.toIntExact(audioStart);
        }
        for (int index = start; index < bytes.length - 1; index++) {
            int first = bytes[index] & 0xff;
            int second = bytes[index + 1] & 0xff;
            if (first == 0xff && (second & 0xe0) == 0xe0 && (second & 0x18) != 0x08) {
                return true;
            }
        }
        return false;
    }

    private static void terminate(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Could not delete temporary narration path {}", path, exception);
        }
    }
}
