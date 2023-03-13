package com.github.xpwu.ktdbtable_processor

import com.github.xpwu.ktdbtble.annotation.Column
import com.github.xpwu.ktdbtble.annotation.Index
import com.github.xpwu.ktdbtble.annotation.Table
import org.jetbrains.annotations.NotNull
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
//import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.tools.Diagnostic
import kotlin.io.path.Path

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
  internal var logger: Logger = Logger(object : Messager {
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?) {}
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?, p2: Element?) {}
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?, p2: Element?, p3: AnnotationMirror?) {}
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?, p2: Element?, p3: AnnotationMirror?, p4: AnnotationValue?) {}
  })

  internal var outDirectory = ""

  internal var processingEnv: ProcessingEnvironment? = null

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    logger = Logger(processingEnv.messager)
    outDirectory = processingEnv.options["kapt.kotlin.generated"]!!
    this.processingEnv = processingEnv
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
        && e.modifiers.containsAll(arrayListOf(Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC))
      ) {
        hasCompanion = true

        tableInfo.MigInit = this.processMigInit(table.simpleName.toString(), clazz)
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
    tableInfo.Columns.add(
      ColumnInfo(
        ty,
        field.simpleName.toString(),
        columnA,
        isNotNull(field),
        indexA
      )
    )
  }
  if (!hasCompanion) {
    this.logger.error(table, "must have 'companion object'")
    return false
  }

  this.outATable(tableInfo)

  return true
}

fun Processor.processMigInit(tableClass: String, companion: TypeElement): Pair<String, String> {
  val mig = """
    fun ${tableClass}.Companion.Migrators(): Map<Version, Migration> {
      return emptyMap()
    }
  """.trimIndent()
  val init = """
    fun ${tableClass}.Companion.Initializer(): Collection<${tableClass}> {
      return emptyList()
    }
  """.trimIndent()

  var hasMig = false
  var hasInit = false

  for (e in companion.enclosedElements) {
    if (e.kind != ElementKind.METHOD) {
      continue
    }

    val m = e as ExecutableElement
    // todo check return type
    if (m.simpleName.toString() == "Migrators" && m.parameters.size == 0) {
      hasMig = true
    }
    // todo check return type
    if (m.simpleName.toString() == "Initializer" && m.parameters.size == 0) {
      hasInit = true
    }
  }

  if (!hasMig && !hasInit) {
    return Pair(mig, init)
  }
  if (!hasMig) {
    return Pair(mig, "")
  }
  if (!hasInit) {
    return Pair("", init)
  }
  return Pair("", "")
}

fun Processor.outATable(tableInfo: TableInfo) {
  val tableClass = tableInfo.Type.simpleName.toString()
  val packageName = this.processingEnv!!.elementUtils.getPackageOf(tableInfo.Type).toString()

  val columns = StringBuilder()
  for (c in tableInfo.Columns) {
    columns.append(c.outField(tableClass))
    columns.append("\n")
  }

  val fileContent = """
    package $packageName
    
    // This file is generated by ktdbtable_processor. DO NOT edit it!
    
    import com.github.xpwu.ktdbtable
    
    fun ${tableClass}.Companion.TableNameIn(db: DB<*>): String {
      val name = db.Name(${tableClass}::class) ?: tableName

      if (!db.Exist(name)) {
        ${tableClass}.CreateTableIn(db)
      } else {
        db.OnOpenAndUpgrade(name, ${tableClass}::class)
      }

      return name
    }
    
    $columns
    
    ${tableInfo.allColumnsFun().align("    ")}
    
    fun ${tableClass}.Companion.Binding(): TableBinding {
      return MakeBinding(${tableClass}::class, tableName)
    }
    
    ${tableInfo.out(logger).align("    ")}
    
    ${tableInfo.MigInit.first.align("    ")}
    
    ${tableInfo.MigInit.second.align("    ")}
    
    ${tableInfo.allIndexFun(logger).align("    ")}
    
    ${tableInfo.toContentValuesFun().align("    ")}
    
    private fun ${tableClass}.Companion.CreateTableIn(db: DB<*>) {
      val tableName = db.Name(${tableClass}::class) ?: tableName
      db.OnlyForInitTable {
        it.BeginTransaction()
        try {
          it.ExecSQL(${tableInfo.sqlForCreating(logger)})
          for ((_, index) in ${tableClass}.allIndex()) {
            it.ExecSQL(index)
          }
          db.SetVersion(tableName, tableVersion)
          for (init in ${tableClass}.Initializer()) {
            it.Replace(tableName, init.ToContentValues())
          }
          it.SetTransactionSuccessful()
        } finally {
          it.EndTransaction()
        }
      }
    }
    
    private const val tableName = ${tableInfo.Name}
    private const val tableVersion = ${tableInfo.Version}
    
  """.trimIndent()

  write(outDirectory, packageName, tableClass, fileContent)
}

fun write(outDirectory: String, packageName: String, fileName: String, fileContent: String) {
  var outputDirectory = Path(outDirectory)
  if (packageName.isNotEmpty()) {
    for (packageComponent in packageName.split('.').dropLastWhile { it.isEmpty() }) {
      outputDirectory = outputDirectory.resolve(packageComponent)
    }
  }

  Files.createDirectories(outputDirectory)

  val outputPath = outputDirectory.resolve("$fileName.kt")
  OutputStreamWriter(Files.newOutputStream(outputPath), StandardCharsets.UTF_8).write(fileContent)
}
