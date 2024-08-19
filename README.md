# kt-dbtable

解除库与表的耦合度；表可以自己维护自己的升级；表自动升级；使用原有的数据库接口；协程接口

## 0、代码库的引用
1、jitpack目前编译有问题，暂未发布为依赖包，暂时直接在代码中导入module的方式使用   
2、build.grable加入 
```
apply plugin: 'kotlin-kapt'


dependencies {
  implementation project(path: ':ktdbtable')
  kapt project(path: ':ktdbtable-processor')
}

```


## 1、基本使用  
1、创建一个DB实例，首先使用底层的数据库实现DBer接口(同时实现DBInner与UnderlyingDBer接口)，然后使用DB的构造
函数创建即可。在ktdbtable中已经实现了 SQLiteDatabase 与 SupportSQLiteDatabase 的 adapter (满足DBer接口)，
如果使用的 其他数据库接口，可参照写一个adapter即可；   
2、用 @Table @Column @Index 注解对表做注解处理，只支持kotlin，不支持java，
类中必须声明companion object，参见testcase中的使用；   
3、编译后，会生成新的方法，主要有 TableNameIn(db)、各个列的名称；   
4、在所有数据库的操作中，使用 TableNameIn(db) 方法获取表名即可；   
5、通过DB实例的UnderlyingDB属性获取到底层数据库对象，调用相关方法操作数据库即可。   

## 2、升级   
1、添加列：直接在代码中添加属性即可  
2、删除列：表的代码里面删除属性即可   
3、添加索引：直接用@Index指定新的索引即可   
4、改列名：修改属性名而不修改真实字段名即可  
5、创建新表：定义表的类，在使用此表的地方会自动创建，表名必须通过TableNameIn(db)获取   
6、其他复杂升级：在表的类代码中实现 fun xxx.Companion.Migrators(): Map<Version, Migration>
同时在@Table中指定新的版本号  
### 注意：ALTER xxx ADD COLUMN xxx 有如下要求 [alter](https://www.sqlite.org/lang_altertable.html)
1. The column may not have a PRIMARY KEY or UNIQUE constraint.
2. The column may not have a default value of CURRENT_TIME, CURRENT_DATE, CURRENT_TIMESTAMP, or an expression in parentheses.
3. If a NOT NULL constraint is specified, then the column must have a default value other than NULL.
4. If foreign key constraints are enabled and a column with a REFERENCES clause is added, the column must have a default value of NULL.
5. The column may not be GENERATED ALWAYS ... STORED, though VIRTUAL columns are allowed.
* 其中：UNIQUE 在本库中是通过CREATE UNIQUE INDEX 而添加，所有本库中支持UNIQUE列的添加


## 3、库与表的绑定  
1、使用 xxx.TableNameIn(db) 方法时，会自动在db库中创建xxx表，并创建索引；   
2、表与库是完全分离的，一个表的定义可以在多个不同的库中绑定，如果同一个
库中的表出现名字冲突，可以在创建DB的时候，直接绑定此表并指定此表在此库中的名字


## 4、库的其他升级  
1、库的所有操作都直接使用底层数据库的接口，所有必须要库升级时，按照原有升级策略实施就行，
ktdbtable不影响原有的升级   
2、如果希望ktdbtable的升级也有进度提示，可以在创建DB时，最后一个参数传入false，然后手动
调用DB的Upgrade() 方法

## 5、表的初始化
在建表的时候，如果需要指定初始插入的数据，可以实现 fun Xxx.Companion.Initializer(): Collection<Xxx>
方法即可，类定义的初始化值并不会直接影响表的初始化，除非通过实现Xxx.Companion.Initializer()方法明确的指定用
类定义的初始化值初始化表

## 6、队列，协程接口
使用DBQueue 而不直接使用 DB 即可使用队列串行执行数据库操作，异步的返回使用协程方式。

