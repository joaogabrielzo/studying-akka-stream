package part2_graphs

import java.util.Date

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

object MoreOpenGraphs extends App {
    
    implicit val system = ActorSystem("more-open-graphs")
    implicit val materializer = ActorMaterializer()
    
    /**
      * Example: Max3 operator
      * - 3 inputs of type int
      * - the maximum of the 3
      */
    
    val max3StaticGraph = GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        
        val max1 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
        val max2 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
        
        max1.out ~> max2.in0
        
        UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
        
    }
    
    val source1 = Source(1 to 10)
    val source2 = Source((1 to 10).map(_ => 5))
    val source3 = Source((1 to 10).reverse)
    
    val maxSink = Sink.foreach[Int](x => println(s"The max value is $x"))
    
    val max3RunnableGraph = RunnableGraph.fromGraph(
        GraphDSL.create() { implicit builder =>
            import GraphDSL.Implicits._
            
            val max3Shape = builder.add(max3StaticGraph)
            
            source1 ~> max3Shape.in(0)
            source2 ~> max3Shape.in(1)
            source3 ~> max3Shape.in(2)
            
            max3Shape.out ~> maxSink
            
            ClosedShape
        }
        )
//    max3RunnableGraph.run()
    
    /*
       non-uniform fan out shape
       - Processing bank transactions
       - suspicious if amount > 10000
       Streams components for transactions
       - output1: let the transaction go through
       - output2: suspicious transactions ids
    */
    
    case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)
    
    val transSource = Source(List(
        Transaction("123", "Paul", "Jim", 100, new Date),
        Transaction("456", "Jim", "Daniel", 100000, new Date),
        Transaction("789", "Alice", "Jim", 7000, new Date)
        ))
    
    val bankProcessor = Sink.foreach[Transaction](println)
    val suspiciousAnalysisService = Sink.foreach[String](id => println(s"Suspicious Transaction ID: $id"))
    
    val suspiciousTransStaticGraph = GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        
        val broadcast = builder.add(Broadcast[Transaction](2))
        val suspiciousTransFilter = builder.add(Flow[Transaction].filter(trans => trans.amount > 10000))
        val idExtractor = builder.add(Flow[Transaction].map[String](trans => trans.id))
        
        broadcast.out(0) ~> suspiciousTransFilter ~> idExtractor
        
        new FanOutShape2(broadcast.in, broadcast.out(1), idExtractor.out)
    }
    
    val suspiciousTransRunnableGraph = RunnableGraph.fromGraph(
        GraphDSL.create() { implicit builder =>
            import GraphDSL.Implicits._
            
            val suspiciousTransShape = builder.add(suspiciousTransStaticGraph)
            
            transSource ~> suspiciousTransShape.in
            suspiciousTransShape.out0 ~> bankProcessor
            suspiciousTransShape.out1 ~> suspiciousAnalysisService
            
            ClosedShape
        }
        )
    suspiciousTransRunnableGraph.run()
    
}
