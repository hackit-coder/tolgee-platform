package io.tolgee.ee.data.task

import io.tolgee.model.enums.TranslationState

open class TranslationScopeFilters {
  var filterState: List<TranslationState>? = listOf()
  var filterOutdated: Boolean? = false

  val filterStateOrdinal: List<Int>? get() {
    return filterState?.map { it.ordinal }
  }
}
