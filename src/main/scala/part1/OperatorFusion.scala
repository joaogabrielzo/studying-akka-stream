package part1

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {
    
    implicit val system = ActorSystem("operator-fusion")
    implicit val materializer = ActorMaterializer()
    
    val simpleSource = Source(1 to 1000)
    val simpleFlow = Flow[Int].map(_ + 1)
    val simpleFlow2 = Flow[Int].map(_ * 10)
    val simpleSink = Sink.foreach[Int](println)

//    this runs on the same ACTOR - Operator/Component Fusion
//    simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink).run()
    
    /**
      * In the case of Operator Fusion, one single CPU core will run the entire flow
      */
    
    class SimpleActor extends Actor {
        
        override def receive: Receive = {
            case x: Int =>
                // flow operation
                val x2 = x + 1
                val y = x2 * 10
                // sink operation
                println(y)
        }
    }
    val simpleActor = system.actorOf(Props[SimpleActor])
//    (1 to 1000).foreach(simpleActor ! _)
//    equals the operator fusion
    
    //    Complex Flows
    val complexFlow = Flow[Int].map { x =>
        // simulating complex processing
        Thread.sleep(1000)
        x + 1
    }
    val complexFlow2 = Flow[Int].map { x =>
        // simulating complex processing
        Thread.sleep(1000)
        x * 10
    }
//    simpleSource.via(complexFlow).via(complexFlow2).to(simpleSink).run()
    // It's gonna go from 10 to 10, 2 seconds to 2 seconds, because it runs on the same core

//    async boundary
//    simpleSource.via(complexFlow).async // runs on actor 1
//                .via(complexFlow2).async // runs on actor 2
//                .to(simpleSink) // runs on actor 3
//                .run()

//    ordering guarantees
    Source(1 to 3)
        .map(x => {println(s"Flow A: $x"); x}).async
        .map(x => {println(s"Flow B: $x"); x}).async
        .map(x => {println(s"Flow C: $x"); x}).async
        .runWith(Sink.ignore)
    // Independently of the flow order, it will ALWAYS follows the Source order (1, 2, 3)
}