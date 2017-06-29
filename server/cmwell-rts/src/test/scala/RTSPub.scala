/**
  * Copyright 2015 Thomson Reuters
  *
  * Licensed under the Apache License, Version 2.0 (the “License”); you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
  * an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */


import akka.actor.ActorSystem
import akka.serialization.{Serializer, SerializationExtension, Serialization}
import cmwell.domain._
import cmwell.domain.FString
import cmwell.rts.{Rule, Publisher}
import k.grid.{GridConnection, Grid}
import scala.io.StdIn

/**
 * Created by markz on 7/10/14.
 */
object RTSPub extends App {
  // java -Done-jar.main.class=RTSPub -jar cmwell-rts-ng_2.10-1.2.1-SNAPSHOT-one-jar.jar
  val ip : String = "127.0.0.1"
  // start grid
  //Grid.roles = Set("publisher")
  Grid.setGridConnection(GridConnection(memberName = "rts", hostName = ip, seeds = Set("127.0.0.1:2551"), port = 0))
  Grid.joinClient
  //Grid.join(Set("127.0.0.1"),Set("subscriber"))
  Publisher.init
  println("Press enter publisher.")
  StdIn.readLine()
  println("-----------------------------------------")
  (1 to 9).foreach{ i =>
    val m : Map[String , Set[FieldValue]]= Map("name" -> Set(FString("gal"), FString("yoav")), "types" -> Set(FString("123"), FInt(123)))
    val ii = ObjectInfoton("/cmt/cm/command-test/objinfo_" + i,"dc_test", None, m)
    Publisher.publish(Vector(ii))
  }
  println("-----------------------------------------")
  StdIn.readLine()
  Grid.shutdown


}
