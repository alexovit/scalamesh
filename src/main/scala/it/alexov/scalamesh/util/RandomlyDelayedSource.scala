package it.alexov.scalamesh.util

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import akka.actor.Cancellable
import akka.stream._
import akka.stream.scaladsl.Source
import akka.stream.stage._

import scala.concurrent.duration._
import scala.util.Random

class RandomlyDelayedSource[A](maxDuration: FiniteDuration, get: () => A) extends GraphStage[SourceShape[A]] {

    val out = Outlet[A]("RandomlyDelayedSource.out")
    val shape = SourceShape.of(out)

    val maxMillis = maxDuration.toMillis.toInt

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new TimerGraphStageLogic(shape) with Cancellable {
        val cancelled = new AtomicBoolean(false)
        val cancelCallback: AtomicReference[Option[AsyncCallback[Unit]]] = new AtomicReference(None)

        var open = false

        override def preStart(): Unit = {
          cancelCallback.set(Some(getAsyncCallback[Unit](_ ⇒ completeStage())))
          if (cancelled.get)
            completeStage()
          else
            scheduleOnce("TickTimer", Random.nextInt(maxMillis).millis)
        }

        def handler = {
          if (!open) {
            val elem: A = get()
            push(out, elem)
            open = true
            scheduleOnce("TickTimer", Random.nextInt(maxMillis).millis)
          }
        }

        setHandler(out, eagerTerminateOutput)

        override protected def onTimer(timerKey: Any): Unit = {
          open = false
          handler
        }

        override def cancel() = {
          val success = !cancelled.getAndSet(true)
          if (success) cancelCallback.get.foreach(_.invoke(()))
          success
        }

        override def isCancelled = cancelled.get

      }
  }

