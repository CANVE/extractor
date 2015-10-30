package performance

/*
 * counters factory & manager that can provide the status of all counters
 */
object Counters {
  private var counters: List[Counter] = List()
  
  // factory method
  def apply(name: String): Counter = {
    val counter = new Counter(name)
    counters = counters :+ counter
    counter
  }
  
  // invokes a supplied reporter function with the counts of all counters 
  def report(func: String => Unit) = {
    val counts = counters.map(counter => (counter.name, counter))
    func(counts.mkString(": ")) 
  }
}

/*
 * a simple (thread-unsafe) counter
 */
class Counter(val name: String) {
  private var count: Long = 0
  def increment = count += 1
  def apply = count
}