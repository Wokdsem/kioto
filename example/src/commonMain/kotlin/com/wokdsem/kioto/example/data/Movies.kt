package com.wokdsem.kioto.example.data

import com.wokdsem.kioto.example.domain.Movie
import kotlinx.coroutines.delay
import kotlin.random.Random

internal suspend fun suggestMovie(): Movie {
    return movies.random().also { delay(Random.nextLong(2500)) }
}

private val movies = listOf(
    Movie("m001", "The Shawshank Redemption", "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.", 9.3f, 1994),
    Movie("m002", "The Godfather", "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son.", 9.2f, 1972),
    Movie(
        "m003",
        "The Dark Knight",
        "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.",
        9.0f,
        2008
    ),
    Movie(
        "m004",
        "Pulp Fiction",
        "The lives of two mob hitmen, a boxer, a gangster's wife, and a pair of diner bandits intertwine in four tales of violence and redemption.",
        8.9f,
        1994
    ),
    Movie(
        "m005",
        "The Lord of the Rings: The Return of the King",
        "Gandalf and Aragorn lead the World of Men against Sauron's army to draw his gaze from Frodo and Sam as they approach Mount Doom with the One Ring.",
        8.9f,
        2003
    ),
    Movie(
        "m006",
        "Forrest Gump",
        "The presidencies of Kennedy and Johnson, the Vietnam War, the Watergate scandal and other historical events unfold from the perspective of an Alabama man with an IQ of 75, whose only desire is to be reunited with his childhood sweetheart.",
        8.8f,
        1994
    ),
    Movie(
        "m007",
        "Inception",
        "A thief who steals corporate secrets through use of dream-sharing technology is given the inverse task of planting an idea into the mind of a CEO.",
        8.8f,
        2010
    ),
    Movie(
        "m008",
        "Fight Club",
        "An insomniac office worker looking for a way to change his life crosses paths with a devil-may-care soap maker and they form an underground fight club that evolves into something much, much more.",
        8.8f,
        1999
    ),
    Movie(
        "m009",
        "The Matrix",
        "When a beautiful stranger leads computer hacker Neo to a forbidding underworld, he discovers the shocking truth--the life he knows is the elaborate deception of an evil cyber-intelligence.",
        8.7f,
        1999
    ),
    Movie(
        "m010",
        "Goodfellas",
        "The story of Henry Hill and his life in the mob, covering his relationship with his wife Karen Hill and his two crime partners Jimmy Conway and Tommy DeVito in the Italian-American community in New York City.",
        8.7f,
        1990
    ),
    Movie("m011", "Interstellar", "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.", 8.6f, 2014),
    Movie(
        "m012",
        "Spirited Away",
        "During her family's move to the suburbs, a sullen 10-year-old girl wanders into a world ruled by gods, witches, and spirits, and where humans are changed into beasts.",
        8.6f,
        2001
    ),
    Movie(
        "m013",
        "Saving Private Ryan",
        "Following the Normandy Landings, a group of U.S. soldiers go behind enemy lines to retrieve a paratrooper whose brothers have been killed in action.",
        8.6f,
        1998
    ),
    Movie(
        "m014",
        "The Green Mile",
        "The lives of guards on Death Row are affected by one of their charges: a black man accused of child murder and rape, who has a mysterious gift.",
        8.6f,
        1999
    ),
    Movie(
        "m015",
        "The Prestige",
        "Two stage magicians engage in a ego-driven battle to create the ultimate illusion while sacrificing everything they have to outwit each other.",
        8.5f,
        2006
    ),
    Movie("m016", "Gladiator", "A Roman General sets out to exact vengeance against the corrupt emperor who murdered his family and sent him into slavery.", 8.5f, 2000),
    Movie(
        "m017",
        "Whiplash",
        "A promising young drummer enrolls at a cut-throat music conservatory where his ruthless instructor will stop at nothing to realize a student's potential.",
        8.5f,
        2014
    ),
    Movie(
        "m018",
        "Parasite",
        "Greed and class discrimination threaten the newly formed symbiotic relationship between the wealthy Park family and the destitute Kim family.",
        8.5f,
        2019
    ),
    Movie("m019", "Lion King", "A young lion prince flees his kingdom after the murder of his father, only to return years later to reclaim his rightful place.", 8.5f, 1994),
    Movie("m020", "Modern Times", "The Tramp struggles to live in modern industrial society with the help of a young homeless woman.", 8.5f, 1936),
    Movie(
        "m021",
        "Back to the Future",
        "Marty McFly, a 17-year-old high school student, is accidentally sent thirty years into the past in a time-traveling DeLorean invented by his eccentric scientist friend Doc Brown.",
        8.5f,
        1985
    ),
    Movie(
        "m022",
        "Inglourious Basterds",
        "In Nazi-occupied France during World War II, a plan to assassinate Nazi leaders by a group of Jewish U.S. soldiers coincides with a theatre owner's similar plot.",
        8.4f,
        2009
    ),
    Movie("m023", "The Pianist", "A Polish-Jewish musician struggles to survive the destruction of the Warsaw Ghetto during World War II.", 8.5f, 2002),
    Movie(
        "m024",
        "The Departed",
        "An undercover state cop and a mole in the police force try to identify each other while infiltrating an Irish gang in South Boston.",
        8.5f,
        2006
    ),
    Movie(
        "m025",
        "Alien",
        "The crew of a commercial spacecraft encounters a deadly extraterrestrial lifeform after investigating a mysterious signal on a remote planet.",
        8.4f,
        1979
    ),
    Movie(
        "m026",
        "Amelie",
        "Amélie is an innocent and naive waitress in Montmartre, Paris, with her own sense of justice. She secretly orchestrates the lives of those around her.",
        8.3f,
        2001
    ),
    Movie("m027", "Toy Story", "A cowboy doll is profoundly threatened and jealous when a new spaceman figure supplants him as top toy in a boy's room.", 8.3f, 1995),
    Movie(
        "m028",
        "Coco",
        "Aspiring musician Miguel, confronted with his family's ancestral ban on music, enters the Land of the Dead to find his great-great-grandfather, a legendary singer.",
        8.4f,
        2017
    ),
    Movie(
        "m029",
        "Blade Runner 2049",
        "Young Blade Runner K's discovery of a long-buried secret leads him to track down former Blade Runner Rick Deckard, who's been missing for 30 years.",
        8.0f,
        2017
    ),
    Movie(
        "m030",
        "Trainspotting",
        "Renton, deeply immersed in the Edinburgh drug scene, tries to clean up his act despite the allure of his friends and the temptations of addiction.",
        8.1f,
        1996
    ),
    Movie(
        "m031",
        "Eternal Sunshine of the Spotless Mind",
        "When their relationship turns sour, a couple undergoes a medical procedure to have each other erased from their memories.",
        8.3f,
        2004
    ),
    Movie("m032", "No Country for Old Men", "Violence and mayhem ensue after a hunter stumbles upon a drug deal gone wrong and takes a briefcase full of cash.", 8.1f, 2007),
    Movie("m033", "Django Unchained", "With the help of a German bounty hunter, a freed slave sets out to rescue his wife from a brutal Mississippi plantation owner.", 8.4f, 2012),
    Movie(
        "m034",
        "Guardians of the Galaxy",
        "A group of intergalactic criminals are forced to work together to stop a fanatical warrior from taking control of the universe.",
        7.9f,
        2014
    ),
    Movie(
        "m035",
        "Spider-Man: Into the Spider-Verse",
        "Teen Miles Morales becomes the Spider-Man of his universe, and must join with five spider-powered individuals from other dimensions to stop a threat to all realities.",
        8.4f,
        2018
    ),
    Movie(
        "m036",
        "The Grand Budapest Hotel",
        "The adventures of Gustave H, a legendary concierge at a famous hotel from the interwar period, and Zero Moustafa, the lobby boy who becomes his most trusted friend.",
        8.1f,
        2014
    ),
    Movie("m037", "Her", "A lonely writer develops an unlikely relationship with an operating system designed to meet his every need.", 8.0f, 2013),
    Movie("m038", "Arrival", "A linguist is recruited by the military to assist in translating alien communications.", 7.9f, 2016),
    Movie(
        "m039",
        "V for Vendetta",
        "In a future British tyranny, a shadowy freedom fighter, known only by the alias of 'V', plots to overthrow the government with the help of a young woman.",
        8.2f,
        2005
    ),
    Movie(
        "m040",
        "Gone Girl",
        "With his wife's disappearance having become the focus of an intense media circus, a man sees the spotlight turned on him when it's suspected that he may not be innocent.",
        8.1f,
        2014
    ),
    Movie(
        "m041",
        "Mad Max: Fury Road",
        "In a post-apocalyptic wasteland, a woman rebels against a tyrannical ruler in search for her homeland with the aid of a group of female prisoners, a psychotic worshipper, and a drifter named Max.",
        8.1f,
        2015
    ),
    Movie(
        "m042",
        "La La Land",
        "While navigating their careers in Los Angeles, a pianist and an actress fall in love while attempting to reconcile their aspirations for the future.",
        8.0f,
        2016
    ),
    Movie("m043", "Drive", "A mysterious Hollywood stuntman and mechanic moonlights as a getaway driver and finds himself in trouble when he helps his neighbor.", 7.8f, 2011),
    Movie(
        "m044",
        "Baby Driver",
        "After being coerced into working for a crime boss, a talented getaway driver finds himself in over his head when he falls for a waitress.",
        7.6f,
        2017
    ),
    Movie("m045", "The Lighthouse", "Two lighthouse keepers try to maintain their sanity whilst living on a remote and mysterious New England island in the 1890s.", 7.4f, 2019),
    Movie("m046", "Knives Out", "A detective investigates the death of a patriarch of an eccentric, combative family.", 7.9f, 2019),
    Movie("m047", "Joker", "A mentally troubled comedian embarks on a downward spiral that leads him to the rise of a legendary psychopathic criminal.", 8.4f, 2019),
    Movie("m048", "1917", "Two young British soldiers are sent on a seemingly impossible mission to deliver a message deep in enemy territory.", 8.3f, 2019),
    Movie(
        "m049",
        "Once Upon a Time in Hollywood",
        "A faded television actor and his stunt double strive to achieve fame and success in the film industry during the final years of Hollywood's Golden Age in 1969 Los Angeles.",
        7.6f,
        2019
    )
)
