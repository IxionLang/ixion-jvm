package com.kingmang.ixion.exception;

import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.parser.Node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class IxException {
    private static final String RED_START = "\u001B[31m";
    private static final String BLUE_START = "\u001B[34m";
    private static final String RESET = "\u001B[0m";

    public final int code;
    private final String suggestion;
    private final String templateString;

    protected IxException(int code, String templateString, String suggestion) {
        this.code = code;
        this.templateString = templateString;
        this.suggestion = suggestion;
    }

    public static void killIfErrors(IxApi ixApi, String message) throws CompilerError {
        if (!ixApi.errorData().isEmpty()) {
            ixApi.errorData().forEach(e -> System.out.println(e.message));
            throw new CompilerError(message, ixApi.errorData());
        }
    }

    public void send(IxApi ixApi, File file, Node node, Object... varargs) {
        try {
            var pos = node.pos();
            int line = pos.line() - 2;
            if (line < 0) line = 0;
            int limit = 3;

            Stream<String> lines = Files.lines(file.toPath());
            var selection = lines.skip(line).limit(limit).toList();

            int startLine = line;
            int endLine = line + limit;
            int padding = (int) Math.ceil(Math.log10(endLine + 1)) + 1;
            var result = IntStream.range(0, limit)
                    .mapToObj(i -> {
                        if (i < selection.size()) {
                            String lineNumber = String.valueOf(i + startLine + 1);
                            String paddedLineNumber = leftPad(lineNumber + ":", padding);
                            String s = BLUE_START + paddedLineNumber + RESET + " ";
                            s += selection.get(i);
                            if (i + startLine + 1 == pos.line()) {
                                s += "\n" + RED_START + "^".repeat(pos.col() + padding) + RESET;
                            }
                            return s;
                        } else {
                            return (i + startLine + 1) + "|";
                        }
                    })
                    .collect(Collectors.joining("\n"));

            StringBuilder buffer = new StringBuilder();
            buffer.append(RED_START)
                    .append("[")
                    .append(getClass().getSimpleName())
                    .append("] in ")
                    .append(String.format(file.getName() + "[%d:%d]\n", pos.line(), pos.col()))
                    .append(RESET)
                    .append(MessageFormat.format(templateString, varargs)).append("\n")
                    .append(result).append("\n");
            if (suggestion != null) {
                buffer.append(suggestion);
            }

            if (ixApi.developmentMode()) {
                var stackTrace = Thread.currentThread().getStackTrace()[2];
                int sourceLineNumber = stackTrace.getLineNumber();
                String sourceLocation = stackTrace.getClassName() + ":" + stackTrace.getMethodName();
                var logSource = "\nLogged from " + sourceLocation + "@" + sourceLineNumber + "\n";
                buffer.append(logSource);
            }

            ixApi.errorData().add(new Data(code, buffer.toString(), line, pos.col()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Вспомогательный метод для выравнивания строки слева
    private static String leftPad(String str, int size) {
        if (str.length() >= size) {
            return str;
        }
        return " ".repeat(size - str.length()) + str;
    }

    public record Data(int code, String message, int line, int col) {}

    public static class CompilerError extends Exception {
        public final List<Data> errorData;

        public CompilerError(String errorMessage, List<Data> data) {
            super(errorMessage);
            this.errorData = data;
        }
    }
}