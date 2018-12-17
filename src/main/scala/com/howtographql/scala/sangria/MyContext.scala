package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.{AuthenticationException, User}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class MyContext(dao: DAO, currentUser: Option[User] = None) {

  def login(email: String, password: String): User = {
    val userOpt = Await.result(dao.authenticate(email, password), Duration.Inf)
    userOpt.getOrElse({
      throw AuthenticationException("Email or password are not matched")
    })
  }

  def ensureAuthenticated() =
    if (currentUser.isEmpty) {
      throw AuthenticationException("You do not have permission. Please sign in")
    }
}