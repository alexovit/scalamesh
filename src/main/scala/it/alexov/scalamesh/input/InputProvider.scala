package it.alexov.scalamesh.input

import akka.stream.scaladsl.Source
import it.alexov.scalamesh.DataMessage

trait InputProvider {
  def inputSource: Source[DataMessage, _]
}
