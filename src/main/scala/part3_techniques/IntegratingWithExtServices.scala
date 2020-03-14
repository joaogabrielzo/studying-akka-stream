package part3_techniques

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.Future

object IntegratingWithExtServices extends App {
    
    implicit val system = ActorSystem("integrating-with-ext-services")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher // not recommended in production
//    implicit val dispatcher = system.dispatchers.lookup("dedicated-dispatcher")
    
    def genericService[A, B](element: A): Future[B] = ???
    
    //    exemple: simplified PagerDuty
    case class PagerEvent(application: String, description: String, date: Date)
    
    val eventSource = Source(List(
        PagerEvent("AkkaInfra", "Infrastructure broke", new Date),
        PagerEvent("FastDataPipeline", "Illegal elements in the data pipeline", new Date),
        PagerEvent("AkkaInfra", "Service stopped responding", new Date),
        PagerEvent("SuperFrontEnd", "A button doesn't work", new Date)
        ))
    
    object PagerService {
        
        private val engineers = List("Zó", "Outro Zó", "Mais Alguém")
        private val emails = Map(
            "Zó" -> "zo@zo.zo",
            "Outro Zó" -> "outrozo@zo.zo",
            "Mais Alguém" -> "maisalguem@zo.zo"
            )
        
        def processEvent(pagerEvent: PagerEvent) = Future {
            val engineerIndex = (pagerEvent.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
            val engineer = engineers(engineerIndex.toInt)
            val engineerEmail = emails(engineer)
            
            // page the engineer
            println(s"Sending engineer $engineerEmail a high priority notification: $pagerEvent")
            Thread.sleep(1000)
            
            // return the email that was paged
            engineerEmail
        }
    }
    
    val infraEvents = eventSource.filter(_.application == "AkkaInfra")
    val pagedEngineersEmail = infraEvents.mapAsync(parallelism = 4)(event => PagerService.processEvent(event))
    // mapAsync guarantees the relative order of the elements
    // it always run in parallel, but it also always wait for everyone to finish processing
    val pagedEmailSink = Sink.foreach[String](email => println(s"Succesfully sent notification to $email"))

//    pagedEngineersEmail.to(pagedEmailSink).run()
    
    class PagerActor extends Actor with ActorLogging {
        
        private val engineers = List("Zó", "Outro Zó", "Mais Alguém")
        private val emails = Map(
            "Zó" -> "zo@zo.zo",
            "Outro Zó" -> "outrozo@zo.zo",
            "Mais Alguém" -> "maisalguem@zo.zo"
            )
        
        private def processEvent(pagerEvent: PagerEvent) = {
            val engineerIndex = (pagerEvent.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
            val engineer = engineers(engineerIndex.toInt)
            val engineerEmail = emails(engineer)
            
            // page the engineer
            println(s"Sending engineer $engineerEmail a high priority notification: $pagerEvent")
            Thread.sleep(1000)
            
            // return the email that was paged
            engineerEmail
        }
        
        override def receive: Receive = {
            case pagerEvent: PagerEvent =>
                sender() ! processEvent(pagerEvent)
        }
    }
    
    import akka.pattern.ask
    import scala.concurrent.duration._
    
    implicit val timeout = Timeout(3 seconds)
    val pagerActor = system.actorOf(Props[PagerActor], "pager-actor")
    val alternativePagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => (pagerActor ? event)
        .mapTo[String])
    
    alternativePagedEngineerEmails.to(pagedEmailSink).run()
}
