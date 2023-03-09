package com.github.xpwu.ktdbtable_processor

import com.github.xpwu.ktdbtble.annotation.Table
//import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.tools.Diagnostic

private fun isJavaFile(element: TypeElement): Boolean {
  val metaDataClass = Class.forName("kotlin.Metadata").asSubclass(Annotation::class.java)
  return element.getAnnotation(metaDataClass) == null
}


//@AutoService(Processor::class)
class Processor : AbstractProcessor() {
  private var logger: Logger = Logger(object :Messager{
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?) {}
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?, p2: Element?) {}
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?, p2: Element?, p3: AnnotationMirror?) {}
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?, p2: Element?, p3: AnnotationMirror?, p4: AnnotationValue?) {}
  })

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    logger = Logger(processingEnv.messager)
  }

  override fun getSupportedSourceVersion(): SourceVersion? {
    return SourceVersion.latestSupported()
  }

  override fun getSupportedAnnotationTypes(): Set<String> {
    val annotations: MutableSet<String> = LinkedHashSet()
    annotations.add(Table::class.qualifiedName!!)
    return annotations
  }

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    if (annotations.isEmpty()) return false


    val tables: MutableSet<TableInfo> = emptySet<TableInfo>().toMutableSet()

    for (ee in roundEnv.getElementsAnnotatedWith(annotations.first())) {
      processATable(ee as TypeElement, tables)

    }
    return false
  }
}

fun processATable(table: TypeElement, tables: MutableSet<TableInfo>) {
  val ta = table.getAnnotation(Table::class.java)
  tables.add(TableInfo(ta.name, ta.version, table))

  val enclosedElements = table.enclosedElements
  for (e in enclosedElements) {
    if (e.kind != ElementKind.FIELD) {
      continue
    }

    val field = e as VariableElement

  }
}
