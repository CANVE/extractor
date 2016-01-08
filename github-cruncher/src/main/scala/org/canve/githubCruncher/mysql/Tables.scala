package org.canve.githubCruncher.mysql
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.driver.MySQLDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.driver.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Projects.schema ++ Runs.schema ++ Subprojects.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Projects
   *  @param id Database column id SqlType(INT UNSIGNED), PrimaryKey
   *  @param name Database column name SqlType(CHAR), Length(255,false)
   *  @param `is-simple-sbt-structure` Database column is-simple-sbt-structure SqlType(BIT) */
  case class ProjectsRow(id: Int, name: String, `is-simple-sbt-structure`: Boolean)
  /** GetResult implicit for fetching ProjectsRow objects using plain SQL queries */
  implicit def GetResultProjectsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Boolean]): GR[ProjectsRow] = GR{
    prs => import prs._
    ProjectsRow.tupled((<<[Int], <<[String], <<[Boolean]))
  }
  /** Table description of table projects. Objects of this class serve as prototypes for rows in queries. */
  class Projects(_tableTag: Tag) extends Table[ProjectsRow](_tableTag, "projects") {
    def * = (id, name, `is-simple-sbt-structure`) <> (ProjectsRow.tupled, ProjectsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(name), Rep.Some(`is-simple-sbt-structure`)).shaped.<>({r=>import r._; _1.map(_=> ProjectsRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT UNSIGNED), PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    /** Database column name SqlType(CHAR), Length(255,false) */
    val name: Rep[String] = column[String]("name", O.Length(255,varying=false))
    /** Database column is-simple-sbt-structure SqlType(BIT) */
    val `is-simple-sbt-structure`: Rep[Boolean] = column[Boolean]("is-simple-sbt-structure")
  }
  /** Collection-like TableQuery object for table Projects */
  lazy val Projects = new TableQuery(tag => new Projects(tag))

  /** Entity class storing rows of table Runs
   *  @param id Database column id SqlType(INT UNSIGNED), PrimaryKey
   *  @param startTime Database column start_time SqlType(DATETIME), Default(None)
   *  @param endTime Database column end_time SqlType(DATETIME), Default(None)
   *  @param elapsed Database column elapsed SqlType(INT UNSIGNED), Default(None)
   *  @param finishCircumstances Database column finish_circumstances SqlType(CHAR), Length(64,false), Default(unknown) */
  case class RunsRow(id: Int, startTime: Option[java.sql.Timestamp] = None, endTime: Option[java.sql.Timestamp] = None, elapsed: Option[Int] = None, finishCircumstances: String = "unknown")
  /** GetResult implicit for fetching RunsRow objects using plain SQL queries */
  implicit def GetResultRunsRow(implicit e0: GR[Int], e1: GR[Option[java.sql.Timestamp]], e2: GR[Option[Int]], e3: GR[String]): GR[RunsRow] = GR{
    prs => import prs._
    RunsRow.tupled((<<[Int], <<?[java.sql.Timestamp], <<?[java.sql.Timestamp], <<?[Int], <<[String]))
  }
  /** Table description of table runs. Objects of this class serve as prototypes for rows in queries. */
  class Runs(_tableTag: Tag) extends Table[RunsRow](_tableTag, "runs") {
    def * = (id, startTime, endTime, elapsed, finishCircumstances) <> (RunsRow.tupled, RunsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), startTime, endTime, elapsed, Rep.Some(finishCircumstances)).shaped.<>({r=>import r._; _1.map(_=> RunsRow.tupled((_1.get, _2, _3, _4, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT UNSIGNED), PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    /** Database column start_time SqlType(DATETIME), Default(None) */
    val startTime: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("start_time", O.Default(None))
    /** Database column end_time SqlType(DATETIME), Default(None) */
    val endTime: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("end_time", O.Default(None))
    /** Database column elapsed SqlType(INT UNSIGNED), Default(None) */
    val elapsed: Rep[Option[Int]] = column[Option[Int]]("elapsed", O.Default(None))
    /** Database column finish_circumstances SqlType(CHAR), Length(64,false), Default(unknown) */
    val finishCircumstances: Rep[String] = column[String]("finish_circumstances", O.Length(64,varying=false), O.Default("unknown"))
  }
  /** Collection-like TableQuery object for table Runs */
  lazy val Runs = new TableQuery(tag => new Runs(tag))

  /** Entity class storing rows of table Subprojects
   *  @param id Database column id SqlType(INT UNSIGNED), PrimaryKey
   *  @param name Database column name SqlType(CHAR), Length(255,false), Default(None)
   *  @param scalaVersion Database column scala_version SqlType(CHAR), Length(8,false)
   *  @param handling Database column handling SqlType(CHAR), Length(64,false)
   *  @param symbolsNum Database column symbols_num SqlType(INT UNSIGNED), Default(None)
   *  @param relationsNum Database column relations_num SqlType(INT UNSIGNED), Default(None)
   *  @param elapsed Database column elapsed SqlType(INT UNSIGNED), Default(None)
   *  @param startTime Database column start_time SqlType(DATETIME), Default(None)
   *  @param endTime Database column end_time SqlType(DATETIME), Default(None) */
  case class SubprojectsRow(id: Int, name: Option[String] = None, scalaVersion: String, handling: String, symbolsNum: Option[Int] = None, relationsNum: Option[Int] = None, elapsed: Option[Int] = None, startTime: Option[java.sql.Timestamp] = None, endTime: Option[java.sql.Timestamp] = None)
  /** GetResult implicit for fetching SubprojectsRow objects using plain SQL queries */
  implicit def GetResultSubprojectsRow(implicit e0: GR[Int], e1: GR[Option[String]], e2: GR[String], e3: GR[Option[Int]], e4: GR[Option[java.sql.Timestamp]]): GR[SubprojectsRow] = GR{
    prs => import prs._
    SubprojectsRow.tupled((<<[Int], <<?[String], <<[String], <<[String], <<?[Int], <<?[Int], <<?[Int], <<?[java.sql.Timestamp], <<?[java.sql.Timestamp]))
  }
  /** Table description of table subprojects. Objects of this class serve as prototypes for rows in queries. */
  class Subprojects(_tableTag: Tag) extends Table[SubprojectsRow](_tableTag, "subprojects") {
    def * = (id, name, scalaVersion, handling, symbolsNum, relationsNum, elapsed, startTime, endTime) <> (SubprojectsRow.tupled, SubprojectsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), name, Rep.Some(scalaVersion), Rep.Some(handling), symbolsNum, relationsNum, elapsed, startTime, endTime).shaped.<>({r=>import r._; _1.map(_=> SubprojectsRow.tupled((_1.get, _2, _3.get, _4.get, _5, _6, _7, _8, _9)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT UNSIGNED), PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    /** Database column name SqlType(CHAR), Length(255,false), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Length(255,varying=false), O.Default(None))
    /** Database column scala_version SqlType(CHAR), Length(8,false) */
    val scalaVersion: Rep[String] = column[String]("scala_version", O.Length(8,varying=false))
    /** Database column handling SqlType(CHAR), Length(64,false) */
    val handling: Rep[String] = column[String]("handling", O.Length(64,varying=false))
    /** Database column symbols_num SqlType(INT UNSIGNED), Default(None) */
    val symbolsNum: Rep[Option[Int]] = column[Option[Int]]("symbols_num", O.Default(None))
    /** Database column relations_num SqlType(INT UNSIGNED), Default(None) */
    val relationsNum: Rep[Option[Int]] = column[Option[Int]]("relations_num", O.Default(None))
    /** Database column elapsed SqlType(INT UNSIGNED), Default(None) */
    val elapsed: Rep[Option[Int]] = column[Option[Int]]("elapsed", O.Default(None))
    /** Database column start_time SqlType(DATETIME), Default(None) */
    val startTime: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("start_time", O.Default(None))
    /** Database column end_time SqlType(DATETIME), Default(None) */
    val endTime: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("end_time", O.Default(None))
  }
  /** Collection-like TableQuery object for table Subprojects */
  lazy val Subprojects = new TableQuery(tag => new Subprojects(tag))
}
