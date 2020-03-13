package part2

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source, Zip}
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}

object OpenGraphs extends App {
    
    implicit val system = ActorSystem("open-graphs")
    implicit val materializer = ActorMaterializer()
    
    /*
        A composite source that concatenates 2 sources
        - emits ALL the elements from the first source
        - then ALL the elements from the second source
     */
    
    val firstSource = Source(1 to 10)
    val secondSource = Source(42 to 100)
    
    val sourceGraph = Source.fromGraph(
        GraphDSL.create() { implicit builder =>
            import GraphDSL.Implicits._
            
            val concat = builder.add(Concat[Int](2))
            
            firstSource ~> concat
            secondSource ~> concat
            
            SourceShape(concat.out)
        }
        )
//    sourceGraph.runWith(Sink.foreach(println))
    
    /**
      * Complex Sink
      */
    
    val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1> $x"))
    val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2> $x"))
    
    val sinkGraph = Sink.fromGraph(
        GraphDSL.create() { implicit builder =>
            import GraphDSL.Implicits._
            
            val broadcast = builder.add(Broadcast[Int](2))
            
            broadcast ~> sink1
            broadcast ~> sink2
            
            SinkShape(broadcast.in)
        }
        )
//    firstSource.runWith(sinkGraph)
    
    /**
      * Complex Flow
      */
    
    val incrementer = Flow[Int].map(_ + 1)
    val multiplier = Flow[Int].map(_ * 10)
    
    val flowGraph = Flow.fromGraph(
        GraphDSL.create() { implicit builder =>
            import GraphDSL.Implicits._
            
            val incrementerShape = builder.add(incrementer)
            val multiplierShape = builder.add(multiplier)
            
            incrementerShape ~> multiplierShape
            
            FlowShape(incrementerShape.in, multiplierShape.out)
        }
        )
    firstSource.via(flowGraph).to(Sink.foreach(println)).run()
}
