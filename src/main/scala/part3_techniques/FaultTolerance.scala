package part3_techniques

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory

object FaultTolerance extends App {
    
    implicit val system = ActorSystem("fault-tolerance", ConfigFactory.load("application.conf"))
    implicit val materializer = ActorMaterializer()
    
    // 1 - logging
    val faultySource = Source(1 to 10).map(e => if (e == 6) throw new RuntimeException else e)
    faultySource.log("trackingElement").to(Sink.ignore).run()
}
