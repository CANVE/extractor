package org.canve.githubCruncher
import backtype.storm.topology.TopologyBuilder
import backtype.storm.topology.base.BaseRichBolt
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.tuple.{Fields, Tuple}
import backtype.storm.task.OutputCollector
import backtype.storm.task.TopologyContext
import scala.language.implicitConversions
import java.util.{Map => JMap}

/*
 * Not yet in use
 */

class Toplogy {
  val StormTopology: TopologyBuilder = new TopologyBuilder()
}

abstract class StormBolt(val streamToFields: collection.Map[String, List[String]]) extends BaseRichBolt {
    var _context: TopologyContext = _
    var _conf: JMap[_, _] = _

    // A constructor for the common case when you just want to output to the default stream
    def this(outputFields: List[String]) = { this(Map("default" -> outputFields)) }

    def prepare(conf:JMap[_, _], context:TopologyContext, collector:OutputCollector) {
    }

    def declareOutputFields(declarer: OutputFieldsDeclarer) {
      streamToFields foreach { case(stream, fields) =>
        declarer.declareStream(stream, new Fields(fields:_*))
      }
    }
    
    override def cleanup() = {}
}