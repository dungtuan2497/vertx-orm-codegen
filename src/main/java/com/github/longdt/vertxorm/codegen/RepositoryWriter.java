package com.github.longdt.vertxorm.codegen;

import com.github.longdt.vertxorm.annotation.Driver;
import com.github.longdt.vertxorm.repository.CrudRepository;
import com.github.longdt.vertxorm.repository.RowMapper;
import com.github.longdt.vertxorm.repository.base.RowMapperImpl;
import com.squareup.javapoet.*;
import io.vertx.sqlclient.Pool;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.lang.reflect.Type;

import static com.google.auto.common.GeneratedAnnotationSpecs.generatedAnnotationSpec;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;

public class RepositoryWriter {
    private final Filer filer;
    private final Elements elements;
    private final SourceVersion sourceVersion;
    private final Types types;

    RepositoryWriter(ProcessingEnvironment processingEnv) {
        this.filer = processingEnv.getFiler();
        this.elements = processingEnv.getElementUtils();
        this.sourceVersion = processingEnv.getSourceVersion();
        this.types = processingEnv.getTypeUtils();
    }

    void writeRepository(RepositoryDescriptor descriptor)
            throws IOException {
        String factoryName = descriptor.name().className();
        TypeSpec.Builder factory =
                classBuilder(factoryName)
                        .addOriginatingElement(descriptor.repositoryDeclaration().targetType());
        generatedAnnotationSpec(
                elements,
                sourceVersion,
                CodeGenProcessor.class,
                "Do not edit this file")
                .ifPresent(factory::addAnnotation);
        factory.addSuperinterface(descriptor.repositoryDeclaration().targetType().asType());
        factory.addModifiers(PUBLIC);
        addSuperclass(factory, descriptor);
        addConstructor(factory, descriptor);


//        for (TypeMirror implementingType : descriptor.implementingTypes()) {
//            factory.addSuperinterface(TypeName.get(implementingType));
//        }
//
//        ImmutableSet<TypeVariableName> factoryTypeVariables = getFactoryTypeVariables(descriptor);
//
//        addFactoryTypeParameters(factory, factoryTypeVariables);
//        addFactoryMethods(factory, descriptor, factoryTypeVariables);
//        addImplementationMethods(factory, descriptor);
//        addCheckNotNullMethod(factory, descriptor);

        JavaFile.builder(descriptor.name().packageName(), factory.build())
                .skipJavaLangImports(true)
                .build()
                .writeTo(filer);
    }

    private void addConstructor(TypeSpec.Builder factory, RepositoryDescriptor descriptor) {
        MethodSpec.Builder constructor = constructorBuilder();
        constructor.addModifiers(PUBLIC);
        constructor.addParameter(TypeName.get(Pool.class), "pool");
        var entityDeclaration = descriptor.entityDeclaration();
        var entityElement = entityDeclaration.targetType();
        constructor.addCode("var mapperBuilder = $1T.<$2T, $3T>builder($4S, $3T::new)",
                ClassName.get(RowMapper.class),
                entityDeclaration.pkField().javaType(),
                entityElement,
                entityDeclaration.tableName());
        constructor.addCode("\n\t\t.pk($1S, $2T::get$3L, $2T::set$3L, true)",
                entityDeclaration.pkField().fieldName(),
                entityElement,
                toPropertyMethodSuffix(entityDeclaration.pkField().fieldName()));
        for (var fieldEntry : entityDeclaration.fieldsMap().entrySet()) {
            var field = fieldEntry.getValue();
            constructor.addCode("\n\t\t.addField($1S, $2T::get$3L, $2T::set$3L)",
                    field.fieldName(),
                    entityElement,
                    toPropertyMethodSuffix(field.fieldName()));
        }
        constructor.addCode(";\n");
        constructor.addStatement("init(pool, ($1T<$2T, $3T>) mapperBuilder.build())",
                ClassName.get(RowMapperImpl.class),
                entityDeclaration.pkField().javaType(),
                entityElement);
        factory.addMethod(constructor.build());
    }

    private String toPropertyMethodSuffix(String fieldName) {
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    private boolean hasExtend(TypeMirror extendingType) {
        return !types.asElement(extendingType).toString().equals(CrudRepository.class.getTypeName());
    }

    private void addSuperclass(TypeSpec.Builder factory, RepositoryDescriptor descriptor) {
        if (hasExtend(descriptor.extendingType())) {
            factory.superclass(descriptor.extendingType());
            return;
        }
        var supperClass = descriptor.repositoryDeclaration().driver() == Driver.POSTGRESQL ?
                com.github.longdt.vertxorm.repository.postgresql.AbstractCrudRepository.class
                : com.github.longdt.vertxorm.repository.mysql.AbstractCrudRepository.class;
        factory.superclass(ParameterizedTypeName.get(
                ClassName.get(supperClass),
                ClassName.get(descriptor.entityDeclaration().pkField().javaType()),
                ClassName.get(descriptor.entityDeclaration().targetType())
        ));
    }

}
