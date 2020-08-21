/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.client;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Attention! Not complete, annotations do not work yet
 *
 * @author alukin@gmail.com
 */
public class SubscribePathAnnotationProcessor extends AbstractProcessor {

    private Messager messager;
    private Types typesUtil;
    private Elements elementsUtil;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        typesUtil = processingEnv.getTypeUtils();
        elementsUtil = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> arg0, RoundEnvironment arg1) {
        messager.printMessage(Diagnostic.Kind.WARNING, "========== processing subscribePath ============");
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(SubscribePath.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

}
