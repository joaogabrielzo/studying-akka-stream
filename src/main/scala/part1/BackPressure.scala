package part1

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object BackPressure extends App {
    
    implicit val system = ActorSystem("backpressure")
    implicit val materializer = ActorMaterializer()
    
    val fastSource = Source(1 to 100)
    val slowSink = Sink.foreach[Int] { x =>
        // simulate computation
        Thread.sleep(1000)
        println(s"Sink: $x")
    }
//    fastSource.to(slowSink).run() // fusing
    // not backpressure

//    fastSource.async.to(slowSink).run()
    // backpressure
    
    val simpleFlow = Flow[Int].map { x =>
        println(s"incoming: $x")
        x + 1
    }
//    fastSource.async
//              .via(simpleFlow).async
//              .to(slowSink)
//              .run()
    
    /**
      * reactions to backpressure:
      * try to slowdown the source if possible
      * buffer elements until there's more demand
      * drop down elements from the buffer if it overflows
      * - Flow buffers 16 elements(default), Sink consumes 8, Flow buffers 8+ elements, Sink consumes 8,...
      */
    
    val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)
    // It will buffer only 10 numbers and drop the firsts
//    fastSource.async
//              .via(bufferedFlow).async
//              .to(slowSink)
//              .run()
    /*
        The Sink will buffer the first 16 numbers
        The Flow will buffer 17-26
        Sink is so slow that Flow will start dropping the numbers
        Leaving only the last 10 for Sink to compute
     */
    
    /**
      * Overflow Strategies:
      * - .drophead = drop oldest
      * - .droptail = drop newest
      * - .dropnew = drop the exact element to be added = keeps the buffer
      * - drop the entire buffer
      * - backpressure signal
      * - fail
      */

//    throttling
    
    import scala.concurrent.duration._
    
    fastSource.throttle(12, 1 second).runWith(Sink.foreach(println))
    
}
