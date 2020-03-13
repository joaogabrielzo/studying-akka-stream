package part2_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Source}

object GraphCycles extends App {

    implicit val system: ActorSystem = ActorSystem("graph-cycles")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val accelerator = GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val sourceShape = builder.add(Source(1 to 100))
        val mergeShape = builder.add(Merge[Int](2))
        val incrementerShape = builder.add(Flow[Int].map { x =>
            println(s"Accelerating $x")
            x + 1
        })

        sourceShape ~> mergeShape ~> incrementerShape
                       mergeShape <~ incrementerShape

        ClosedShape
    }

//    RunnableGraph.fromGraph(accelerator).run()
    // even though we have a 1 to 100 source, as we never get rid of any number the buffer gets full
    // then all the flow gets backpressured resulting on a Graph Cycle Deadlock

    /*
        Solution 1: MergePreferred
        Version of Merge that has a preferential input and whenever a new element is available on that input
        it will pass that element on regardless anything.
     */

    val acceleratorPreferred = GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val sourceShape = builder.add(Source(1 to 100))
        val mergeShape = builder.add(MergePreferred[Int](1))
        val incrementerShape = builder.add(Flow[Int].map { x =>
            println(s"Accelerating $x")
            x + 1
        })

        sourceShape ~> mergeShape ~> incrementerShape
        mergeShape.preferred <~ incrementerShape

        ClosedShape
    }

//    RunnableGraph.fromGraph(acceleratorPreferred).run()

    /*
        Solution 2: Buffers
     */

    val acceleratorBuffer = GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val sourceShape = builder.add(Source(1 to 100))
        val mergeShape = builder.add(Merge[Int](2))
        val repeaterShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map { x =>
            println(s"Accelerating $x")
            Thread.sleep(100)
            x
        })

        sourceShape ~> mergeShape ~> repeaterShape
        mergeShape <~ repeaterShape

        ClosedShape
    }

    RunnableGraph.fromGraph(acceleratorBuffer).run()

    /*
        Cycles risk deadlocking
        - add bounds to the elements in the cycly to avoid
     */

}
