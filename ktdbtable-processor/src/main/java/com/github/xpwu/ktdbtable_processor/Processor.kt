package com.github.xpwu.ktdbtable_processor

import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.Index
import com.github.xpwu.ktdbtble.annotation.Table
import com.google.auto.service.AutoService
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
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

  override fun getSupportedAnnotationTypes(): Set<String> {
    val annotations: MutableSet<String> = LinkedHashSet()
    annotations.add(Column::class.qualifiedName as String)
    annotations.add(Index::class.qualifiedName as String)
    annotations.add(Table::class.qualifiedName as String)
    return annotations
  }

  override fun process(
    annotations: MutableSet<out TypeElement>?,
    roundEnv: RoundEnvironment?
  ): Boolean {
    TODO("Not yet implemented")
  }
}