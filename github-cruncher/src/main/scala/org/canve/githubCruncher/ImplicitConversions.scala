package org.canve.githubCruncher
import scala.reflect.io.Directory
import org.canve.shared.DataWithLog

trait ImplicitConversions {
   
  /* 
   * Provides implicit conversion evidence for conversion between play and spray json,  
   * the technique is similar but not identical to http://stackoverflow.com/a/34501231/1509695 ―
   * it makes play's JsValue eligible for spray's generic json conversion trait, through 
   * concrete `read` and `write` methods. 
   */
  
  implicit object PlayJsonConversion 
    extends spray.json.JsonFormat[play.api.libs.json.JsValue] {
    
    import spray.json.{JsonParser => parseToSpray}
    import play.api.libs.json.Json.{parse => parseToPlay} 
    
    def read(jsonObj: spray.json.JsValue)      : play.api.libs.json.JsValue = parseToPlay(jsonObj.toString)
        def write(obj: play.api.libs.json.JsValue) : spray.json.JsValue         = parseToSpray(obj.toString)              
  }
  
  /*
   * Implicit conversions for pipeline serialization of a `Directory` 
   */
  implicit object DataWithLogConversion 
    extends org.allenai.pipeline.StringSerializable[DataWithLog] {
    
    def fromString(s: String): DataWithLog = new DataWithLog(s)
    def toString(dataWithLog: DataWithLog): String = dataWithLog.outputRootDir              
  }
  
  /*
   * Example implicit conversions for pipeline serialization.
   * Same technique as above ― it makes a custom type `ProjectDetails` a StringSerliazable.
   */
  
  @deprecated("", "")
  implicit object ProjectDetails 
    extends org.allenai.pipeline.StringSerializable[ProjectDetails] {
    
    def fromString(s: String): org.canve.githubCruncher.ProjectDetails = ???
    def toString(p: ProjectDetails): String = p.toString
  } 
}