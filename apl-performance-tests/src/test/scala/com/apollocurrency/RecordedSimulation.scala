package com.apollocurrency

import com.typesafe.config.ConfigFactory
import collection.JavaConverters._
import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import scala.util.Random
import scalaj.http._

class RecordedSimulation extends Simulation {

	var peers = ConfigFactory.load("application.conf").getStringList("peers").asScala.toList
	val random = new Random
	var httpProtocol = http.baseUrls(peers)
		.inferHtmlResources()
		.acceptHeader("*/*")

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

		for( i <- 1 to 10) {
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



	val scn = scenario("RecordedSimulation")
		.exec(http("StopForging")
		.post("/apl?requestType=sendmoney&adminPassword=1"))
		.pause(1)

		.exec { session =>
			println(session)
			session
		}
	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}