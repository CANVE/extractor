package org.canve.githubCruncher

case class Project(
  searchScore: Float,
  isFork: Boolean,
  forksCount: Int,
  description: String,
  fullName: String,
  sshCloneUrl: String,
  httpCloneUrl: String,
  url: String,
  languagesApiUrl: String) {
}

object Project {
  def apply(item: play.api.libs.json.JsValue) = 
    new Project(
      searchScore = (item \ "score").as[Float],
      isFork = (item \ "fork").as[Boolean],
      forksCount = (item \ "forks").as[Int],
      description = (item \"description").as[String],
      fullName = (item \ "full_name").as[String],
      sshCloneUrl = (item \"ssh_url").as[String],
      httpCloneUrl = (item \"clone_url").as[String],
      url = (item \"html_url").as[String],
      languagesApiUrl = (item \"languages_url").as[String]
    )
}