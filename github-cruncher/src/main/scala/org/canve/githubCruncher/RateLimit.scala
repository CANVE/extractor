package org.canve.githubCruncher
import scala.concurrent.{Future, Promise}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import scalaj.http._
import com.github.nscala_time.time.Imports._
import scala.concurrent.Await
import play.api.libs.json._
import play.api.libs.functional.syntax._

/*
 * A rate limiting api caller
 */
object LimitingApiCaller {
  
  private var applicableRateState: Future[SearchRateState] = safeRateLimitCheck
  
  /*
   * get rate limit status without counting as part of quota
   */
  private def safeRateLimitCheck: Future[SearchRateState] = 
    performApiCall(Http("https://api.github.com/rate_limit")) map { response =>
      if (!response.isSuccess) throw new Exception(s"github api bad or unexpected response: \n$response")
      new SearchRateState(response) 
  }

  /* Class representing the established github rate limit */
  private case class Rate(windowLimit: Int, windowRemaining: Int, windowEnd: DateTime) {
    
    /*
     * We use this function to always reserve some api quota, so that the api can always be 
     * manually examined outside the run of the application
     */
    def windowQuotaReserveLeft = windowRemaining > (0.1 * windowLimit)   
  }
  
  /* 
   * Class that gets the github rate limit applicable to search queries, both from a github api response,
   * and from a rate limit api query (the latter does does not count towards quota)
   */
  private case class SearchRateState(response: HttpResponse[String]) {
    
    private val asJson: JsObject = Json.parse(response.body).as[JsObject]
    
    val rate = asJson.keys.contains("resources") match {

      /* handles response coming from a query api call */
      case false => 
        Rate(
          windowLimit     = response.headers("X-RateLimit-Limit").head.toInt,
          windowRemaining = response.headers("X-RateLimit-Remaining").head.toInt,
          windowEnd       = (response.headers("X-RateLimit-Reset").head.toLong * 1000).toDateTime)
      
      /* handles response coming from a rate limit api call */
      case true  =>
        val jsonSearchObj = (asJson \ "resources" \ "search")
        Rate(
          windowLimit     = (jsonSearchObj \ "limit").as[Int],
          windowRemaining = (jsonSearchObj \ "remaining").as[Int],
          windowEnd       = ((jsonSearchObj \ "reset").as[Long] * 1000).toDateTime)             
    }
  }
  
  def nonBlockingHttp(apiCall: HttpRequest) = maybeApiCall(apiCall)
  
  private val slack = 5.seconds // time to linger after the new rate limit window start time
  
  private def maybeApiCall(apiCall: HttpRequest) = {
    
    if (!apiCall.url.contains("//api.github.com/search/"))
      throw new Exception("rate limiting for non-search github api is not yet supported here") 
  
    /* pass the api call through, or elegantly reject if rate limit protection is needed */
    applicableRateState.flatMap { s =>
      
      if (DateTime.now > s.rate.windowEnd) applicableRateState = safeRateLimitCheck
        
      applicableRateState.flatMap { _.rate.windowQuotaReserveLeft match {
        case true  => performApiCall(apiCall)
        case false => 
          applicableRateState flatMap { currentRateState =>
            Future.failed[HttpResponse[String]](
              RateLimitHint(currentRateState.rate.windowEnd + slack))
        }
      }}
    }
  } 
  
  private def performApiCall(apiCall: HttpRequest): Future[HttpResponse[String]] = {
    val response = Future { apiCall.asString }
    response.onComplete { 
      case Success(response) => applicableRateState = Future.successful(SearchRateState(response)) 
      case Failure(f) => throw new Exception(s"failed completing github api call: \n$f") 
    }
    response
  }
}

/* Exception type conveying a hint for when to retry the api call ― back to the caller */
case class RateLimitHint(safeRetryTime: DateTime) extends Throwable
