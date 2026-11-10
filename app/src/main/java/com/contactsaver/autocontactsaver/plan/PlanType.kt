package com.contactsaver.autocontactsaver.plan

enum class PlanType(
    val planName: String,
    val price: String,
    val productId: String,
    val durationDays: Int,
    val isTrial: Boolean = false
) {
    TRIAL(
        planName = "Trial Plan",
        price = "Free",
        productId = "",
        durationDays = -1,
        isTrial = true
    ),
    MONTHLY(
        planName = "Monthly Plan",
        price = "₹199",
        productId = "monthly_plan_199",
        durationDays = 30
    ),
    YEARLY(
        planName = "Yearly Plan",
        price = "₹999",
        productId = "yearly_plan_999",
        durationDays = 365
    ),
    LIFETIME(
        planName = "Lifetime Plan",
        price = "₹1999",
        productId = "lifetime_plan_1999",
        durationDays = -1
    );

    fun isLifetime() = durationDays == -1 && !isTrial
}
