package performance
import play.api.libs.json._
import org.canve.compilerPlugin.MetaDataLog
import org.canve.logging.loggers.JsonLogger
import org.canve.compilerPlugin.PluginArgs

/*
 * counters factory & manager that can provide the status of all counters
 */
object Counter {
  private var counters: List[Counter] = List()
  
  // factory method
  def apply(name: String): Counter = {
    val counter = new Counter(name)
    counters = counters :+ counter
    counter
  }
  
  // invokes a supplied reporter function with the counts of all counters 
  def report = {
    lazy val log = new JsonLogger(PluginArgs.outputPath.performanceDir.toString)
    def PerformanceLog(path: String, jsonObj: JsValue) = log(path, jsonObj) 
    
    counters.foreach(counter => PerformanceLog(counter.name, Json.parse(s""" { "count" : "${counter.count}" } """)))
  }
}

/*
 * a simple (thread-unsafe) counter
 */
class Counter(val name: String) {
  private var count: Long = 0
  def increment = count += 1
  def get = count
}