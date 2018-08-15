package it.alexov.scalamesh

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import caseapp.core.RemainingArgs
import scala.io.StdIn
import caseapp.core.app.CaseApp
import it.alexov.scalamesh.input.RandomInput
import scala.util.Try

object Main extends CaseApp[Options] {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val logger = system.log

  override def run(options: Options, remainingArgs: RemainingArgs): Unit = {
    try {
      val seeds: Seq[(String, Int)] = Try(options.seed.map(in =>
        "(.*):(\\d+)$".r.findFirstMatchIn(in).map(m => m.group(1) -> m.group(2).toInt).get
      )).getOrElse(throw new Exception("Invalid input seeds format"))
      val input = new RandomInput()
      val server = new MeshServer(options.host, options.port, options.file, seeds, input)
      server.run()
      println(">>> Press ENTER to exit <<<")
      StdIn.readLine()
    } catch {
      case e: Throwable =>
        println(s"Error occured: ${e.getMessage}")
    }
  }



}
