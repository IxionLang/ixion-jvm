package com.kingmang.ixion.api;

import com.kingmang.ixion.ast.ExportStatement;
import com.kingmang.ixion.ast.Statement;
import com.kingmang.ixion.ast.UseStatement;
import com.kingmang.ixion.codegen.BytecodeGenerator;
import com.kingmang.ixion.env.EnvironmentVisitor;
import com.kingmang.ixion.exception.*;
import com.kingmang.ixion.modules.Modules;
import com.kingmang.ixion.runtime.DefType;
import com.kingmang.ixion.runtime.IxType;
import com.kingmang.ixion.typechecker.TypeCheckVisitor;
import org.apache.commons.io.FilenameUtils;
import org.javatuples.Pair;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record IxApi(
        List<IxException.Data> errorData,
        Map<String, IxFile> compilationSet,
        boolean developmentMode
) {


    public IxApi() {
        this(new ArrayList<>(), new HashMap<>(), true);
    }

    public String compile(String projectRoot, String filename) throws FileNotFoundException, IxException.CompilerError {

        String relativePath = FilenameUtils.getPath(filename);
        String name = FilenameUtils.getName(filename);

        var entry = parse(projectRoot, relativePath, name);
        IxException.killIfErrors(this, "Correct parser errors before continuing.");

        for (String filePath : compilationSet.keySet()) {
            var source = compilationSet.get(filePath);
            EnvironmentVisitor environmentVisitor = new EnvironmentVisitor(this, source.rootContext, source);
            source.acceptVisitor(environmentVisitor);

            for (Statement stmt : source.stmts) {
                if (stmt instanceof ExportStatement exportStmt) {
                    if (exportStmt.stmt instanceof PublicAccess publicAccess) {
                        String identifier = publicAccess.identifier();
                        IxType type = source.rootContext.getVariable(identifier);
                        if (type != null) {
                            source.exports.put(identifier, type);
                        }
                    }

                }
            }

            IxException.killIfErrors(this, "Correct syntax errors before type checking can continue.");
        }

        for (String filePath : compilationSet.keySet()) {
            var source = compilationSet.get(filePath);
            for (String s : source.imports.keySet()) {
                var sourceFile = source.imports.get(s);
                var exportedMembers = sourceFile.exports;
                for (String exportedName : exportedMembers.keySet()) {
                    var exportedType = exportedMembers.get(exportedName);
                    var qualifiedName = sourceFile.name + "::" + exportedName;
                    if (exportedType instanceof DefType ft) {
                        ft.external = sourceFile;
                    }
                    source.rootContext.addVariable(qualifiedName, exportedType);
                }
            }

        }

        for (String filePath : compilationSet.keySet()) {
            var source = compilationSet.get(filePath);
            TypeCheckVisitor typeCheckVisitor = new TypeCheckVisitor(this, source.rootContext, source);
            source.acceptVisitor(typeCheckVisitor);

            IxException.killIfErrors(this, "Correct type errors before compilation can continue.");

        }
        output(compilationSet);

        String base = entry.getFullRelativePath();

        return base.replace("/", ".");
    }

    public void output(Map<String, ? extends IxFile> compilationSet) throws IxException.CompilerError {

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();
        for (var key : compilationSet.keySet()) {
            var source = compilationSet.get(key);
            var allByteUnits = bytecodeGenerator.generate(this, source);
            IxException.killIfErrors(this, "Correct build errors before compilation can complete.");

            var byteUnit = allByteUnits.getValue0().toByteArray();
            String base = FilenameUtils.removeExtension(source.getFullRelativePath());
            String fileName = Path.of(source.projectRoot, IxionConstant.OUT_DIR, base + ".class").toString();
            File tmp = new File(fileName);
            tmp.getParentFile().mkdirs();
            OutputStream output;
            try {
                output = new FileOutputStream(fileName);
                output.write(byteUnit);
                output.close();
            } catch (IOException e) {
                System.err.println("The above call to mkdirs() should have worked.");
                System.exit(9);
            }

            for (var p : allByteUnits.getValue1().entrySet()) {

                var st = p.getKey();
                var innerCw = p.getValue();

                String innerName = source.getFullRelativePath() + "$" + st.name;

                fileName = Path.of(source.projectRoot, IxionConstant.OUT_DIR, innerName + ".class").toString();
                tmp = new File(fileName);
                tmp.getParentFile().mkdirs();
                try {
                    output = new FileOutputStream(fileName);
                    output.write(innerCw.toByteArray());
                    output.close();
                } catch (IOException e) {
                    System.err.println("The above call to mkdirs() should have worked.");
                    System.exit(9);
                }
            }

        }

    }

    public IxFile parse(String projectRoot, String relativePath, String name) throws FileNotFoundException {

        var source = new IxFile(projectRoot, relativePath, name);
        Debugger.debug("Parsing `" + source.file.getName() + "`");

        compilationSet.put(FilenameUtils.separatorsToUnix(source.file.getPath()), source);

        List<Pair<String, String>> imports = new ArrayList<>();

        for (Statement statement : source.stmts) {
            if (statement instanceof UseStatement useStmt) {
                String requestedUse = useStmt.stringLiteral.source();
                Debugger.debug("\tFound module `" + requestedUse + "`");

                String relative = FilenameUtils.getPath(requestedUse);
                String n = FilenameUtils.getName(requestedUse);
                String filePath = Path.of(source.projectRoot, source.relativePath, relative, n + IxionConstant.EXT).toString();
                var normalizedPath = FilenameUtils.separatorsToUnix(Path.of(filePath).normalize().toString());
                if (new File(normalizedPath).exists()) {
                    imports.add(Pair.with(relative, normalizedPath));
                } else if (!Modules.modules.containsKey(n)) {
                    new ModuleNotFoundException().send(this, source.file, statement, n);
                }

            }
        }

        for (Pair<String, String> i : imports) {
            var relative = i.getValue0();
            var normalizedPath = i.getValue1();
            var n = FilenameUtils.removeExtension(FilenameUtils.getName(normalizedPath));
            if (!compilationSet.containsKey(normalizedPath)) {
                Debugger.debug("triggered parse, no key exists `" + normalizedPath + "`");
                IxFile next;
                try {
                    next = parse(projectRoot, Path.of(source.relativePath, relative).normalize().toString(), n);
                    source.addImport(normalizedPath, next);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    exit("Issues with building import tree.", 67);
                }
            } else {
                source.addImport(normalizedPath, compilationSet.get(normalizedPath));
            }

        }

        return source;
    }

    public static void exit(String message, int code) {
        System.err.println(message);
        System.exit(code);
    }


}
