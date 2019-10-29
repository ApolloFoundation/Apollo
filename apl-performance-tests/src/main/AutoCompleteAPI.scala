
import java.io.FileInputStream
import java.util.Properties

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class AutoCompleteAPI extends Simulation {

	val (apihost,rps, duration) =
		try
		{
			val prop = new Properties()
			prop.load(new FileInputStream("src\\main\\resources\\configuration.properties"))
			(
			prop.getProperty("apihost"),
			new Integer(prop.getProperty("rps")),
			new Integer(prop.getProperty("duration"))
			)
		} catch { case e: Exception =>
			e.printStackTrace()
			sys.exit(1)
		}

	val feeder = csv("src\\main\\resources\\data.csv").random

	val httpProtocol = http
		.baseUrl(apihost)
		.inferHtmlResources()
		.acceptHeader("*/*")

	val scn = scenario("AutoComplete")
  	.feed(feeder)
		.exec(http("Random Request")
			.get("/api/v2/AutoComplete?p=1&s=10&q=${search}"))


	val inject = rampUsers(50000).during(60 minutes)
	setUp(scn.inject(inject)).protocols(httpProtocol)

	//setUp(scn.inject(constantUsersPerSec(rps.toDouble) during (duration minutes))).protocols(httpProtocol)
}