/*
 * Copyright (c) 2010-2012 by Ben de Waal. All Rights Reserved.
 *
 * This code is licensed under GPLv3. Other licenses are available directly from the author.
 *
 * No liability is assumed for whatever purpose, intended or unintended.
 */

package com.bronzecastle.iff.core.orm

import java.sql.{SQLException, DriverManager}
import collection.mutable.{HashSet => MutableHashSet}
import com.bronzecastle.iff.core.{Core, UnreachableCodeReachedException}
import com.bronzecastle.iff.core.orm.DatabaseException.UpdateFailedCanRetryException

/**
 * database abstraction
 */
class Database(val name: String) {
  //
  // connection pool
  //
  protected val lock = new Object
  protected val freePool = new MutableHashSet[Connection]()
  protected val usePool = new MutableHashSet[Connection]()

  def getConnection() = {
    lock.synchronized {
      freePool.headOption match {
        case None => {
          // TODO: at some point we need to limit max
          val conn = createConnection()
          usePool.add(conn)
          conn
        }
        case Some(conn) => {
          if (usePool.contains(conn)) throw new RuntimeException("connection already in use")
          // TODO - check that connection is still operational (i.e. SELECT 1)
          freePool.remove(conn)
          usePool.add(conn)
          conn
        }
      }
    }
  }

  def returnConnection(conn: Connection) {
    lock.synchronized {
      if (usePool.contains(conn)) {
        usePool.remove(conn)
        freePool.add(conn)
      } else {
        throw new RuntimeException("Connection not in use")
      }
    }
  }

  def resetConnection(conn: Connection) {
    lock.synchronized {
      if (usePool.contains(conn)) {
        usePool.remove(conn)
        conn.close()
      } else {
        throw new RuntimeException("Connection not in use")
      }
    }
  }

  def shutdown() {
    lock.synchronized {
      if (usePool.size > 0) {
        Core.LOG.warn(usePool.size+" database connections still in use!")
      }
      usePool.foreach(_.close())
      usePool.clear()
      freePool.foreach(_.close())
      freePool.clear()
    }
  }

  Class.forName("org.h2.Driver")
  protected val CONNECTIONSTRING = "jdbc:h2:"+name // use "mem:test" to persist to memory
  protected def createConnection() = {
    val conn = DriverManager.getConnection(CONNECTIONSTRING,"sa","")
    conn.setAutoCommit(false)
    conn.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED)
    new Connection(conn)
  }

  //
  // transactions
  //
  def joinTransaction[T](body: =>T): T = {
    if (Connection() == null)
      doTransaction(body)
    else
      body
  }

  def doTransaction[T](body: =>T): T = {
    if (Connection() != null) throw new RuntimeException("Transaction already open")
    val conn = getConnection()
    var trouble = false
    Connection.setCurrent(conn)
    try {
      while (true) {
        try {
          val t = body
          conn.commitTransaction()
          return t
        } catch {
          case ex: SQLException => {
            if (ex.getErrorCode == 50200) { // timeout
              // can retry transaction
              Core.LOG.info("Transaction timeout (usually due to deadlock)")
              throw new UpdateFailedCanRetryException()
            } else {
              throw ex
            }
          }
        }
      }
    } catch {
      case ex: Exception => {
        trouble = true
        throw ex
      }
    } finally {
      Connection.setCurrent(null)
      if (trouble) {
        try {
          conn.rollbackTransaction()
          returnConnection(conn)
        } catch {
          case ex2: SQLException => {
            // dunno what state connection is in, so just close it
            resetConnection(conn)
          }
        }
      } else {
        returnConnection(conn)
      }
    }
    throw new UnreachableCodeReachedException()
  }
}
