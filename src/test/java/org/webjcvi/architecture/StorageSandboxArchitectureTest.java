package org.webjcvi.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.webjcvi.storage.FileStorageService;

@AnalyzeClasses(packages = "org.webjcvi", importOptions = ImportOption.DoNotIncludeTests.class)
class StorageSandboxArchitectureTest {

    @ArchTest
    static final ArchRule noDirectNioFilesOutsideStorage = noClasses()
            .that().resideInAPackage("org.webjcvi..")
            .and().doNotHaveFullyQualifiedName(FileStorageService.class.getName())
            .should().dependOnClassesThat().haveFullyQualifiedName("java.nio.file.Files")
            .because("all filesystem access must go through FileStorageService");

    @ArchTest
    static final ArchRule noFileInputStreamOutsideStorage = noClasses()
            .that().resideInAPackage("org.webjcvi..")
            .and().doNotHaveFullyQualifiedName(FileStorageService.class.getName())
            .should().dependOnClassesThat().haveFullyQualifiedName("java.io.FileInputStream");

    @ArchTest
    static final ArchRule noFileOutputStreamOutsideStorage = noClasses()
            .that().resideInAPackage("org.webjcvi..")
            .and().doNotHaveFullyQualifiedName(FileStorageService.class.getName())
            .should().dependOnClassesThat().haveFullyQualifiedName("java.io.FileOutputStream");

    @ArchTest
    static final ArchRule noFileReaderOutsideStorage = noClasses()
            .that().resideInAPackage("org.webjcvi..")
            .and().doNotHaveFullyQualifiedName(FileStorageService.class.getName())
            .should().dependOnClassesThat().haveFullyQualifiedName("java.io.FileReader");

    @ArchTest
    static final ArchRule noFileWriterOutsideStorage = noClasses()
            .that().resideInAPackage("org.webjcvi..")
            .and().doNotHaveFullyQualifiedName(FileStorageService.class.getName())
            .should().dependOnClassesThat().haveFullyQualifiedName("java.io.FileWriter");
}
