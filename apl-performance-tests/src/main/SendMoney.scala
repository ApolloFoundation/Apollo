package com.apollocurrency

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class SendMoney extends Simulation {

	val httpProtocol = http
		.baseUrl("http://51.15.37.165:7876")
		.inferHtmlResources()
		.acceptHeader("*/*")
		.acceptEncodingHeader("gzip, deflate")
		.userAgentHeader("python-requests/2.22.0")



	val scn = scenario("SendMoney")
		.exec(http("request_0")
			.post("/apl?requestType=getAccountId&secretPhrase=24"))
		.pause(2)
		.exec(http("request_1")
			.post("/apl?requestType=sendMoney&feeATM=3000000000&deadline=1440&&amountATM=27000000000&recipient=APL-78GD-DM6E-879N-EAWTP&secretPhrase=44"))

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}