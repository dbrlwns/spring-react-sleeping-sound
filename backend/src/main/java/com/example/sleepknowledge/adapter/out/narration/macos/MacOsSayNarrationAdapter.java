package com.example.sleepknowledge.adapter.out.narration.macos;

import com.example.sleepknowledge.application.exception.SpeechSynthesisException;
import com.example.sleepknowledge.application.port.out.SpeechSynthesisPort;
import com.example.sleepknowledge.config.NarrationProperties;
import com.example.sleepknowledge.domain.model.AudioContent;
import com.example.sleepknowledge.domain.model.NarrationOptions;
import com.example.sleepknowledge.domain.model.Voice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** macOS 내장 {@code say}를 사용하는 내레이션 outbound adapter입니다. */
public class MacOsSayNarrationAdapter implements SpeechSynthesisPort {

    private static final Pattern VOICE_LINE = Pattern.compile(
            "^(.+?)\\s+([a-z]{2,3}_[A-Z0-9]{2,3})\\s+#\\s?(.*)$"
    );

    private final NarrationProperties.MacOsSay properties;

    public MacOsSayNarrationAdapter(NarrationProperties.MacOsSay properties) {
        this.properties = properties;
    }

    @Override
    public List<Voice> findAvailableVoices() {
        ProcessResult result = execute(List.of(properties.command(), "-v", "?"));
        List<Voice> voices = parseVoices(result.output());
        if (voices.isEmpty()) {
            throw new SpeechSynthesisException("macOS 음성 목록을 읽지 못했습니다.");
        }
        return voices;
    }

    @Override
    public AudioContent synthesize(String script, NarrationOptions options, Voice voice) {
        Path workDirectory = null;
        try {
            workDirectory = Files.createTempDirectory("narration-say-");
            Path inputFile = workDirectory.resolve("script.txt");
            Path outputFile = workDirectory.resolve("narration.wav");
            Files.writeString(inputFile, script, StandardCharsets.UTF_8);

            int wordsPerMinute = Math.max(
                    1,
                    (int) Math.round(properties.baseWordsPerMinute() * options.speed())
            );

            // 셸 문자열을 만들지 않고 각 인수를 분리해 command injection을 방지합니다.
            execute(List.of(
                    properties.command(),
                    "-v", voice.id(),
                    "-r", Integer.toString(wordsPerMinute),
                    "-o", outputFile.toString(),
                    "--file-format=WAVE",
                    "--data-format=LEI16@22050",
                    "-f", inputFile.toString()
            ));

            if (!Files.isRegularFile(outputFile) || Files.size(outputFile) == 0) {
                throw new SpeechSynthesisException("say 명령이 WAV 파일을 생성하지 않았습니다.");
            }
            return AudioContent.wav(Files.readAllBytes(outputFile));
        } catch (IOException exception) {
            throw new SpeechSynthesisException("내레이션 파일을 처리하지 못했습니다.", exception);
        } finally {
            deleteTemporaryFiles(workDirectory);
        }
    }

    static List<Voice> parseVoices(String output) {
        List<Voice> voices = new ArrayList<>();
        output.lines().forEach(line -> {
            Matcher matcher = VOICE_LINE.matcher(line);
            if (matcher.matches()) {
                String name = matcher.group(1).trim();
                voices.add(new Voice(name, name, matcher.group(3).trim(), matcher.group(2)));
            }
        });
        return voices.stream()
                .sorted(Comparator.comparing(Voice::locale).thenComparing(Voice::name))
                .toList();
    }

    private ProcessResult execute(List<String> command) {
        Process process = null;
        Path outputLog = null;
        try {
            // 출력이 파이프 버퍼를 채우며 멈추지 않도록 임시 파일로 계속 배출합니다.
            outputLog = Files.createTempFile("narration-process-", ".log");
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(outputLog.toFile())
                    .start();

            boolean finished = process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new SpeechSynthesisException("TTS 엔진 응답 시간이 초과되었습니다.");
            }

            String output = Files.readString(outputLog, StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new SpeechSynthesisException("TTS 엔진 실행에 실패했습니다(exit=" + process.exitValue() + ")");
            }
            return new ProcessResult(process.exitValue(), output);
        } catch (IOException exception) {
            destroyProcess(process);
            throw new SpeechSynthesisException(
                    "macOS say 명령을 실행할 수 없습니다. "
                            + "app.narration.macos-say.command 설정을 확인해 주세요.",
                    exception
            );
        } catch (InterruptedException exception) {
            destroyProcess(process);
            Thread.currentThread().interrupt();
            throw new SpeechSynthesisException("TTS 엔진 실행 대기가 중단되었습니다.", exception);
        } finally {
            deleteProcessLog(outputLog);
        }
    }

    private void destroyProcess(Process process) {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private void deleteProcessLog(Path outputLog) {
        if (outputLog == null) {
            return;
        }
        try {
            Files.deleteIfExists(outputLog);
        } catch (IOException ignored) {
            // 운영 환경에서는 임시 파일 정리 실패를 메트릭으로 남길 수 있습니다.
        }
    }

    private void deleteTemporaryFiles(Path directory) {
        if (directory == null) {
            return;
        }
        try {
            Files.deleteIfExists(directory.resolve("script.txt"));
            Files.deleteIfExists(directory.resolve("narration.wav"));
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // 합성 성공 여부보다 임시 파일 정리 실패를 우선하지 않습니다.
        }
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
