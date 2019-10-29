package com.apollocurrency

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import scala.util.Random

class RecordedSimulation extends Simulation {

	var peers = ConfigFactory.load("application.conf").getStringList("peers")
	println(peers.get(0).getClass) // prints my-app
	val random = new Random
	println(peers.get(random.nextInt(peers.size())))

	var httpProtocol = http.baseUrls(peers)
		.inferHtmlResources()
		.acceptHeader("*/*")
	//"http://localhost:7876"

	foreach("peers", "peer") {
		println("++++++++++++++++++++++++++++++++++++++++++++++++")
		println("http://${peer}:7876")
		exec(http("get google inside during")
			.get("http://${peer}:7876")
			.check(status.is(200)))
	}



	val scn = scenario("RecordedSimulation")
		.exec(http("StopForging")
		.post("/apl?requestType=stopForging&adminPassword=1"))
		.pause(1)

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}