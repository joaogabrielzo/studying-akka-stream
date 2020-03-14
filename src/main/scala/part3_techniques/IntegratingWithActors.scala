package part3_techniques

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

object IntegratingWithActors extends App {
    
    implicit val system: ActorSystem = ActorSystem("integrating-actors")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    
    class SimpleActor extends Actor with ActorLogging {
        
        override def receive: Receive = {
            case s: String =>
                log.info(s"Just received the string $s")
                sender() ! s"$s$s"
            case n: Int    =>
                log.info(s"Just received the number $n")
                sender() ! (2 * n)
            case _         =>
        }
    }
    
    val simpleActor = system.actorOf(Props[SimpleActor], "simple-actor")
    
    val numbersSource = Source(1 to 10)
    
    /*
        actor as a flow
     */
    implicit val timeout: Timeout = Timeout(2 seconds)
    val actorFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)

//    numbersSource.via(actorFlow).to(Sink.ignore).run()
//    numbersSource.ask[Int](parallelism = 4)(simpleActor).to(Sink.ignore).run()
    // both are equivalent
    
    /*
        actor as a source
     */
    val actorSource = Source.actorRef[Int](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)
    val materializedActorRef = actorSource.to(Sink.foreach[Int](n => println(s"Actor flow got number: $n"))).run()
//    materializedActorRef ! 10

//    terminating the stream
//    materializedActorRef ! akka.actor.Status.Success("Terminated")
    
    //        This is used to control which messages go into the stream, how and when
    
    
    /*
        actor as a destination/sink
        - an init message
        - an ack(nowledge) message
        - a complete message
        - a function to generate a message in case the stream throws an exception
     */
    case object StreamInit
    case object StreamAck
    case object StreamComplete
    case class StreamFail(ex: Throwable)
    
    class DestinationActor extends Actor with ActorLogging {
        
        override def receive: Receive = {
            case StreamInit     =>
                log.info("Stream initialized")
                sender() ! StreamAck
            case StreamComplete =>
                context.stop(self)
                log.info("Stream completed")
            case StreamFail(ex) =>
                log.warning(s"Stream failed with exception: $ex")
            case message        =>
                log.info(s"Message $message has come to it's final resting point.")
                sender() ! StreamAck
            // if there was not a Stream Acknowledge, it would be interpreted as back pressure
            // and the stream would stop
        }
    }
    val destinationActor = system.actorOf(Props[DestinationActor], "destination-actor")
    
    val actorSink = Sink.actorRefWithAck[Int]( // Sink.actorRef() is not recommendo because it's unable to back pressure
                                               destinationActor,
                                               onInitMessage = StreamInit,
                                               onCompleteMessage = StreamComplete,
                                               ackMessage = StreamAck,
                                               onFailureMessage = throwable => StreamFail(throwable)
                                               )
    
    Source(1 to 10).to(actorSink).run()
    
}
