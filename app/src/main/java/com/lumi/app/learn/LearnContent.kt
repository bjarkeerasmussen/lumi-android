package com.lumi.app.learn

/**
 * Curated, plain-language skincare guidance. General education only — not
 * medical advice, and deliberately conservative. Every article ends pointing
 * back to "see a professional for anything persistent."
 */
object LearnContent {

    data class Article(
        val title: String,
        val summary: String,
        val body: List<String>
    )

    val articles: List<Article> = listOf(
        Article(
            title = "The simple core routine",
            summary = "Three steps cover most skin most of the time.",
            body = listOf(
                "You don't need ten products. A gentle cleanser, a moisturiser, and a daily sunscreen (SPF 30+) handle the basics for almost everyone.",
                "Morning: cleanse, moisturise, SPF. Night: cleanse, moisturise. Add one 'active' (like a vitamin C in the morning or a retinoid at night) only once your skin is comfortable with the basics.",
                "Consistency beats intensity. A simple routine done daily outperforms a complicated one you can't keep up.",
                "If a product stings, burns, or makes things worse, stop it. See a dermatologist for anything that persists."
            )
        ),
        Article(
            title = "Sunscreen, the highest-impact step",
            summary = "The closest thing to an anti-ageing product that actually works.",
            body = listOf(
                "Daily SPF protects against the UV that causes most visible ageing, dark spots, and uneven tone. It also keeps post-blemish marks from darkening.",
                "Use SPF 30 or higher, apply generously, and reapply if you're outside for long stretches.",
                "This is sun PROTECTION. It's the opposite of UV exposure — Lumi never recommends UV light on your skin."
            )
        ),
        Article(
            title = "LED light therapy masks, explained",
            summary = "What red, blue, and near-infrared actually do — and what they don't.",
            body = listOf(
                "Consumer 'light therapy' masks (Therabody, Omnilux, CurrentBody, Shark CryoGlow, and similar) use LEDs — visible light. They do NOT emit UV. UV would damage skin; these don't.",
                "Red light (~630–660nm) is studied for supporting collagen and calming redness. Blue light (~415nm) targets the bacteria involved in breakouts. Near-infrared (~830nm) penetrates a little deeper and is used for soothing.",
                "Evidence is promising but modest, and results are gradual — think weeks of consistent short sessions, not overnight change. Follow the device's own time and frequency guidance.",
                "Safety basics: use the eye protection that comes with the device, don't combine with photosensitising medication without checking with a doctor, and stop if skin becomes irritated.",
                "Log your sessions in Lumi's Routine tab so you can see whether they line up with changes in your photos over time.",
                "LED masks are cosmetic wellness devices, not treatment for a medical skin condition. For persistent acne or any diagnosed condition, see a dermatologist."
            )
        ),
        Article(
            title = "Common ingredients, decoded",
            summary = "A quick glossary so labels make sense.",
            body = listOf(
                "Hyaluronic acid / glycerin: humectants that pull water into skin — hydration.",
                "Niacinamide: a gentle all-rounder for evenness, oil balance, and barrier support.",
                "Salicylic acid (BHA): oil-soluble, good for clogged pores and blackheads.",
                "Glycolic / lactic acid (AHA): surface exfoliation for texture and dullness.",
                "Vitamin C: morning antioxidant for brightness and tone.",
                "Retinoids: night-time vitamin-A derivatives for texture and lines — introduce slowly, always pair with SPF.",
                "Introduce one new active at a time and patch-test first."
            )
        ),
        Article(
            title = "When to see a dermatologist",
            summary = "Some things belong with a professional, not an app.",
            body = listOf(
                "Lumi is a tracking and self-care companion, not a medical tool. Please book a professional skin check for any of these:",
                "• A mole or spot that's new, changing shape or colour, asymmetric, or bleeding.",
                "• A rash or patch that's spreading, weeping, or very itchy or painful.",
                "• A sore that hasn't healed after a few weeks.",
                "• Acne that's painful, scarring, or not improving with basic care.",
                "• Anything that worries you. Peace of mind is a good reason on its own."
            )
        )
    )
}
