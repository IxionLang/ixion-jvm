package com.kingmang.ixion.api;

import com.kingmang.ixion.Visitor;
import com.kingmang.ixion.ast.Statement;
import com.kingmang.ixion.lexer.LexerImpl;
import com.kingmang.ixion.parser.Parser;
import com.kingmang.ixion.runtime.IxType;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class IxFile {
    public final File file;
    public final List<Statement> stmts;
    public final LinkedMap<String, IxType> exports = new LinkedMap<>();
    public final Context rootContext = new Context();
    public final String projectRoot;
    public final String relativePath;

    public final String name;

    public final LinkedMap<String, IxFile> imports = new LinkedMap<>();

    public IxFile(String projectRoot, String relativePath, String name) throws FileNotFoundException {
        this.projectRoot = FilenameUtils.separatorsToUnix(projectRoot);
        this.relativePath = FilenameUtils.separatorsToUnix(relativePath);
        this.name = FilenameUtils.removeExtension(FilenameUtils.removeExtension(name));

        String fullPath = FilenameUtils.separatorsToUnix(Path.of(projectRoot, relativePath, this.name + IxionConstant.EXT).toString());
        this.file = new File(fullPath);

        LexerImpl lexer = new LexerImpl(file);
        var parser = new Parser(lexer);
        this.stmts = parser.parse();
    }

    public <R> List<R> acceptVisitor(Visitor<? extends R> visitor) {
        List<R> results = new ArrayList<>();
        for (var s : this.stmts) {
            var r = s.accept(visitor);
            results.add(r);
        }
        return results;
    }

    public void addImport(String absolute, IxFile ixFile) {
        absolute = FilenameUtils.separatorsToUnix(absolute);
        imports.put(absolute, ixFile);
    }

    public <T extends Statement, R> void filter(Class<? extends T> kind, Function<? super T, R> function) {
        for (var s : stmts) {
            if (kind.isInstance(s)) {
                function.apply(kind.cast(s));
            }
        }
    }

    public String getFullRelativePath() {
        return FilenameUtils.separatorsToUnix(Path.of(relativePath, name).toString());
    }

    @Override
    public String toString() {
        return file.getName();
    }
}
