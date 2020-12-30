package com.goticks

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.goticks.TicketSeller.{Add, Buy, Ticket, Tickets}
import org.scalatest.wordspec.AnyWordSpecLike

class TicketSellerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "The TicketSeller" should {
    "Sell tickets until they are sold out" in {

      def mkTickets      = (1 to 10).map(i => Ticket(i)).toVector
      val event          = "RHCP"
      val probe          = createTestProbe[TicketSeller.Tickets]()
      val ticketingActor = spawn(TicketSeller(event))

      ticketingActor ! Add(mkTickets)
      ticketingActor ! Buy(1, probe.ref)

      probe.expectMessage(Tickets(event, Vector(Ticket(1))))

      val nrs = (2 to 10)
      nrs.foreach(_ => ticketingActor ! Buy(1, probe.ref))

      val tickets = probe.receiveMessages(9)
      tickets.zip(nrs).foreach { case (Tickets(event, Vector(Ticket(id))), ix) => id should be(ix) }

      ticketingActor ! Buy(1, probe.ref)
      probe.expectMessage(Tickets(event))
    }

    "Sell tickets in batches until they are sold out" in {

      val firstBatchSize = 10

      def mkTickets = (1 to (10 * firstBatchSize)).map(i => Ticket(i)).toVector

      val probe          = createTestProbe[TicketSeller.Tickets]()
      val event          = "Madlib"
      val ticketingActor = spawn(TicketSeller(event))

      ticketingActor ! Add(mkTickets)
      ticketingActor ! Buy(firstBatchSize, probe.ref)
      val bought = (1 to firstBatchSize).map(Ticket).toVector

      probe.expectMessage(Tickets(event, bought))

      val secondBatchSize = 5
      val nrBatches       = 18

      val batches = (1 to nrBatches * secondBatchSize)
      batches.foreach(_ => ticketingActor ! Buy(secondBatchSize, probe.ref))

      val tickets = probe.receiveMessages(nrBatches)

      tickets.zip(batches).foreach { case (Tickets(event, bought), ix) =>
        bought.size should equal(secondBatchSize)
        val last  = ix * secondBatchSize + firstBatchSize
        val first = ix * secondBatchSize + firstBatchSize - (secondBatchSize - 1)
        bought.map(_.id) should equal((first to last).toVector)
      }

      ticketingActor ! Buy(1, probe.ref)
      probe.expectMessage(Tickets(event))

      ticketingActor ! Buy(10, probe.ref)
      probe.expectMessage(Tickets(event))
    }
  }
}
