package com.igarciamen.messages;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.igarciamen.messages");
    }

    @Test
    void controllers_enPaqueteControllerYAcabanEnController() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().resideInAPackage("..controller..")
                .andShould().haveSimpleNameEndingWith("Controller");
        rule.check(classes);
    }

    @Test
    void services_enPaqueteServiceYAcabanEnService() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
                .should().resideInAPackage("..service..")
                .andShould().haveSimpleNameEndingWith("Service");
        rule.check(classes);
    }

    @Test
    void repositorios_enPaqueteRepository() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .should().resideInAPackage("..repository..");
        rule.check(classes);
    }

    @Test
    void controllers_noUsanRepositorioDirecto() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..");
        rule.check(classes);
    }

    @Test
    void modelo_noDependeDeControladores() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..model..")
                .should().dependOnClassesThat().resideInAPackage("..controller..");
        rule.check(classes);
    }

    @Test
    void dtos_enPaqueteDto() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Dto")
                .should().resideInAPackage("..dto..");
        rule.check(classes);
    }
}
