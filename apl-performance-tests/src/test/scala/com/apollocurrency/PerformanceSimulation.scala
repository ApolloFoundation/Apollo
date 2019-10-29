package com.apollocurrency

import com.typesafe.config.ConfigFactory
import collection.JavaConverters._
import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import scala.util.Random
import scalaj.http._

class PerformanceSimulation extends Simulation {

	var peers = ConfigFactory.load("application.conf").getStringList("n1").asScala.toList
	val random = new Random
	val httpProtocol = http.baseUrls(peers)

	before {
		println("Stop/Start forging!")
		for (peer <- peers) {
			try {
			println(peer)
			val response = Http(peer+"/apl")
				.postForm
				.param("requestType","stopForging")
  			.param("adminPassword","1").asString
		   	 println(response.body)
			} catch { case e: Exception =>
				println(e.getMessage)
			}
		 }

		for( i <- 1 to 5) {
				try {
					val peer = peers(
						random.nextInt(peers.length)
					)
					println(peer)
					val response = Http(peer+"/apl")
						.postForm
						.param("requestType","startForging")
						.param("secretPhrase",i.toString).asString
					println(response.body)
					} catch { case e: Exception =>
						println(e.getMessage)
					}
				}
	}


	val scn = scenario("Send Money")
		.exec(http("Get Account Id")
		.post("/apl?requestType=getAccountId&secretPhrase="+random.nextInt(200).toString).check(jsonPath("$.accountRS").find.saveAs("accountRS")))
		.pause(1)
		.exec(session => {
			val transaction = session("accountRS").asOption[String]
			session
		})
		.exec(http("Send Money")
		.post("/apl?" +
			"requestType=sendMoney&" +
			"feeATM=3000000000&" +
			"deadline=1440&" +
			"amountATM="+random.nextInt(2000).toString+"00000000&" +
			"recipient=${accountRS}&secretPhrase="+random.nextInt(200).toString))
		.exec { session =>
			println(session)
			session
		}

	val inject = 	constantUsersPerSec(2) during (60 minutes)
	setUp(scn.inject(inject)).protocols(httpProtocol)
}