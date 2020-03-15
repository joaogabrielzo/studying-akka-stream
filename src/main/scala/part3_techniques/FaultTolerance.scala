package part3_techniques

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.Supervision.{Resume, Stop}
import akka.stream.{ActorAttributes, ActorMaterializer}
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import com.typesafe.config.ConfigFactory

import scala.util.Random

object FaultTolerance extends App {
    
    implicit val system = ActorSystem("fault-tolerance", ConfigFactory.load())
    implicit val materializer = ActorMaterializer()
    
    // 1 - logging
    val faultySource = Source(1 to 10).map(e => if (e == 6) throw new RuntimeException else e)
//    faultySource.log("trackingElement").to(Sink.ignore).run()
    
    // 2 - gracefully terminating a stream
    faultySource.recover {
        case _: RuntimeException => Int.MinValue
    }
                .log("gracefulSource")
                .to(Sink.ignore)
//                .run()
    
    // 3 - recover with another stream
    faultySource.recoverWithRetries(3, {
        case _: RuntimeException => Source(90 to 99)
    })
                .log("recoverWithRetries")
                .to(Sink.foreach[Int](println))
//                .run()
    
    // 4 - backoff supervisors
    
    import scala.concurrent.duration._
    
    val restartSource = RestartSource.onFailuresWithBackoff(
        minBackoff = 1 second,
        maxBackoff = 30 seconds,
        randomFactor = 0.2
        )(() => {
        val randomNumber = new Random().nextInt(20)
        Source(1 to 20).map(elem => if (elem == randomNumber) throw new RuntimeException else elem)
    })
                                     .log("restartBackoff")
                                     .to(Sink.foreach[Int](println))
//                                     .run()
    
    // 5 - supervision strategy
    val numbers = Source(1 to 20).map(x => if (x == 13) throw new RuntimeException("PT") else x).log("supervision")
    val supervisedNumbers = numbers.withAttributes(ActorAttributes.supervisionStrategy {
        /*
            Resume - skips the faulty element and continues
            Stop - stop the stream
            Restart - do the same as Resume + clears internal state
         */
        case _: RuntimeException => Resume
        case _                   => Stop
    })
    supervisedNumbers.to(Sink.foreach[Int](println)).run()
    
}
