package org.canve.githubCruncher.mysql
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

object DB {
  // Slick Database configuration
  
  object slick { 
    /* 
     * TODO: improve database connection configuration:
     * 
     * - switch to sbt-build-info plugin for sharing these values with the slick generation task of build.sbt
     * - for concurrency add a connection pool: http://slick.typesafe.com/doc/3.1.1/database.html
     * - for the above TODO's might need to switch database factory methods 
     * - extract the connection details to configuration. but then, how can application.conf share with an sbt task?!?
     * 
     * c.f. http://slick.typesafe.com/doc/3.1.1/database.html, http://slick.typesafe.com/doc/3.1.1/api/index.html#slick.jdbc.JdbcBackend$DatabaseFactoryDef@forConfig(String,Config,Driver):Database 
     *  
     */
    val dbName = "github_crawler" 
    val user = "canve"
    val db = Database.forURL(
      driver = "com.mysql.jdbc.Driver",
      url = s"jdbc:mysql://localhost:3306/$dbName", 
      user = user,
      keepAliveConnection = true)
  }
  
  implicit val session: Session = slick.db.createSession
  
  //slick.db.run()
}
