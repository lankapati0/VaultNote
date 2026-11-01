package com.lankeshwar.vaultnote.data.model

data class JournalTemplate(
    val id:String,
    val name: String,
    val description: String,
    val prompts: String
)

object JournalTemplates {

    val all = listOf(

        JournalTemplate(
            id = "daily_reflection",
            name = "Daily Reflection",
            description = "Reflect on your day and learn from your experiences.",
            prompts = """
                Today's Highlights:

                What challenges did I face?

                What did I learn today?

                What could I improve?

                Goals for Tomorrow:

            """.trimIndent()
        ),

        JournalTemplate(
            id = "gratitude",
            name = "Gratitude Journal",
            description = "Focus on the positive things in your life.",
            prompts = """
                Three Things I'm Grateful For:

                1.

                2.

                3.

                Why am I grateful for these things?

                What made me smile today?

            """.trimIndent()
        ),

        JournalTemplate(
            id = "productivity",
            name = "Productivity Journal",
            description = "Track your priorities and achievements.",
            prompts = """
                Today's Top Priorities:

                1.

                2.

                3.

                What did I accomplish today?

                What distracted me?

                What can I improve tomorrow?

            """.trimIndent()
        ),

        JournalTemplate(
            id = "mood_wellness",
            name = "Mood & Wellness",
            description = "Understand your emotions and well-being.",
            prompts = """
                How am I feeling today?

                What affected my mood?

                My energy level today:

                What did I do for self-care?

                What do I need more of?

            """.trimIndent()
        ),

        JournalTemplate(
            id = "travel",
            name = "Travel Journal",
            description = "Capture your travel experiences and memories.",
            prompts = """
                Places I Visited:

                Memorable Moments:

                New Things I Experienced:

                My Favourite Part:

                People I Met:

                Memories I Want to Keep:

            """.trimIndent()
        )
    )
}