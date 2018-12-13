package com.howtographql.scala.sangria

import akka.http.scaladsl.model.DateTime
import com.howtographql.scala.sangria.models._
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, Relation, RelationIds}
import sangria.macros.derive.{AddFields, _}
import sangria.schema.{Field, _}


object GraphQLSchema {

  implicit val GraphQLDateTime: ScalarType[DateTime] = ScalarType[DateTime](
    "DateTime",
    coerceOutput = (dt, _) => dt.toString(),
    coerceInput = {
      case StringValue(dt, _, _) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    },
    coerceUserInput = {
      case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    }
  )

  val IdentifiableType = InterfaceType(
    "Identifiable",
    fields[Unit, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  lazy val LinkType: ObjectType[Unit, Link] = deriveObjectType[Unit, Link](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    ReplaceField("postedBy", Field("postedBy", UserType, resolve = l => usersFetcher.defer(l.value.postedBy))),
    AddFields(Field("votes", ListType(VoteType), resolve = l => votesFetcher.deferRelSeq(voteByLinkRel, l.value.id)))
  )

  lazy val UserType: ObjectType[Unit, User] = deriveObjectType[Unit, User](
    Interfaces(IdentifiableType),
    AddFields(Field("links", ListType(LinkType), resolve = u => linksFetcher.deferRelSeq(linkByUserRel, u.value.id))),
    AddFields(Field("votes", ListType(VoteType), resolve = u => votesFetcher.deferRelSeq(voteByUserRel, u.value.id)))
  )

  lazy val VoteType: ObjectType[Unit, Vote] = deriveObjectType[Unit, Vote](
    Interfaces(IdentifiableType),
    ExcludeFields("userId"),
    AddFields(Field("user", UserType, resolve = v => usersFetcher.defer(v.value.userId))),
    ExcludeFields("linkId"),
    AddFields(Field("link", LinkType, resolve = v => linksFetcher.defer(v.value.linkId)))
  )


  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  val linkByUserRel: Relation[Link, Link, Int] = Relation[Link, Int]("byUser", l => Seq(l.postedBy))
  val voteByUserRel: Relation[Vote, Vote, Int] = Relation[Vote, Int]("byUser", v => Seq(v.userId))
  val voteByLinkRel: Relation[Vote, Vote, Int] = Relation[Vote, Int]("byLink", v => Seq(v.linkId))

  val linksFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids),
    (ctx: MyContext, ids: RelationIds[Link]) => ctx.dao.getLinksByUserIds(ids(linkByUserRel))
  )

  val usersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  val votesFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids),
    (ctx: MyContext, ids: RelationIds[Vote]) => ctx.dao.getVotesByRelationIds(ids)
  )

  val Resolver: DeferredResolver[MyContext] = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)

  // 2
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),

      Field("link",
        OptionType(LinkType),
        arguments = Id :: Nil,
        resolve = c => linksFetcher.deferOpt(c.arg(Id))
      ),

      Field("links",
        ListType(LinkType),
        arguments = Ids :: Nil,
        resolve = c => linksFetcher.deferSeq(c.arg(Ids))
      ),

      Field("users",
        ListType(UserType),
        arguments = Ids :: Nil,
        resolve = c => usersFetcher.deferSeq(c.arg(Ids))
      ),

      Field("votes",
        ListType(VoteType),
        arguments = Ids :: Nil,
        resolve = c => votesFetcher.deferSeq(c.arg(Ids))
      )
    )
  )

  // 3
  val SchemaDefinition = Schema(QueryType)
}