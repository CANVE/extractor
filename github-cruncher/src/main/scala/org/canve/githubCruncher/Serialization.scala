package org.canve.githubCruncher

trait ImplicitPersistenceSerializations {
   
  implicit object Serialize 
    extends spray.json.JsonFormat[play.api.libs.json.JsValue] {

    import spray.json.{JsonParser => parseToSpray}
    import play.api.libs.json.Json.{parse => parseToPlay} 
    
    def read(jsonObj: spray.json.JsValue): play.api.libs.json.JsValue = parseToPlay(jsonObj.toString)
    def write(obj: play.api.libs.json.JsValue): spray.json.JsValue = parseToSpray(obj.toString)   
  }
  
  @deprecated("not currently in use", "")
  implicit object ProjectSerialize 
    extends org.allenai.pipeline.StringSerializable[Project] {
    
    def fromString(s: String): org.canve.githubCruncher.Project = ???
    def toString(p: Project): String = p.toString()
  } 
}