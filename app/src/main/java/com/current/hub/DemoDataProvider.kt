package com.current.hub

data class DemoContact(
    val name: String,
    val handle: String,
    val status: String,
    val avatarColor: String // Material 3 Hex String
)

object DemoDataProvider {
    val contacts = listOf(
        DemoContact(
            name = "Charli xcx",
            handle = "@charli_xcx",
            status = "Everything is romantic",
            avatarColor = "#8ACE00" // Brat Green
        ),
        DemoContact(
            name = "Lady Gaga",
            handle = "@ladygaga",
            status = "Dancing in the flames",
            avatarColor = "#FF007F" // Chromatica Pink
        ),
        DemoContact(
            name = "Melanie Martinez",
            handle = "@littlebodybigheart",
            status = "In the Cry Baby world",
            avatarColor = "#B29DD9" // Pastel Purple
        ),
        DemoContact(
            name = "Chappell Roan",
            handle = "@chappellroan",
            status = "Your favorite artist's favorite artist",
            avatarColor = "#D32F2F" // Midwest Princess Red
        ),
        DemoContact(
            name = "Sabrina Carpenter",
            handle = "@sabrinacarpenter",
            status = "Please please please",
            avatarColor = "#F4D03F" // Espresso Gold
        ),
        DemoContact(
            name = "Troye Sivan",
            handle = "@troyesivan",
            status = "Something to give each other",
            avatarColor = "#3498DB" // Rush Blue
        ),
        DemoContact(
            name = "Billie Eilish",
            handle = "@billieeilish",
            status = "HIT ME HARD AND SOFT",
            avatarColor = "#0055FF" // Deep Blue
        ),
        DemoContact(
            name = "Lorde",
            handle = "@lorde",
            status = "Solar Power state of mind",
            avatarColor = "#F39C12" // Sun Orange
        ),
        DemoContact(
            name = "Doja Cat",
            handle = "@dojacat",
            status = "Paint the town red",
            avatarColor = "#1B1B1F" // Scarlet Black
        ),
        DemoContact(
            name = "Ethel Cain",
            handle = "@motherethel",
            status = "God loves you, but not enough",
            avatarColor = "#795548" // Preacher's Daughter Brown
        )
    )
}
