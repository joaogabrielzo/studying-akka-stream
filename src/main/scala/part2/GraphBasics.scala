package part2

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object GraphBasics extends App {
    
    implicit val system = ActorSystem("graph-basics")
    implicit val materializer = ActorMaterializer()
    
    val input = Source(1 to 1000)
    val incrementer = Flow[Int].map(_ + 1)
    val multiplier = Flow[Int].map(_ * 10)
    val output = Sink.foreach[(Int, Int)](println)
    
    // Setting up the fundamentals for the graph
    val graph = RunnableGraph.fromGraph(
        GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
            import GraphDSL.Implicits._
            
            // step 2 - add the necessary components of this graph
            val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator (Single input and 2 outputs)
            val zip = builder.add(Zip[Int, Int]) // fan-in operator (2 inputs and 1 output
            
            // step 3 - tying up the components
            input ~> broadcast // input `feeds into` broadcast
            broadcast.out(0) ~> incrementer ~> zip.in0 // The first element from Broadcast will feed into Incrementer
            // the Incrementer result will feed into Zip's first element
            
            broadcast.out(1) ~> multiplier ~> zip.in1  // The second element from Broadcast will feed into Multiplier
            // the Multiplier result will feed into Zip's second element
            
            zip.out ~> output // The zip will feed into the output
            
            // step 4 - return a closed shape
            ClosedShape // It freezes the Builder and make it imutable
            // returns a tuple (x+1, x*10)
            
        } // graph
        ) // runnable graph

//    graph.run()
    
    /**
      * One source to two sinks
      */
    
    val firstSink = Sink.foreach[Int](x => println(s"First sink: $x"))
    val secondSink = Sink.foreach[Int](x => println(s"Second sink: $x"))
    
    val sourceTwoSinksGraph = RunnableGraph.fromGraph(
        GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
            import GraphDSL.Implicits._
            
            val broadcast = builder.add(Broadcast[Int](2))
            
            input ~> broadcast ~> firstSink // .out(0)
            broadcast ~> secondSink // .out(1)
            // Implicit port numbering
            
            ClosedShape
        }
        )
//    sourceTwoSinksGraph.run()
    
    /**
      * Two inputs
      * Merge and Balance through 2 sinks
      */
    
    import scala.concurrent.duration._
    
    val fastSource = input.throttle(5, 1 second)
    val slowSource = input.throttle(2, 1 second)
    
    val sink1 = Sink.fold[Int, Int](0)((count, _) => {
        println(s"Sink 1 elements received: ${count}")
        count + 1
    })
    val sink2 = Sink.fold[Int, Int](0)((count, _) => {
        println(s"Sink 2 elements received: ${count}")
        count + 1
    })
    
    val balanceGraph = RunnableGraph.fromGraph(
        GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
            import GraphDSL.Implicits._
            
            val merge = builder.add(Merge[Int](2)) // looks like Zip
            val balance = builder.add(Balance[Int](2))
            
            fastSource ~> merge ~> balance ~> sink1
            slowSource ~> merge
            balance ~> sink2
            
            ClosedShape
        }
        )
    balanceGraph.run()
    
}
