package org.corejava.v2ch08.bytecodeAnnotations;

import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.commons.AdviceAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Adds "entering" logs to all methods of a class that have the LogEntry annotation.
 *
 * @author Cay Horstmann
 * @version 1.20 2016-05-10
 */
public class EntryLogger extends ClassVisitor {
    private String className;

    /**
     * Constructs an EntryLogger that inserts logging into annotated methods of a given class.
     *
     * @param cg the class
     */
    public EntryLogger(ClassWriter writer, String className) {
        super(Opcodes.ASM5, writer);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, methodName, desc, signature, exceptions);
        return new AdviceAdapter(Opcodes.ASM5, mv, access, methodName, desc) {
            private String loggerName;

            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return new AnnotationVisitor(Opcodes.ASM5) {
                    public void visit(String name, Object value) {
                        if (desc.equals("LbytecodeAnnotations/LogEntry;") && name.equals("logger"))
                            loggerName = value.toString();
                    }
                };
            }

            public void onMethodEnter() {
                if (loggerName != null) {
                    visitLdcInsn(loggerName);
                    visitMethodInsn(INVOKESTATIC, "java/util/logging/Logger", "getLogger",
                            "(Ljava/lang/String;)Ljava/util/logging/Logger;", false);
                    visitLdcInsn(className);
                    visitLdcInsn(methodName);
                    visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "entering",
                            "(Ljava/lang/String;Ljava/lang/String;)V", false);
                    loggerName = null;
                }
            }
        };
    }

    /**
     * Adds entry logging code to the given class.
     *
     * @param args the name of the class file to patch
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("USAGE: java bytecodeAnnotations.EntryLogger classfile");
            System.exit(1);
        }
        Path path = Paths.get(args[0]);
        ClassReader reader = new ClassReader(Files.newInputStream(path));
        ClassWriter writer = new ClassWriter(
                ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        EntryLogger entryLogger = new EntryLogger(writer,
                path.toString().replace(".class", "").replaceAll("[/\\\\]", "."));
        reader.accept(entryLogger, ClassReader.EXPAND_FRAMES);
        Files.write(Paths.get(args[0]), writer.toByteArray());
    }
}
