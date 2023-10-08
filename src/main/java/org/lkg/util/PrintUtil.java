package org.lkg.util;

import com.intellij.psi.PsiElement;

import java.util.Collection;
import java.util.Objects;
import java.util.logging.Logger;

public class PrintUtil {

    private final static Logger log = Logger.getLogger(PrintUtil.class.getSimpleName());

    public static void print(Collection<? extends PsiElement> psiElement) {
        if (Objects.isNull(psiElement)) {
            return;
        }
        for (Object psi : psiElement) {
            log.info(((PsiElement) psi).getText());
        }
    }

}
