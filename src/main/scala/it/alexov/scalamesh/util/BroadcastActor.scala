package it.alexov.scalamesh.util

import akka.actor.{Actor, ActorRef, Props}

class BroadcastActor extends Actor {

  import BroadcastActor._

  @volatile var routees = Set[ActorRef]()

  def receive = {
    case Add(ref) => routees = routees + ref
    case Remove(ref) => routees = routees - ref
    case msg => routees.foreach(r => r forward msg)
  }
}

object BroadcastActor {
  def actorName: String = "broadCast"
  def props = Props(classOf[BroadcastActor])

  case class Add(ref: ActorRef)
  case class Remove(ref: ActorRef)
}
