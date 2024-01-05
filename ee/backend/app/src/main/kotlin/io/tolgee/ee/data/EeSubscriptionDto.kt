package io.tolgee.ee.data

import io.tolgee.constants.Feature
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault
import java.util.*

data class EeSubscriptionDto(
  @field:NotBlank
  var licenseKey: String,
  @field:ColumnDefault("Plan")
  var name: String,
  @field:NotNull
  var currentPeriodEnd: Date? = null,
  var cancelAtPeriodEnd: Boolean = false,
  var enabledFeatures: Array<Feature>,
  var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
  var lastValidCheck: Date? = null,
)
