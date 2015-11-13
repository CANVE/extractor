import org.canve.simpleGraph._
import utest._
import utest.ExecutionContext.RunNow
import scala.util.{Try, Success, Failure}

object TestRunner extends TestSuite {
  
  val tests = TestSuite {
    "Core" - new Core
    "Algo" - new Algo
    "CoreRemovals" - new CoreRemovals
  }  
  
  //val results = tests.run()
}