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


package cmwell.it

import cmwell.util.concurrent.SimpleScheduler._
import com.typesafe.scalalogging.LazyLogging
import org.scalatest._
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.util.Try

class LinkInfotonTests extends AsyncFunSpec with Matchers with Helpers with LazyLogging {

  describe("Link infoton") {
    val jsonObjIn = Json.obj("name" -> "TestObjectForLink", "title" -> "title1")
    val expected = Json.parse(s"""{"type":"ObjectInfoton","system":{"path":"/cmt/cm/test/InfoObj3","parent":"/cmt/cm/test","dataCenter":"$dcName"},"fields":{"title":["title1"],"name":["TestObjectForLink"]}}""")

    val postLinkAndObj = {
      val fo = Http.post(cmt / "InfoObj3", Json.stringify(jsonObjIn), Some("application/json;charset=UTF-8"), Nil, ("X-CM-WELL-TYPE" -> "OBJ") :: tokenHeader)
      val fl = Http.post(cmt / "LinkToObj3", "/cmt/cm/test/InfoObj3", Some("text/plain;charset=UTF-8"), Nil, ("X-CM-WELL-TYPE" -> "LN") :: tokenHeader)
      fo.zip(fl).map {
        case t@(res1,res2) => withClue(t) {
          Json.parse(res1.payload) should be(jsonSuccess)
          Json.parse(res2.payload) should be(jsonSuccess)
        }
      }
    }
    val getLink = postLinkAndObj.flatMap(_ => scheduleFuture(indexingDuration){
      Http.get(cmt / "LinkToObj3")
    })
    val testLink = getLink.map { res =>
      withClue(s"expecting a redirect response: $res") {
        res.status should be(307)
      }
      val loc = res.headers.find(_._1 == "Location").map(_._2)
      withClue(s"expecting a Location header: ${res.headers}") {
        loc.isDefined should be(true)
      }
    }
    val getDataThroughLink = getLink.flatMap { result =>
      val locH = result.headers.find(_._1 == "Location")
      locH.map(_._2).fold(Future[Assertion](fail("expected location header"))) { loc =>
        val dst = loc.split('/').filter(_.nonEmpty).foldLeft(cmw)(_ / _)
        Http.get(dst,List("format" -> "json")).map { res =>
          val body = new String(res.payload, "UTF-8")
          withClue(body + "\n" + res.toString) {
            Try(Json.parse(res.payload).transform(uuidDateEraser).get) match {
              case scala.util.Failure(e) => fail(s"parsing & transforming failed for [$body]",e)
              case scala.util.Success(v) => v shouldEqual expected
            }
          }
        }
      }
    }
    val postUnderscoreInFwdLink = {
      val triples =
        """
          |<http://en.wikipedia.org/wiki/cyrtantheae> <cmwell://meta/sys#linkType> "2"^^<http://www.w3.org/2001/XMLSchema#int> .
          |<http://en.wikipedia.org/wiki/cyrtantheae> <cmwell://meta/sys#linkTo> <http://en.wikipedia.org/wiki/cyrtanthus> .
          |<http://en.wikipedia.org/wiki/cyrtantheae> <cmwell://meta/sys#type> "LinkInfoton" .
        """.stripMargin
      Http.post(_in, triples, Some("text/plain;charset=UTF-8"), List("format" -> "ntriples"), tokenHeader).map { res =>
        withClue(res) {
          Json.parse(res.payload) should be(jsonSuccess)
        }
      }
    }
    val verifyFwdLink = postUnderscoreInFwdLink.flatMap(_ => scheduleFuture(indexingDuration) {
      Http.post(_out, "/en.wikipedia.org/wiki/cyrtantheae", Some("text/plain;charset=UTF-8"), List("format" -> "json"), tokenHeader).map { res =>
        val expected = Json.parse(s"""
          |{
          |  "type": "RetrievablePaths",
          |  "infotons": [
          |    {
          |      "type": "LinkInfoton",
          |      "system": {
          |        "path": "/en.wikipedia.org/wiki/cyrtantheae",
          |        "parent": "/en.wikipedia.org/wiki",
          |        "dataCenter": "lh"
          |      },
          |      "linkTo": "/en.wikipedia.org/wiki/cyrtanthus",
          |      "linkType": 2
          |    }
          |  ],
          |  "irretrievablePaths": []
          |}
        """.stripMargin)
        withClue(res){
          Json.parse(res.payload).transform(bagUuidDateEraserAndSorter).get should be(expected)
        }
      }
    })

    it("should succeed posting ObjectInfoton & LinkInfoton")(postLinkAndObj)
    it("should get obj through link")(testLink)
    it("should retrieve data from link ref")(getDataThroughLink)
    it("should succeed posting LinkInfoton type Forward")(postUnderscoreInFwdLink)
    it("should verify the forward link infoton using POST")(verifyFwdLink)
  }
}
