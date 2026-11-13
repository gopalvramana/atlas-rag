package com.atlas.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CompileJavaTool {

    private final JavaCompiler compiler;

    public CompileJavaTool() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (this.compiler == null) {
            throw new IllegalStateException(
                "Java compiler not available — Atlas must run on a JDK, not a JRE");
        }
    }

    public record CompileResult(boolean compiled, List<String> errors) {}

    @Tool(description = """
            Compile a Java source file and return compilation errors.
            Use this to verify that a Java code example is syntactically correct.
            Pass a complete, self-contained Java class as the source.
            """)
    public CompileResult compileJava(
            @ToolParam(description = "A complete Java class source code to compile") String source) {

        Path tempDir  = null;
        Path tempFile = null;
        try {
            tempDir  = Files.createTempDirectory("atlas-compile-");
            tempFile = tempDir.resolve(extractClassName(source) + ".java");
            Files.writeString(tempFile, source);

            // collect diagnostics instead of printing to stderr
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            try (StandardJavaFileManager fileManager =
                         compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, null)) {

                Iterable<? extends JavaFileObject> compilationUnits =
                        fileManager.getJavaFileObjects(tempFile.toFile());

                JavaCompiler.CompilationTask task = compiler.getTask(
                        null,              // use DiagnosticCollector, not a Writer
                        fileManager,
                        diagnostics,
                        null,              // no extra compiler options
                        null,              // no annotation processing
                        compilationUnits
                );

                boolean success = task.call();

                List<String> errors = diagnostics.getDiagnostics().stream()
                        .filter(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR)
                        .map(d -> "Line %d, Col %d: %s".formatted(
                                d.getLineNumber(), d.getColumnNumber(),
                                d.getMessage(Locale.ENGLISH)))
                        .toList();

                return new CompileResult(success, errors);
            }

        } catch (IOException e) {
            return new CompileResult(false, List.of("Failed to write source to temp file: " + e.getMessage()));
        } finally {
            deleteSilently(tempFile);
            deleteSilently(tempDir);
        }
    }

    private static final Pattern CLASS_NAME_PATTERN =
            Pattern.compile("\\bpublic\\s+class\\s+(\\w+)");

    private String extractClassName(String source) {
        Matcher m = CLASS_NAME_PATTERN.matcher(source);
        return m.find() ? m.group(1) : "Source";
    }

    private void deleteSilently(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
        }
    }

    // --- standalone test ---
    public static void main(String[] args) {
        CompileJavaTool tool = new CompileJavaTool();

        String validSource = """
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello");
                    }
                }
                """;

        String invalidSource = """
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello")   // missing semicolon
                    }
                }
                """;

        System.out.println("--- Valid source ---");
        System.out.println(tool.compileJava(validSource));

        System.out.println("\n--- Invalid source ---");
        System.out.println(tool.compileJava(invalidSource));
    }
}
