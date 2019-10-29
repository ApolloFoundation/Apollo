import com.typesafe.config.ConfigFactory
import io.gatling.core.scenario.Simulation

class TestBase extends Simulation{
  val peers = ConfigFactory.load("application.conf").getList("peers")
  println(peers) // prints my-app
}
