package com.github.xpwu.ktdbtable_processor

import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.Index
import com.github.xpwu.ktdbtble.annotation.Table
import org.jetbrains.annotations.NotNull
//import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.tools.Diagnostic

private fun isJavaFile(element: TypeElement): Boolean {
  val metaDataClass = Class.forName("kotlin.Metadata").asSubclass(Annotation::class.java)
  return element.getAnnotation(metaDataClass) == null
}

private fun isNotNull(element: Element): Boolean {
  val metaDataClass = NotNull::class.java.asSubclass(Annotation::class.java)
  return element.getAnnotation(metaDataClass) != null
}


//@AutoService(Processor::class)
class Processor : AbstractProcessor() {
  internal var logger: Logger = Logger(object :Messager{
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
    if (annotations.isEmpty()) {
      // todo print table info
      return true
    }

    val tables: MutableSet<TableInfo> = emptySet<TableInfo>().toMutableSet()

    for (ee in roundEnv.getElementsAnnotatedWith(annotations.first())) {
      // todo:  support java class
      if (isJavaFile(annotations.first())) continue

       if (!processATable(ee as TypeElement, tables)) {
         return true
       }
    }
    return true
  }
}

typealias ok = Boolean

fun Processor.processATable(table: TypeElement, tables: MutableSet<TableInfo>): ok {
  val ta = table.getAnnotation(Table::class.java)
  val tableInfo = TableInfo(ta.name, ta.version, table)
  tables.add(tableInfo)

//  val enclosedElements =
  var hasCompanion = false
  for (e in table.enclosedElements) {
    if (e.kind != ElementKind.FIELD) {
      continue
    }
    if (e.kind == ElementKind.CLASS) {
      /**
       * companion object  converted by kapt to:
       public static final class Companion {

        private Companion() {
          super();
        }
      }
       */
      val clazz = e as TypeElement
      if (e.simpleName.toString() == "Companion"
        && e.modifiers.containsAll(arrayListOf(Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC))) {
        hasCompanion = true
      }
    }

    val field = e as VariableElement
    if (field.simpleName.toString() == "Companion") {
      continue
    }
    val columnA = field.getAnnotation(Column::class.java) ?: continue
    val ty = entity2column[field.asType().toString()]
    if (ty == null) {
      this.logger.error(e, printTypeError(e.asType().toString()))
      return false
    }

    val indexA = field.getAnnotationsByType(Index::class.java)
    tableInfo.Columns.add(ColumnInfo(ty, field.simpleName.toString(), columnA, isNotNull(field), indexA))
  }
  if (!hasCompanion) {
    this.logger.error(table, "must have 'companion object'")
    return false
  }

  // out
  return true
}
