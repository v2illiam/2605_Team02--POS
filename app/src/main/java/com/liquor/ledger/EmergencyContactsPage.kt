package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class EmergencyContactsPage(private val activity: Activity) {

    fun build(): ScrollView {

        val scrollView = ScrollView(activity)

        // Added so the scroll area matches Light or Dark Mode. Theme Manager - AF
        scrollView.setBackgroundColor(ThemeManager.pageBackground(activity))

        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 20, 20, 20)

        // Main BG Color based on theme selection - AF
        root.setBackgroundColor(ThemeManager.pageBackground(activity))

        val title = TextView(activity)
        title.text = "Emergency Contacts"
        title.textSize = 28f
        title.setTypeface(null, Typeface.BOLD)

        // Changes Primary text color based on Settings - AF
        title.setTextColor(ThemeManager.primaryText(activity))

        val subtitle = TextView(activity)
        subtitle.text = "Important phone numbers for emergencies and support"
        subtitle.textSize = 14f

        // Changes subtitle text color based on Settings - AF
        subtitle.setTextColor(ThemeManager.secondaryText(activity))

        subtitle.setPadding(0, 10, 0, 30)

        root.addView(title)
        root.addView(subtitle)

        val topRow = LinearLayout(activity)
        topRow.orientation = LinearLayout.HORIZONTAL

        val rightSectionParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        rightSectionParams.marginStart = 10

        topRow.addView(
            createSection(
                "Emergency Services",
                listOf(
                    Triple("Police Emergency", "911", "EMERGENCY"),
                    Triple("Fire Department", "911", "EMERGENCY"),
                    Triple("EMS / Ambulance", "911", "EMERGENCY"),
                    Triple("Police Non-Emergency", "(312) 744-5000", "NON-EMERGENCY"),
                    Triple("Poison Control", "1-800-222-1222", "HOTLINE")
                )
            ),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 10
            }
        )

        topRow.addView(
            createSection(
                "Store Management",
                listOf(
                    Triple("Store Manager", "(312) 555-0100", "DIRECT"),
                    Triple("Assistant Manager", "(312) 555-0101", "DIRECT"),
                    Triple("District Manager", "(312) 555-0200", "DIRECT"),
                    Triple("Corporate Office", "1-800-555-0150", "MAIN"),
                    Triple("HR Department", "1-800-555-0151", "MAIN")
                )
            ),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        root.addView(topRow)

        val bottomRow = LinearLayout(activity)
        bottomRow.orientation = LinearLayout.HORIZONTAL
        bottomRow.setPadding(0, 20, 0, 0)

        bottomRow.addView(
            createSection(
                "Security & Safety",
                listOf(
                    Triple("Security Company", "(312) 555-0300", "DIRECT"),
                    Triple("Alarm Monitoring", "1-800-555-0301", "HOTLINE"),
                    Triple("Lock & Key Service", "(312) 555-0400", "SERVICE")
                )
            ),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        bottomRow.addView(
            createSection(
                "Utilities & Maintenance",
                listOf(
                    Triple("Gas Emergency", "1-800-555-0500", "EMERGENCY"),
                    Triple("Electric Emergency", "1-800-555-0501", "EMERGENCY"),
                    Triple("Water Emergency", "(312) 744-7038", "EMERGENCY"),
                    Triple("HVAC Repair", "(312) 555-0600", "SERVICE"),
                    Triple("Plumbing Service", "(312) 555-0601", "SERVICE")
                )
            ),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        root.addView(bottomRow)

        val protocolBox = LinearLayout(activity)
        protocolBox.orientation = LinearLayout.VERTICAL
        protocolBox.setPadding(30, 25, 30, 25)

        val protocolParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        protocolParams.topMargin = 25

        // Changes protocol box background based on settings - AF
        protocolBox.setBackgroundColor(ThemeManager.emergencyBackground(activity))

        val protocolTitle = TextView(activity)
        protocolTitle.text = "⚠ EMERGENCY PROTOCOL"
        protocolTitle.textSize = 14f
        protocolTitle.setTypeface(null, Typeface.BOLD)

        // Changes emergency title color based on settings - AF
        protocolTitle.setTextColor(ThemeManager.negative(activity))

        val protocolText = TextView(activity)
        protocolText.text =
            "For life-threatening emergencies, always dial 911 first. Then contact store management and follow emergency procedures posted in the break room."
        protocolText.textSize = 13f


        // Changes protocol message color based on Settings - AF
        protocolText.setTextColor(ThemeManager.secondaryText(activity))

        // Changes protocol box background based on settings - AF
        val protocolBackground = GradientDrawable()
        protocolBackground.setColor(ThemeManager.emergencyBackground(activity))
        protocolBackground.cornerRadius = 30f

        protocolBox.background = protocolBackground

        protocolBox.addView(protocolTitle)
        protocolBox.addView(protocolText)

        root.addView(protocolBox, protocolParams)

        scrollView.addView(root)

        return scrollView
    }

    private fun createSection(
        title: String,
        contacts: List<Triple<String, String, String>>
    ): LinearLayout {

        val section = LinearLayout(activity)
        section.orientation = LinearLayout.VERTICAL
        section.setPadding(20, 20, 20, 20)

        val params = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        params.marginEnd = 12

        // Changes section BG color based on Settings - AF
        section.setBackgroundColor(ThemeManager.sectionBackground(activity))
        val sectionBackground = GradientDrawable()
        sectionBackground.setColor(ThemeManager.sectionBackground(activity))
        sectionBackground.cornerRadius = 30f

        section.background = sectionBackground

        val sectionTitle = TextView(activity)
        sectionTitle.text = title
        sectionTitle.textSize = 18f
        sectionTitle.setTypeface(null, Typeface.BOLD)

        // Changes section title color based on settings - AF
        sectionTitle.setTextColor(ThemeManager.primaryText(activity))

        sectionTitle.setPadding(0, 0, 0, 15)

        section.addView(sectionTitle)

        contacts.forEach {

            section.addView(
                createContactCard(
                    it.first,
                    it.second,
                    it.third
                )
            )
        }

        return section
    }

    private fun createContactCard(
        name: String,
        phone: String,
        type: String
    ): LinearLayout {

        val card = LinearLayout(activity)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(25, 20, 25, 20)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.bottomMargin = 15

        // Changes contact card background based on Settings - AF
        card.setBackgroundColor(ThemeManager.cardBackground(activity))
        val cardBackground = GradientDrawable()
        cardBackground.setColor(ThemeManager.cardBackground(activity))
        cardBackground.cornerRadius = 24f

        card.background = cardBackground

        val topRow = LinearLayout(activity)
        topRow.orientation = LinearLayout.HORIZONTAL

        val nameView = TextView(activity)
        nameView.text = name
        nameView.textSize = 14f
        nameView.setTextColor(ThemeManager.secondaryText(activity))

        val badge = TextView(activity)
        badge.text = type
        badge.textSize = 10f
        badge.setPadding(12, 4, 12, 4)

        // Changes badge colors based on Settings - AF
        val badgeBackground = GradientDrawable()
        badgeBackground.cornerRadius = 100f

        when (type) {

            "EMERGENCY" -> {
                badge.setTextColor(Color.WHITE)
                badgeBackground.setColor(ThemeManager.negative(activity))
            }

            "HOTLINE" -> {
                badge.setTextColor(Color.WHITE)
                badgeBackground.setColor(ThemeManager.warning(activity))
            }

            "SERVICE" -> {
                badge.setTextColor(Color.WHITE)
                badgeBackground.setColor(ThemeManager.mutedText(activity))
            }

            else -> {
                badge.setTextColor(Color.WHITE)
                badgeBackground.setColor(ThemeManager.primaryAction(activity))
            }
        }

        badge.background = badgeBackground

        val spacer = TextView(activity)
        spacer.layoutParams =
            LinearLayout.LayoutParams(0, 0, 1f)

        topRow.addView(nameView)
        topRow.addView(spacer)
        topRow.addView(badge)

        val phoneView = TextView(activity)
        phoneView.text = phone
        phoneView.textSize = 18f
        phoneView.setTypeface(null, Typeface.BOLD)

        // Changes phone text color based on settings - AF
        phoneView.setTextColor(ThemeManager.primaryAction(activity))

        phoneView.setPadding(0, 10, 0, 0)

        card.addView(topRow)
        card.addView(phoneView)

        card.layoutParams = params

        return card
    }
}