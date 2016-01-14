package org.canve.githubCruncher

trait ImplicitConversions {
   
  /* 
   * Provides implicit conversion evidence for conversion between play and spray json,  
   * the technique is similar but not the same as http://stackoverflow.com/a/34501231/1509695 
   */
  
  implicit object Serialize 
    extends spray.json.JsonFormat[play.api.libs.json.JsValue] {

    import spray.json.{JsonParser => parseToSpray}
    import play.api.libs.json.Json.{parse => parseToPlay} 
    
    def read(jsonObj: spray.json.JsValue): play.api.libs.json.JsValue = parseToPlay(jsonObj.toString)
    def write(obj: play.api.libs.json.JsValue): spray.json.JsValue = parseToSpray(obj.toString)              
  }
  
  /*
   * Example implicit conversion for pipeline serialization
   */
  @deprecated("not currently needed", "")
  implicit object ProjectSerialize 
    extends org.allenai.pipeline.StringSerializable[ProjectDetails] {
    
    def fromString(s: String): org.canve.githubCruncher.ProjectDetails = ???
    def toString(p: ProjectDetails): String = p.toString()
  } 
}