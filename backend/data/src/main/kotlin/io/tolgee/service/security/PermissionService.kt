@file:Suppress("SpringElInspection")

package io.tolgee.service.security

import io.tolgee.constants.Message
import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.dtos.ProjectPermissionData
import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Invitation
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.repository.PermissionRepository
import io.tolgee.service.CachedPermissionService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PermissionService(
  private val permissionRepository: PermissionRepository,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
  @Lazy
  private val userPreferencesService: UserPreferencesService,
) {
  @set:Autowired
  @set:Lazy
  lateinit var organizationService: OrganizationService

  @set:Autowired
  @set:Lazy
  lateinit var cachedPermissionService: CachedPermissionService

  @set:Lazy
  @set:Autowired
  lateinit var projectService: ProjectService

  fun getAllOfProject(project: Project?): Set<Permission> {
    return permissionRepository.getAllByProjectAndUserNotNull(project)
  }

  fun findById(id: Long): Permission? {
    return cachedPermissionService.findById(id)
  }

  fun getProjectPermissionScopes(projectId: Long, userAccount: UserAccount) =
    getProjectPermissionScopes(projectId, userAccount.id)

  fun getProjectPermissionScopes(projectId: Long, userAccountId: Long): Array<Scope>? {
    val scopes = getProjectPermissionData(projectId, userAccountId).computedPermissions.scopes ?: return null
    return Scope.getUnpackedScopes(scopes)
  }

  fun getProjectPermissionData(project: ProjectDto, userAccountId: Long): ProjectPermissionData {
    val projectPermission = findOneDtoByProjectIdAndUserId(project.id, userAccountId)

    val organizationRole = project.organizationOwnerId
      ?.let { organizationRoleService.findType(userAccountId, it) }

    val organizationBasePermission = project.organizationOwnerId?.let { findOneDtoByOrganizationId(it) }

    val computed = computeProjectPermissionType(
      organizationRole = organizationRole,
      organizationBasePermissionScopes = organizationBasePermission?.scopes,
      directPermissionScopes = projectPermission?.scopes,
      projectPermission?.languageIds
    )

    return ProjectPermissionData(
      organizationRole = organizationRole,
      organizationBasePermissions = organizationBasePermission,
      computedPermissions = computed,
      directPermissions = projectPermission
    )
  }

  fun getPermittedTranslateLanguagesForUserIds(userIds: List<Long>, projectId: Long): Map<Long, List<Long>> {
    val data = permissionRepository.getUserPermittedLanguageIds(userIds, projectId)
    val result = mutableMapOf<Long, MutableList<Long>>()
    data.forEach {
      val languageIds = result.computeIfAbsent(it[0]) {
        mutableListOf()
      }
      languageIds.add(it[1])
    }
    return result
  }

  fun getPermittedTranslateLanguagesForProjectIds(projectIds: List<Long>, userId: Long): Map<Long, List<Long>> {
    val data = permissionRepository.getProjectPermittedLanguageIds(projectIds, userId)
    val result = mutableMapOf<Long, MutableList<Long>>()
    data.forEach {
      val languageIds = result.computeIfAbsent(it[0]) {
        mutableListOf()
      }
      languageIds.add(it[1])
    }
    return result
  }

  fun getProjectPermissionData(projectId: Long, userAccountId: Long): ProjectPermissionData {
    val project = projectService.findDto(projectId) ?: throw NotFoundException()
    return getProjectPermissionData(project, userAccountId)
  }

  fun create(permission: Permission): Permission {
    return cachedPermissionService.create(permission)
  }

  fun delete(permission: Permission) {
    cachedPermissionService.delete(permission)
    permission.user?.let {
      userPreferencesService.refreshPreferredOrganization(it.id)
    }
  }

  /**
   * Deletes all permissions in project
   * No need to evict cache, since this is only used when project is deleted
   */
  fun deleteAllByProject(projectId: Long) {
    val ids = permissionRepository.getIdsByProject(projectId)
    permissionRepository.deleteByIdIn(ids)
  }

  @Transactional
  fun grantFullAccessToProject(userAccount: UserAccount, project: Project) {
    val permission = Permission(
      type = ProjectPermissionType.MANAGE,
      project = project,
      user = userAccount
    )
    create(permission)
  }

  fun computeProjectPermissionType(
    organizationRole: OrganizationRoleType?,
    organizationBasePermissionScopes: Array<Scope>?,
    directPermissionScopes: Array<Scope>?,
    projectPermissionLanguages: Set<Long>?
  ): ComputedPermissionDto {
    if (organizationRole == null) {
      return ComputedPermissionDto(directPermissionScopes, projectPermissionLanguages)
    }

    if (organizationRole == OrganizationRoleType.OWNER) {
      return ComputedPermissionDto(arrayOf(Scope.ADMIN), null)
    }

    if (organizationRole == OrganizationRoleType.MEMBER) {
      if (directPermissionScopes == null) {
        return ComputedPermissionDto(organizationBasePermissionScopes, null)
      }

      if (organizationBasePermissionScopes.isNullOrEmpty()) {
        return ComputedPermissionDto(directPermissionScopes, projectPermissionLanguages)
      }
    }
    throw IllegalStateException("Unexpected organization role")
  }

  fun createForInvitation(
    invitation: Invitation,
    project: Project,
    type: ProjectPermissionType,
    languages: Collection<Language>?
  ): Permission {
    return cachedPermissionService.createForInvitation(invitation, project, type, languages)
  }

  fun findOneByProjectIdAndUserId(projectId: Long, userId: Long): Permission? {
    return cachedPermissionService.findOneByProjectIdAndUserId(projectId, userId)
  }

  fun findOneDtoByProjectIdAndUserId(projectId: Long, userId: Long): PermissionDto? {
    return cachedPermissionService.findOneDtoByProjectIdAndUserId(projectId, userId)
  }

  fun findOneDtoByOrganizationId(organizationId: Long): PermissionDto? {
    return cachedPermissionService.findOneDtoByOrganizationId(organizationId)
  }

  fun acceptInvitation(permission: Permission, userAccount: UserAccount): Permission {
    // switch user to the organization when accepted invitation
    userPreferencesService.setPreferredOrganization(permission.project!!.organizationOwner, userAccount)
    return cachedPermissionService.acceptInvitation(permission, userAccount)
  }

  fun setUserDirectPermission(
    projectId: Long,
    userId: Long,
    newPermissionType: ProjectPermissionType,
    languages: Set<Language>? = null
  ): Permission? {
    validateLanguagePermissions(languages, newPermissionType)

    val data = this.getProjectPermissionData(projectId, userId)

    data.computedPermissions.scopes ?: throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)

    data.organizationRole?.let {
      if (data.organizationRole == OrganizationRoleType.OWNER) {
        throw BadRequestException(Message.USER_IS_ORGANIZATION_OWNER)
      }
    }

    val permission = data.directPermissions?.let { findById(it.id) } ?: let {
      val userAccount = userAccountService.get(userId)
      val project = projectService.get(projectId)
      Permission(user = userAccount, project = project, type = newPermissionType)
    }

    permission.type = newPermissionType
    permission.languages = languages?.toMutableSet() ?: mutableSetOf()
    return cachedPermissionService.save(permission)
  }

  private fun validateLanguagePermissions(
    languages: Set<Language>?,
    newPermissionType: ProjectPermissionType
  ) {
    if (!languages.isNullOrEmpty() && newPermissionType != ProjectPermissionType.TRANSLATE) {
      throw BadRequestException(Message.ONLY_TRANSLATE_PERMISSION_ACCEPTS_LANGUAGES)
    }
  }

  fun saveAll(permissions: Iterable<Permission>) {
    permissions.forEach { cachedPermissionService.save(it) }
  }

  fun revoke(projectId: Long, userId: Long) {
    val data = this.getProjectPermissionData(projectId, userId)
    if (data.organizationRole != null) {
      throw BadRequestException(Message.USER_IS_ORGANIZATION_MEMBER)
    }

    data.directPermissions?.let {
      findById(it.id)?.let { found ->
        cachedPermissionService.delete(found)
      }
    } ?: throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)

    userPreferencesService.refreshPreferredOrganization(userId)
  }

  fun onLanguageDeleted(language: Language) {
    val permissions = permissionRepository.findAllByPermittedLanguage(language)
    permissions.forEach { permission ->
      val hasAccessOnlyToDeletedLanguage = permission.languages.size == 1 &&
        permission.languages.first().id == language.id

      if (hasAccessOnlyToDeletedLanguage) {
        permission.languages = mutableSetOf()
        permission.type = ProjectPermissionType.VIEW
        cachedPermissionService.save(permission)
        return@forEach
      }

      permission.languages.removeIf { it.id == language.id }
      cachedPermissionService.save(permission)
    }
  }

  @Transactional
  fun leave(project: Project, userId: Long) {
    val permissionData = this.getProjectPermissionData(project.id, userId)
    if (permissionData.organizationRole != null) {
      throw BadRequestException(Message.CANNOT_LEAVE_PROJECT_WITH_ORGANIZATION_ROLE)
    }

    val directPermissions = permissionData.directPermissions
      ?: throw BadRequestException(Message.DONT_HAVE_DIRECT_PERMISSIONS)

    val permissionEntity = this.findById(directPermissions.id)
      ?: throw NotFoundException()

    this.delete(permissionEntity)
  }
}