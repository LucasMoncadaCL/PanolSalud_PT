package com.panol_project.backendpanol;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .importPackages("com.panol_project.backendpanol");

    @Test
    void modulesMustNotDependOnBootstrapLayer() {
        noClasses()
                .that().resideInAPackage("..modules..")
                .should().dependOnClassesThat().resideInAPackage("..bootstrap..")
                .check(importedClasses);
    }

    @Test
    void sharedMustNotDependOnModules() {
        noClasses()
                .that().resideInAPackage("..shared..")
                .should().dependOnClassesThat().resideInAPackage("..modules..")
                .check(importedClasses);
    }
}
