package org.canve.githubCruncher
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.util.{Try, Success, Failure}
import scala.annotation.tailrec
import LimitingApiCaller._

/*
 * A crawler that respects github's search query rate limit. 
 * To learn how github rate limiting works you may review:
 * 
 * https://developer.github.com/v3/search/
 * https://developer.github.com/v3/#increasing-the-unauthenticated-rate-limit-for-oauth-applications
 * https://developer.github.com/guides/basics-of-authentication/#registering-your-app
 * https://developer.github.com/v3/#user-agent-required
 * 
 * However some behavior is not documented, e.g. the way the link headers provide information about
 * your rate limit. Specifically at the time of writing, the link headers will report the rate limit
 * applicable to the type of query which they respond to.
 */

trait GithubCrawler {  

  /*
   * get a relevant scala projects list from github api
   */
  def getProjectsList: Future[List[JsValue]] = {
    
    lazy val initialApiCall: HttpRequest = 
      WithUserAgent("https://api.github.com/search/repositories")
      .param("q", "language:scala")
      .param("sort", "forks")

    var result: List[JsValue] = List() 
      
    def impl(apiCall:HttpRequest = initialApiCall): Future[List[JsValue]] = {
      
      println(s"in impl for api call $apiCall")
      
      /* attempt an api call */
      nonBlockingHttp(apiCall).flatMap { response =>

        response.isSuccess match {
          case false => throw new Exception(s"github api bad response: \n$response")
          case true  => println("api call succeeded")
        }

        val asJson: JsValue = Json.parse(response.body) 
        
        /*
        println()
        println(Json.prettyPrint(asJson))
        println()
        */
        
        val headers = response.headers
        val linkHeaders = parseGithubLinkHeader(headers("Link"))
        //println(linkHeaders)

        val projects = (asJson \ "items")
          .as[JsArray]
          .as[List[JsValue]]
        
        result ++= projects 
        
        println("projects: " + result.length)
        
        if (linkHeaders.contains("next")) impl(Http(linkHeaders("next")))  
        else Future.successful(result)
      }
      
      /* if the api call was refused by the rate limit protection of ours, retry it later,
       * at or after the time recommended by the rate limit mechanism 
       */ 
      .recoverWith {
        case RateLimitHint(retryAdvisedTime) =>
          println(s"API call avoided by ${LimitingApiCaller.getClass.getSimpleName} to guarantee rate limit conformance. Will retry this request on $retryAdvisedTime")
          Timer.completeWhen(retryAdvisedTime.toDate) flatMap { _ => impl(apiCall) }
      }
    }
       
    impl() 
  }
  
  /*
   *  parses the link header field (http://tools.ietf.org/html/rfc5988) returned by Github's api 
   *  (c.f. https://developer.github.com/guides/traversing-with-pagination/)
   */
  private def parseGithubLinkHeader(linkHeader: IndexedSeq[String]): Map[String, String] = {
    assert(linkHeader.size == 1)
    println("link header")
    println(linkHeader.head)
    println("link header")
    linkHeader.head.split(',').map { linkEntry =>
      val linkEntryHalf = linkEntry.split(';')
      assert(linkEntryHalf.size == 2)
      val rel = linkEntryHalf(1).replace(" rel=\"", "").replace("\"", "")
      val url = linkEntryHalf(0).replace("<", "").replace(">", "")
      (rel, url)
    }.toMap
  }
}

/* to satisfy https://developer.github.com/v3/#user-agent-required */
object WithUserAgent extends BaseHttp (userAgent = "matanster") 