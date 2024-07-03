package io.tolgee.ee.data.task

data class UpdateTaskKeysRequest(
  var addKeys: MutableSet<Long>? = null,
  var removeKeys: MutableSet<Long>? = null,
)
