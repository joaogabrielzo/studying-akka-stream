package part3_techniques

import java.util.Date

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}

object AdvancedBackpressure extends App {
    
    implicit val system = ActorSystem("advanced-backpressure")
    implicit val materializer = ActorMaterializer()
    
    // control backpressure
    val controlledFlow = Flow[Int].map(_ * 2).buffer(10, OverflowStrategy.dropHead)
    
    case class PagerEvent(description: String, date: Date, nInstances: Int = 1)
    case class Notification(email: String, pagerEvent: PagerEvent)
    
    val events = List(
        PagerEvent("Service discovery failed", new Date),
        PagerEvent("Illegal elements in the data pipeline", new Date),
        PagerEvent("Number of HTTP 500 spiked", new Date),
        PagerEvent("A serviced stopped working", new Date)
        )
    val eventSource = Source(events)
    
    val oncallEngineer = "zo@zo.zo" // fast service for fetching oncall engineers
    
    def sendEmail(notification: Notification): Unit =
        notification.pagerEvent match {
            case PagerEvent(s, date, nInstances) =>
                println(s"Dear ${notification.email.split("@")(0)}, you have a notification: $s @ $date")
            // ^ this would send an email
        }
    
    val notificationSink = Flow[PagerEvent].map(event => Notification(oncallEngineer, event))
                                           .to(Sink.foreach[Notification](sendEmail))

//    eventSource.to(notificationSink).run()
    
    /*
        un-backpressured source
    
        If the source can't be backpressured for some reason, the solution is to aggregate the incoming events
        and create one single notification when receive demand on the sink
        
        Instead of buffering and then receiving 3 single notifications, stack them and receive one giant notification
        with 3 events using method Conflate.
     */
    
    def sendEmailSlow(notification: Notification): Unit = {
        Thread.sleep(1000)
        notification.pagerEvent match {
            case PagerEvent(s, date, n) =>
                println(s"Dear ${notification.email.split("@")(0)}, you have a notification: $s @ $date")
            // ^ this would send an email
        }
    }
    
    val aggregateNotificationFlow = Flow[PagerEvent]
        .conflate((event1, event2) => {
            val nInstances = event1.nInstances + event2.nInstances
            PagerEvent(s"You have $nInstances events that require your attention", new Date, nInstances)
        }).map(resultingEvent => Notification(oncallEngineer, resultingEvent))
//    Conflate acts like Fold, combining elements but emits the result only when the downstream sends the demand

//    eventSource.via(aggregateNotificationFlow).async.to(Sink.foreach[Notification](sendEmailSlow)).run()
    // alternative to backpressure
    
    /*
        Slow producers: extrapolate/expand
     */
    
    import scala.concurrent.duration._
    
    val slowSource = Source(Stream.from(1)).throttle(1, 1 second)
    val hungrySink = Sink.foreach[Int](println)
    
    val extrapolator = Flow[Int].extrapolate(element => Iterator.from(element))
    // Extrapolate takes an element and returns an iterator, so in the case of an unmet demand from downstream
    // this iterator will start producing elements in artificially feed
    val repeater = Flow[Int].extrapolate(element => Iterator.continually(element))
    // This will repeat the element until the next one arrive
    val expander = Flow[Int].expand(element => Iterator.from(element))
    // Works like Extrapolate, but instead of only producing elements when unmet demand, it creates at all times
    
    slowSource.via(expander).to(hungrySink).run()
    
}
