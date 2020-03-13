package part1

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {
    
    implicit val system = ActorSystem("first-principles")
    implicit val materializer = ActorMaterializer()
    
    val source = Source(1 to 10)
    
    val sink = Sink.foreach[Int](println)
    
    val graph = source.to(sink)
//  graph.run()
    
    //  Flows transformam elementos
    val flow = Flow[Int].map(x => x + 1)
    val sourceWithFlow = source.via(flow)
    val flowWithSink = flow.to(sink)

//  sourceWithFlow.to(sink).run()
//  source.to(flowWithSink).run()
//  source.via(flow).to(sink).run()
    
    //  nulls não são permitidos
//  val illegalSource = Source.single[String](null)
//  illegalSource.to(Sink.foreach(println))
//  usar sempre Options
    
    //  vários tipos de source
    val finiteSource = Source.single(1)
    val anotherFiniteSource = Source(List(1, 2, 3))
    val emptySource = Source.empty[Int]
    val infiniteSource = Source(Stream.from(1)) // Akka Stream != collection stream
    
    implicit val ec = system.dispatcher
    val futureSource = Source.fromFuture(Future(42))
    
    //  sinks
    val boringSink = Sink.ignore
    val foreachSink = Sink.foreach[String](println)
    val headSink = Sink.head[Int] // retorna apenas o head do elemento
    val foldSink = Sink.fold[Int, Int](0)((a, b) => a + b)
    
    //  flows - usually maps to collection operators
    val mapFlow = Flow[Int].map(x => x * 2)
    val takeFlow = Flow[Int].take(5)
    // drop, filter
    // NÃO TEM flatmap
    
    //  source -> flow -> flow -> ... -> sink
    val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
//  doubleFlowGraph.run()
    
    val mapSource = Source(1 to 10).map(_ * 2)
//  roda streams diretamente
//  mapSource.runForeach(println)
    
    //  OPERATORS = componentes
    val sourceNames = Source(List("Zó", "Vinicius", "Thais", "Paivinha", "Uehara", "Bardano", "Fernanda"))
    val flowNames = Flow[String].filter(x => x.length > 5).map(_.reverse).take(2)
    val sinkNames = Sink.foreach[String](println)
    
    sourceNames
        .via(flowNames)
        .to(sinkNames).run()
    
}
