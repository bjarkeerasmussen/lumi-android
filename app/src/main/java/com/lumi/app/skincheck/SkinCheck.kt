package com.lumi.app.skincheck

import com.lumi.app.data.SkinCheckResult

/**
 * Lumi Skin Check — a NON-MEDICAL, cosmetic self-assessment.
 *
 * This intentionally only recognises everyday, visible skin *patterns*
 * (dryness, oiliness, congestion, etc.) and gives general care suggestions.
 * It never names or implies a medical condition (eczema, rosacea, psoriasis,
 * skin cancer, infection, …) and never tells the user what they "have".
 *
 * Anything that could be medical is handled by [RedFlags]: if the user reports
 * one, the result drops all cosmetic guidance and simply recommends seeing a
 * dermatologist.
 */
object SkinCheck {

    /** A visible, cosmetic sign the user can tick. */
    data class Sign(val id: String, val label: String)

    /** A cosmetic pattern + plain-language, non-medical care suggestion. */
    data class Pattern(val name: String, val advice: String)

    /** Cosmetic signs shown as checkboxes, grouped for the UI. */
    val signGroups: List<Pair<String, List<Sign>>> = listOf(
        "Feel" to listOf(
            Sign("tight", "Feels tight or flaky"),
            Sign("shine", "Gets shiny / oily during the day"),
            Sign("stings", "Stings or feels sensitive with products")
        ),
        "Texture" to listOf(
            Sign("pores", "Visible pores or blackheads"),
            Sign("rough", "Rough or bumpy patches"),
            Sign("lines", "Fine dryness lines")
        ),
        "Breakouts" to listOf(
            Sign("pimples", "Pimples or whiteheads right now"),
            Sign("marks", "Dark marks left after spots heal")
        ),
        "Tone & color" to listOf(
            Sign("redness", "Redness or blotchiness that comes and goes"),
            Sign("dull", "Dull or uneven tone")
        )
    )

    /**
     * Red-flag questions. A "yes" to any of these means the assessment should
     * stop guessing and point to a professional. These describe *changes /
     * symptoms*, not conditions.
     */
    val redFlags: List<Sign> = listOf(
        Sign("mole", "A mole or spot that's new, changing, asymmetric, multi-coloured, or bleeding"),
        Sign("spreading", "A rash or patch that's spreading, weeping, or very itchy/painful"),
        Sign("sore", "A sore that hasn't healed after a few weeks"),
        Sign("infection", "Swelling with heat, pus, or feeling unwell")
    )

    private val patternForSign: Map<String, Pattern> = mapOf(
        "tight" to Pattern(
            "Dryness / dehydration",
            "Skin reads as thirsty. A gentle, non-stripping cleanser plus a moisturiser with humectants (glycerin, hyaluronic acid) and applying it on slightly damp skin usually helps. Daytime SPF protects the barrier."
        ),
        "shine" to Pattern(
            "Oiliness",
            "Oil is normal and protective — the goal is balance, not stripping. A lightweight gel moisturiser and a gentle cleanser are kinder than harsh, drying products, which can make skin produce more oil."
        ),
        "stings" to Pattern(
            "Sensitivity",
            "Skin is reacting easily. Simplify the routine, patch-test new products, and lean on fragrance-free basics. Introduce one active at a time."
        ),
        "pores" to Pattern(
            "Congestion / clogged pores",
            "Pores look busy. A gentle BHA (salicylic acid) a couple of times a week can help keep them clear. Avoid aggressive scrubbing."
        ),
        "rough" to Pattern(
            "Rough texture",
            "Regular gentle exfoliation (a mild AHA/BHA, not gritty scrubs) plus consistent moisturising tends to smooth texture over a few weeks."
        ),
        "lines" to Pattern(
            "Early dryness lines",
            "These often soften with hydration alone. A moisturiser, daily SPF, and (optionally) a well-tolerated retinoid at night are the usual basics."
        ),
        "pimples" to Pattern(
            "Breakout-prone",
            "Keep the routine simple and non-comedogenic. Spot treatments with benzoyl peroxide or salicylic acid help; resist picking, which worsens marks."
        ),
        "marks" to Pattern(
            "Post-blemish marks",
            "These fade with time. Daily SPF is the single biggest help (sun darkens them); vitamin C or niacinamide can speed evenness."
        ),
        "redness" to Pattern(
            "Redness / reactivity",
            "Soothing, fragrance-free products and avoiding known triggers (very hot water, harsh actives) calm visible redness. If it's persistent or worsening, a dermatologist can advise."
        ),
        "dull" to Pattern(
            "Dullness / uneven tone",
            "Gentle exfoliation, hydration, vitamin C in the morning, and consistent SPF gradually bring back evenness and glow."
        )
    )

    /**
     * Evaluate selected signs into a non-medical result.
     * @param signIds cosmetic signs the user ticked
     * @param redFlagIds red-flag items the user ticked
     */
    fun evaluate(signIds: Set<String>, redFlagIds: Set<String>): SkinCheckResult {
        if (redFlagIds.isNotEmpty()) {
            return SkinCheckResult(
                patterns = emptyList(),
                redFlag = true,
                noteForUser = "One or more of the things you noted are best looked at by a " +
                    "dermatologist or doctor rather than a tracking app. This isn't a " +
                    "diagnosis — but please book a professional skin check. In the " +
                    "meantime, keep your routine gentle and avoid picking at the area."
            )
        }
        val patterns = signIds.mapNotNull { patternForSign[it]?.name }.distinct()
        val note = if (patterns.isEmpty()) {
            "Nothing notable ticked — your skin looks like it's doing well today. " +
                "Keep up gentle cleansing, moisturiser, and daily SPF."
        } else {
            "These are common, everyday skin patterns — not a medical diagnosis. " +
                "If anything is painful, spreading, or doesn't improve over a few " +
                "weeks, see a dermatologist."
        }
        return SkinCheckResult(patterns = patterns, redFlag = false, noteForUser = note)
    }

    /** Care advice lines for a set of pattern names (for display). */
    fun adviceFor(patternNames: List<String>): List<Pattern> =
        patternForSign.values.filter { it.name in patternNames }.distinctBy { it.name }
}
