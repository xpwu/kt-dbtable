package com.github.xpwu.ktdbtable_processor

import com.github.xpwu.ktdbtble.annotation.*
import org.jetbrains.annotations.NotNull
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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
  return element.getAnnotation(NotNull::class.java) != null || element.asType().kind.isPrimitive
}

class Processor : AbstractProcessor() {
  internal var logger: Logger = Logger(object : Messager {
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?) {}
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?, p2: Element?) {}
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?, p2: Element?, p3: AnnotationMirror?) {}
    override fun printMessage(p0: Diagnostic.Kind?, p1: CharSequence?, p2: Element?, p3: AnnotationMirror?, p4: AnnotationValue?) {}
  })

  internal var outDirectory = ""

  internal lateinit var processingEnv1: ProcessingEnvironment

  internal var extensionMigs = emptySet<String>().toMutableSet()
  internal var extensionInits = emptySet<String>().toMutableSet()

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    logger = Logger(processingEnv.messager)
    outDirectory = processingEnv.options["kapt.kotlin.generated"]!!
    this.processingEnv1 = processingEnv
  }

  override fun getSupportedSourceVersion(): SourceVersion? {
    return SourceVersion.latestSupported()
  }

  override fun getSupportedAnnotationTypes(): Set<String> {
    val annotations: MutableSet<String> = LinkedHashSet()
    annotations.add(Table::class.qualifiedName!!)
    annotations.add(FromByteArray::class.qualifiedName!!)
    annotations.add(ToByteArray::class.qualifiedName!!)
    return annotations
  }

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    if (annotations.isEmpty()) {
      return true
    }

    this.ktExtension(roundEnv)

    this.processFromTo(roundEnv)

    val tables: MutableSet<TableInfo> = emptySet<TableInfo>().toMutableSet()

    for (ee in roundEnv.getElementsAnnotatedWith(Table::class.java)) {
      // todo:  support java class
      if (isJavaFile(ee as TypeElement)) continue

      if (!processATable(ee, tables)) {
        return true
      }
    }

    val times = emptyMap<String, Int>().toMutableMap()
    for (info in tables) {
      times[info.Name] = (times[info.Name]?:0) + 1
    }

    val imports = emptySet<String>().toMutableSet()
    val importBuilder = StringBuilder()
    val builder = StringBuilder()
    for (info in tables) {
      val im = this.processingEnv!!.elementUtils.getPackageOf(info.Type).toString() + ".*"
      if (!imports.contains(im)) {
        imports.add(im)

        importBuilder.append("""
          import $im
        """.trimIndent())
        importBuilder.append("\n")
      }

      if (times[info.Name] == 1) {
        builder.append("""
        "${info.Name}" to ${info.Type.simpleName}.TableInfo(),
      """.trimIndent())
        builder.append("\n")
      }

      builder.append("""
        "${info.Type.qualifiedName}" to ${info.Type.simpleName}.TableInfo(),
      """.trimIndent())
      builder.append("\n")
    }

    val tableContainer = """
      package com.github.xpwu.ktdbtable
      
      // This file is generated by ktdbtable_processor. DO NOT edit it!
      
      ${importBuilder.toString().align("      ")}
      
      class TableContainerImpl : TableContainer() {
        override val AllTables: Map<String, TableInfo> = 
        mapOf(
          ${builder.toString().align("          ")}
        )
      }
    """.trimIndent()

//    write(outDirectory, "com.github.xpwu.ktdbtable", "TableContainerImpl", tableContainer)
    return true
  }
}

fun Processor.processFromTo(roundEnv: RoundEnvironment) {
  for (e in roundEnv.getElementsAnnotatedWith(FromByteArray::class.java)) {
    if (e.kind != ElementKind.METHOD) {
      continue
    }

    e as ExecutableElement

    // 'ByteArray' .asType().toString() maybe is
    // @org.jetbrains.annotations.NotNull byte[] or @org.jetbrains.annotations.Nullable byte[]
    if (e.parameters.size != 1
      || e.parameters[0].asType().toTypeString() != ByteArray::class.java.canonicalName) {
      continue
    }

    fromByteArray[e.returnType.toString()] =
      this.processingEnv1.elementUtils.getPackageOf(e).qualifiedName.toString() + "." + e.simpleName.toString()
  }

  for (e in roundEnv.getElementsAnnotatedWith(ToByteArray::class.java)) {
    if (e.kind != ElementKind.METHOD) {
      continue
    }

    e as ExecutableElement

    // 'ByteArray' .asType().toString() maybe is
    // @org.jetbrains.annotations.NotNull byte[] or @org.jetbrains.annotations.Nullable byte[]
    if (e.parameters.size != 1
      || e.returnType.toTypeString() != ByteArray::class.java.canonicalName) {
      continue
    }

    toByteArray[e.parameters[0].asType().toString()] =
      this.processingEnv1.elementUtils.getPackageOf(e).qualifiedName.toString() + "." + e.simpleName.toString()
  }
}

fun Processor.ktExtension(roundEnv: RoundEnvironment) {
  /**
   * 扩展会被kapt转变为：
   *
   *

  User.Companion.Migrators()  ===>

  @kotlin.Metadata(mv = {1, 8, 0}, k = 2, )
  public final class UserKt {

  @org.jetbrains.annotations.NotNull()
  public static final java.util.Map<com.github.xpwu.ktdbtable.Version, kotlin.jvm.functions.Function1<com.github.xpwu.ktdbtable.DB<?>, kotlin.Unit>> Migrators(@org.jetbrains.annotations.NotNull()
  com.github.xpwu.ktdbtable.example.User.Companion $this$Migrators) {
  return null;
  }

  @org.jetbrains.annotations.NotNull()
  public static final java.util.Collection<com.github.xpwu.ktdbtable.example.User> Initializer(@org.jetbrains.annotations.NotNull()
  com.github.xpwu.ktdbtable.example.User.Companion $this$Initializer) {
  return null;
  }
  }
   *
   *
   * 如果原文件使用了  @file:JvmName("XXX")  则类名会变为 XXX 而不是原始的类名后面跟上Kt的形式
   *
   */

  // todo 所有的Kotlin类都会有这个注解，可能返回会很多
  for (e in roundEnv.getElementsAnnotatedWith(Metadata::class.java)) {
    val ann= e.getAnnotation(Metadata::class.java)
    // must be file
    if (ann.kind != 2) {
      continue
    }
    if (e.kind != ElementKind.CLASS) {
      continue
    }
    val clazz = e as TypeElement
    if (!clazz.modifiers.containsAll(listOf(Modifier.PUBLIC, Modifier.FINAL))) {
      continue
    }

    for (ee in clazz.enclosedElements) {
      if (ee.kind != ElementKind.METHOD) {
        continue
      }

      val exe = ee as ExecutableElement

      if (exe.simpleName.toString() == "Migrators"
        && exe.parameters.size == 1
        && exe.parameters[0].asType().toString().takeLast(".Companion".length) == ".Companion") {
        this.extensionMigs.add(exe.parameters[0].asType().toString().substringBefore(".Companion"))
      }

      if (exe.simpleName.toString() == "Initializer"
        && exe.parameters.size == 1
        && exe.parameters[0].asType().toString().takeLast(".Companion".length) == ".Companion") {
        this.extensionInits.add(exe.parameters[0].asType().toString().substringBefore(".Companion"))
      }

    }
  }
}

typealias ok = Boolean

fun Processor.processATable(table: TypeElement, tables: MutableSet<TableInfo>): ok {
  val ta = table.getAnnotation(Table::class.java)
  val tableInfo = TableInfo(ta.name, ta.version, table)
  tables.add(tableInfo)

  val names = emptySet<String>().toMutableSet()
  var hasCompanion = false
  for (e in table.enclosedElements) {
    if (e.kind == ElementKind.CLASS) {
      /**
       * companion object  converted by kapt to:
      public static final class Companion {

        private Companion() {
          super();
        }
      }
       */
      if (e.simpleName.toString() == "Companion"
        && e.modifiers.containsAll(arrayListOf(Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC))
      ) {
        hasCompanion = true

        tableInfo.MigInit = this.processMigInit(table)
      }
      continue
    }

    if (e.kind != ElementKind.FIELD) {
      continue
    }

    val field = e as VariableElement
    // @org.jetbrains.annotations.NotNull()
    // public static final xxx.xxx.Companion Companion = null;
    if (field.simpleName.toString() == "Companion") {
      continue
    }

    val columnA = field.getAnnotation(Column::class.java) ?: continue
    var elseT = false

    val fieldTypeStr = field.asType().toTypeString()
    var ty = entity2column[fieldTypeStr]

    if (ty == null
        && fromByteArray[fieldTypeStr] != null
        && toByteArray[fieldTypeStr] != null) {
      ty = Type.BLOB
      elseT = true
    }

    if (ty == null) {
      this.logger.error(e, printTypeError(fieldTypeStr))
      return false
    }

    if (names.contains(columnA.name)) {
      this.logger.error(e, "column name(${columnA.name}) is duplicate")
      return false
    }
    names.add(columnA.name)

    val indexA = field.getAnnotationsByType(Index::class.java)
    tableInfo.Columns.add(
      ColumnInfo(
        ty,
        field.simpleName.toString(),
        columnA,
        isNotNull(field),
        indexA,
        field.asType(),
        elseT
      )
    )

    if (field.modifiers.contains(Modifier.FINAL)) {
      tableInfo.ValFieldName = field.simpleName.toString()
    }
  }

  if (!hasCompanion) {
    this.logger.error(table, "must have 'companion object'")
    return false
  }

  if (tableInfo.Columns.size == 0) {
    this.logger.error(table, "at least one 'column'")
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

fun Processor.processMigInit(table: TypeElement):
  Pair<String, String> {

  val mig = """
    fun ${table.simpleName}.Companion.Migrators(): Map<Version, Migration> {
      return emptyMap()
    }
  """.trimIndent()
  val init = """
    fun ${table.simpleName}.Companion.Initializer(): Collection<${table.simpleName}> {
      return emptyList()
    }
  """.trimIndent()

  val hasMig = this.extensionMigs.contains(table.qualifiedName.toString())
  val hasInit = this.extensionInits.contains(table.qualifiedName.toString())

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
  val packageName = this.processingEnv1.elementUtils.getPackageOf(tableInfo.Type).toString()

  val columns = StringBuilder()
  for (c in tableInfo.Columns) {
    columns.append(c.outField(tableClass)).append("\n\n")
  }

  val fileContent = """
    @file:JvmName("${tableClass}Table")
    package $packageName
    
    // This file is generated by ktdbtable_processor. DO NOT edit it!
    
    import android.content.ContentValues
    import android.database.Cursor
    import com.github.xpwu.ktdbtable.*
    
    @Deprecated("", replaceWith = ReplaceWith("asTable().OriginNameIn"))
    fun ${tableClass}.Companion.TableNameIn(db: com.github.xpwu.ktdbtable.DB<*>): String {
      return ${tableClass}.CreateTableAndReturnNameIn(db)
    }
    
    private fun ${tableClass}.Companion.CreateTableAndReturnNameIn(db: com.github.xpwu.ktdbtable.DB<*>): String {
      val name = db.Name(${tableClass}::class) ?: tableName

      if (!db.Exist(name)) {
        ${tableClass}.CreateTableIn(db)
        db.Open(name)
      } else {
        db.OpenAndUpgrade(${tableClass}::class, ${tableClass}.TableInfo())
      }

      return name
    }
    
    fun ${tableClass}.Companion.asTable(): Table {
      return object : Table {
        override fun OriginNameIn(db: DB<*>): String {
          return ${tableClass}.CreateTableAndReturnNameIn(db)
        }
      }
    }
    
    ${columns.toString().align("    ")}
    
    ${tableInfo.allColumnsFun().align("    ")}
    
    fun ${tableClass}.Companion.Binding(): TableBinding {
      return MakeBinding(${tableClass}::class, tableName)
    }
    
    ${tableInfo.out(logger).align("    ")}
    
    ${tableInfo.MigInit.first.align("    ")}
    
    ${tableInfo.MigInit.second.align("    ")}
    
    ${tableInfo.allIndexFun(logger).align("    ")}
    
    ${tableInfo.toContentValuesFun().align("    ")}
    
    ${tableInfo.hasClass().align("    ")}
    
    ${tableInfo.cursorToFun().align("    ")}
    
    private fun ${tableClass}.Companion.CreateTableIn(db: com.github.xpwu.ktdbtable.DB<*>) {
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
            it.Replace(tableName.noSqlKeyword(), init.ToContentValues())
          }
          it.SetTransactionSuccessful()
        } finally {
          it.EndTransaction()
        }
      }
    }
    
    private const val tableName = "${tableInfo.Name}"
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
  val writer = OutputStreamWriter(Files.newOutputStream(outputPath), StandardCharsets.UTF_8)
  writer.write(fileContent)
  writer.flush()
}
