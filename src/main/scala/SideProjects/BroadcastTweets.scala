package SideProjects

import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import SideProjects._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}

object BroadcastTweets extends App {

    implicit val system = ActorSystem("broadcast-tweets")
    implicit val materializer = ActorMaterializer()

    val sinkAuthor = Sink.foreach[Author](x => println(s"Author: $x"))
    val sinkHashtag = Sink.foreach[Hashtag](y => println(s"Hashtag: $y"))

    val tweets: Source[Tweet, NotUsed] = Source(
        Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
            Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
            Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
            Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
            Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
            Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
            Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
            Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
            Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
            Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
            Nil)

    val broadcastTweets = RunnableGraph.fromGraph(
        GraphDSL.create() { implicit builder =>
            import GraphDSL.Implicits._

            val broadcast = builder.add(Broadcast[Tweet](2))

            tweets ~> broadcast.in

            broadcast.out(0) ~> Flow[Tweet].map(_.author) ~> sinkAuthor
            broadcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> sinkHashtag

            ClosedShape
        }
    )
    broadcastTweets.run()

}
