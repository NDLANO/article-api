/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.auth

import no.ndla.articleapi.model.api.AccessDeniedException
import no.ndla.network.AuthUser

trait Role {

  val authRole: AuthRole

  class AuthRole {
    val RoleWithWriteAccess = "articles:write"
    val DraftRoleWithWriteAccess = "drafts:write"

    def hasRoles(roles: Set[String]): Boolean = roles.map(AuthUser.hasRole).forall(identity)

    def assertHasRoles(roles: String*): Unit = {
      if (!hasRoles(roles.toSet))
        throw new AccessDeniedException("User is missing required role(s) to perform this operation")
    }

    def assertHasWritePermission(): Unit = assertHasRoles(RoleWithWriteAccess)
    def assertHasDraftWritePermission(): Unit = assertHasRoles(DraftRoleWithWriteAccess)
  }

}


