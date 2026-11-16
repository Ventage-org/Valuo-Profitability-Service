package pe.ventage.valuo.profitability.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Pruebas automatizadas de arquitectura hexagonal y directivas DDD con ArchUnit.
 * Garantiza que ninguna clase viole las dependencias de capas ni use tareas en segundo plano.
 */
@AnalyzeClasses(packages = "pe.ventage.valuo.profitability", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layered_architecture_is_respected = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Interfaces").definedBy("..interfaces..")
            .layer("Shared").definedBy("..shared..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Interfaces")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Interfaces", "Infrastructure")
            .whereLayer("Interfaces").mayNotBeAccessedByAnyLayer();

    @ArchTest
    static final ArchRule no_scheduled_or_async_annotations_allowed = noClasses()
            .should().beAnnotatedWith("org.springframework.scheduling.annotation.Scheduled")
            .orShould().beAnnotatedWith("org.springframework.scheduling.annotation.Async")
            .orShould().beAnnotatedWith("org.springframework.scheduling.annotation.EnableScheduling")
            .because("Por regla de negocio y directiva de la skill DDD, no se permiten procesos asíncronos ni en segundo plano.");

    @ArchTest
    static final ArchRule domain_and_application_must_not_use_http_clients_directly = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web.client..")
            .because("Solo la capa infrastructure/clients puede usar clientes HTTP para hablar con otros microservicios.");
}
