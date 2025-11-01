package app.aihandmade.run

import app.aihandmade.core.pipeline.RunContext
import app.aihandmade.export.ArtifactStoreFs
import app.aihandmade.logging.JsonLogger
import app.aihandmade.logging.ManifestWriter
import app.aihandmade.storage.createProject
import app.aihandmade.storage.createRun
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class RunContextFactory(
    private val storageRoot: File,
    private val idGenerator: RunIdGenerator = UlidGenerator(),
    private val clock: Clock = Clock.systemUTC(),
) {

    fun create(projectId: String): RunContext {
        val runId = idGenerator.nextId()
        val runDate = LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE)
        val projectDir = createProject(storageRoot, projectId)
        val runDir = createRun(projectDir, "$runDate/$runId")

        val runPath = runDir.root.toPath()
        val manifestWriter = ManifestWriter(
            runId = runId,
            outputDir = runPath
        )

        val logger = JsonLogger(
            runId = runId,
            outputDir = runPath,
            manifestWriter = manifestWriter,
            clock = clock
        )

        val artifactStore = ArtifactStoreFs(
            runId = runId,
            rootDir = runPath,
            clock = clock
        )

        val rngSeed = System.currentTimeMillis()

        val eventsFile = runDir.eventsFile.toPath()
        appendLifecycleEvent(eventsFile, "run_start", runId, clock.instant())

        return RunContext(
            runId = runId,
            runDirectory = runPath,
            logger = logger,
            artifactStore = artifactStore,
            rngSeed = rngSeed,
            onClose = {
                try {
                    appendLifecycleEvent(eventsFile, "run_end", runId, clock.instant())
                } finally {
                    manifestWriter.write()
                }
            }
        )
    }

    private fun appendLifecycleEvent(
        eventsFile: Path,
        event: String,
        runId: String,
        timestamp: Instant,
    ) {
        val serialized = buildString {
            append('{')
            append("\"event\":").append(jsonString(event)).append(',')
            append("\"run_id\":").append(jsonString(runId)).append(',')
            append("\"timestamp\":").append(jsonString(DateTimeFormatter.ISO_INSTANT.format(timestamp)))
            append('}')
        }

        Files.newBufferedWriter(
            eventsFile,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        ).use { writer ->
            writer.append(serialized)
            writer.append('\n')
        }
    }

    private fun jsonString(value: String): String {
        val escaped = buildString(value.length + 2) {
            append('"')
            for (ch in value) {
                when (ch) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (ch < ' ') {
                            append(String.format(Locale.US, "\\u%04x", ch.code))
                        } else {
                            append(ch)
                        }
                    }
                }
            }
            append('"')
        }
        return escaped
    }
}
