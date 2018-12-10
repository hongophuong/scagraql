package com.howtographql.scala.sangria

import java.sql.Timestamp

import akka.http.scaladsl.model.DateTime
import com.howtographql.scala.sangria.models.{Link, User, Vote}
import slick.ast.BaseTypedType
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcType

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


object DBSchema {

  implicit val dateTimeColumnType: JdbcType[DateTime] with BaseTypedType[DateTime] =
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.clicks),
      ts => DateTime(ts.getTime)
    )

  class LinksTable(tag: Tag) extends Table[Link](tag, "LINKS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def url = column[String]("URL")

    def description = column[String]("DESCRIPTION")

    def createdAt = column[DateTime]("CREATED_AT")

    def * = (id, url, description, createdAt).mapTo[Link]
  }

  class UsersTable(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def name = column[String]("NAME")

    def email = column[String]("EMAIL")

    def password = column[String]("PASSWORD")

    def createdAt = column[DateTime]("CREATED_AT")

    def * = (id, name, email, password, createdAt).mapTo[User]
  }

  class VotesTable(tag: Tag) extends Table[Vote](tag, "VOTES") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def createdAt = column[DateTime]("CREATED_AT")

    def userId = column[Int]("USER_ID")

    def linkId = column[Int]("LINK_ID")

    def * = (id, createdAt, userId, linkId).mapTo[Vote]
  }

  //2
  val Links = TableQuery[LinksTable]
  val Users = TableQuery[UsersTable]
  val Votes = TableQuery[VotesTable]

  /**
    * Load schema and populate sample data withing this Sequence od DBActions
    */
  val databaseSetup = DBIO.seq(
    Links.schema.create,

    Links forceInsertAll Seq(
      Link(1, "http://howtographql.com", "Awesome community driven GraphQL tutorial", DateTime(2018, 12, 10)),
      Link(2, "http://graphql.org", "Official GraphQL web page", DateTime(2018, 12, 11)),
      Link(3, "https://facebook.github.io/graphql/", "GraphQL specification", DateTime(2017, 10, 1))),

    Users.schema.create,
    Users forceInsertAll Seq(
      User(1, "User A", "a@test.com", "pppppp", DateTime(2018, 12, 12)),
      User(2, "User B", "b@test.com", "pppppp", DateTime(2018, 12, 12))
    ),

    Votes.schema.create,
    Votes forceInsertAll Seq(
      Vote(1, DateTime(2018, 12, 14), 1, 2),
      Vote(2, DateTime(2018, 12, 14), 2, 3),
      Vote(3, DateTime(2018, 12, 14), 2, 1)
    )

  )


  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)

  }

}
