package com.github.xpwu.ktdbtable

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED

class DBQueue<T>(logName: String = "db",
                 tablesBinding: List<TableBinding> = emptyList(),
                 upgrade: Boolean = true,
                 init: suspend ()->DBer<T>){

  lateinit var db: DB<T>
    private set

  private val scope = CoroutineScope(CoroutineName("DBQueue-$logName"))

  internal val queue: Channel<suspend ()->Unit> = Channel(UNLIMITED)

  init {
    // consumer
    scope.launch {
      while (isActive) {
        val exe = queue.receive()
        exe()
      }
    }

    scope.launch {
      queue.send {
        db = DB(init(), tablesBinding, upgrade)
      }
    }
  }

  fun Close() {
    scope.cancel()
  }
}

suspend operator fun <R, T> DBQueue<T>.invoke(block: suspend (DB<T>)->R): R {
  val ch = Channel<R>(1)
  queue.send {
    ch.send(block(this.db))
  }

  return ch.receive()
}

