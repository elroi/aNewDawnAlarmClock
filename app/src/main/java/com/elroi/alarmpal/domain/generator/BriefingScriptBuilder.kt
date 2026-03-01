package com.elroi.alarmpal.domain.generator

import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BriefingScriptBuilder @Inject constructor() {

    fun buildLocalBriefing(
        persona: String,
        location: String,
        weather: String,
        calendar: String,
        funFact: String
    ): String {
        val greeting = getGreeting(persona)
        val timeContext = getTimeContext()
        val weatherIntro = if (weather.isNotBlank()) getWeatherIntro(persona, location, weather) else ""
        val calendarAdvice = if (calendar.isNotBlank()) getCalendarAdvice(persona, calendar) else ""
        val factTransition = if (funFact.isNotBlank()) getFactTransition(persona, funFact) else ""
        val mission = getMission(persona)

        val parts = listOf(greeting, timeContext, weatherIntro, calendarAdvice, factTransition, mission)
            .filter { it.isNotBlank() }
        
        return parts.joinToString("\n\n")
    }

    private fun getTimeContext(): String {
        val now = java.time.LocalDateTime.now()
        val dateStr = now.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        val hour = now.hour
        val timeStr = when {
            hour < 6 -> "It's still very early, the world is quiet."
            hour < 9 -> "The morning is fresh and full of promise."
            hour < 12 -> "The day is well underway now."
            else -> "A late start, but plenty of time to make an impact."
        }
        return "Today is $dateStr. $timeStr"
    }

    private fun getGreeting(persona: String): String {
        val greetings = when (persona) {
            "COMEDIAN" -> listOf(
                "Oh look, the human is awake. Truly a miracle of modern science.",
                "Attention: A very important, slightly groggy person has rejoined the living.",
                "Rise and shine! Or just rise. Honestly, the bar is low this early.",
                "I've alerted the media. You're awake. Don't disappoint them."
            )
            "ZEN" -> listOf(
                "The sun rises, and a new journey begins. Breathe deeply.",
                "Peace be with you. The world awaits your mindful presence.",
                "Wake up with intention. Let the morning stillness guide you.",
                "Breathe in the new day. You are exactly where you need to be."
            )
            "HYPEMAN" -> listOf(
                "LET'S GET IT! New day, new goals! Boom!",
                "WAKE UP CHAMP! The world isn't ready for your energy today!",
                "BOOOM! Rise and grind! Time to make history!",
                "BOOYAH! The legend has awakened! Let's conquer everything!"
            )
            else -> listOf(
                "Rise and shine, legend! Time to conquer the world.",
                "Good morning! Your potential is unlimited today.",
                "Wake up, champion! Victory favors those who get moving.",
                "Start your engines! Today is going to be magnificent.",
                "Hey there! The world is better with you in it.",
                "Awake and ready! Let's make today count.",
                "Good morning! Fresh energy, new opportunities. Let's go!"
            )
        }
        return greetings.random()
    }

    private fun getWeatherIntro(persona: String, location: String, weather: String): String {
        val intros = when (persona) {
            "COMEDIAN" -> listOf(
                "In $location, it's $weather. I'd tell you it's beautiful, but I'm an AI and I don't know what beauty is.",
                "Currently in $location: $weather. Or as I like to call it, 'reason #42 to stay in bed'.",
                "The atmosphere in $location is doing this: $weather. Plan your outfit accordingly, or don't. I'm just an app."
            )
            "ZEN" -> listOf(
                "Observe the atmosphere in $location, currently reflecting $weather. Harmonize with it.",
                "In $location, the sky tells a story of $weather. Every weather is a teacher.",
                "The elements in $location are at peace, showing $weather. Walk softly today."
            )
            "HYPEMAN" -> listOf(
                "CHECK THE VIBE! In $location it's $weather! Perfect weather for a winner!",
                "WHOA! $location is bringing the $weather today! Let's USE that energy!",
                "BOOM! Weather report: $location is $weather! No excuses, just results!"
            )
            else -> listOf(
                "It's currently $weather in $location. A solid start for a solid day.",
                "The forecast for $location is $weather. Perfect conditions for success.",
                "In $location, the day begins with $weather. Let's make the most of it."
            )
        }
        return intros.random()
    }

    private fun getCalendarAdvice(persona: String, calendar: String): String {
        val isBusy = !calendar.contains("clear") && !calendar.contains("No events")
        val advice = when (persona) {
            "COMEDIAN" -> if (isBusy) {
                listOf(
                    "Your schedule looks like a Tetris game on level 99. $calendar. Try not to cry.",
                    "You have commitments. $calendar. I'd suggest faking your own death, but we have work to do.",
                    "Your calendar is judging you. $calendar. Better start caffeinating."
                )
            } else {
                listOf(
                    "Your calendar is empty. Finally, you can focus on being your usual brilliant self.",
                    "No meetings? I'm shocked. Truly. Enjoy your freedom.",
                    "A clear calendar. Don't go filling it with productivity or anything crazy."
                )
            }
            "ZEN" -> if (isBusy) {
                listOf(
                    "Your path is full today. $calendar. Flow through it like water.",
                    "Many opportunities to be present. $calendar. Each event is a breath.",
                    "Your manifest shows activity. $calendar. Find the calm in the center for each one."
                )
            } else {
                listOf(
                    "The universe provides a clear space. Use it to find your center.",
                    "A day of stillness on your calendar. Observe the silence.",
                    "No scheduled requirements. Let your intuition be your guide."
                )
            }
            "HYPEMAN" -> if (isBusy) {
                listOf(
                    "YOUR CALENDAR IS EXPLODING! $calendar. TOTAL DOMINATION MODE!",
                    "STACKED! $calendar. You're going to OWN every single one of those meetings!",
                    "LOOK AT THAT SCHEDULE! $calendar. It's a highlight reel waiting to happen!"
                )
            } else {
                listOf(
                    "BLANK CANVAS! Today is the day you build your empire!",
                    "TOTAL FREEDOM! Use this day to do something BIG!",
                    "NO MEETINGS? That's more time for WINNING! Let's go!"
                )
            }
            else -> if (isBusy) {
                listOf(
                    "Your schedule for today: $calendar. Let's tackle it with focus.",
                    "You have a productive day ahead. $calendar. Stay efficient.",
                    "A full agenda. $calendar. One step at a time, you've got this."
                )
            } else {
                listOf(
                    "Your calendar is open. A perfect opportunity to get ahead.",
                    "No events today. Make it a day of intentional progress.",
                    "A clear schedule today. Focus on your primary goals."
                )
            }
        }
        return advice.random()
    }

    private fun getFactTransition(persona: String, fact: String): String {
        val transitions = when (persona) {
            "COMEDIAN" -> listOf(
                "By the way, did you know? $fact. Knowledge is power, apparently.",
                "Here is a useless fact to clog your brain: $fact. You're welcome.",
                "I found this in the trash bin of the internet: $fact. Delightful, isn't it?"
            )
            "ZEN" -> listOf(
                "Consider this piece of worldly wisdom: $fact. All things are connected.",
                "In the tapestry of life, consider this: $fact. Small wonders are everywhere.",
                "A mindful observation for you: $fact. Reflect on its place in the world."
            )
            "HYPEMAN" -> listOf(
                "MIND BLOWN! Did you know $fact?! Crazy, right?!",
                "INSANE TRIVIA! $fact. Use that to impress someone today!",
                "GET THIS! $fact. You learn something new every day on the way to the top!"
            )
            else -> listOf(
                "Interesting fact for today: $fact. A little wisdom for your morning.",
                "Did you know? $fact. Something to think about as you start your day.",
                "Here's your morning trivia: $fact. Knowledge is the key to success."
            )
        }
        return transitions.random()
    }

    private fun getMission(persona: String): String {
        val missions = when (persona) {
            "COMEDIAN" -> listOf(
                "Your mission: Compliment a stranger's shoes. They'll be confused, you'll be amused.",
                "Mission for today: Drink water before coffee. Your body will thank me, your brain will hate me.",
                "Today's quest: Don't check your emails until you've eaten something. Be a rebel.",
                "Final instruction: Try not to walk into any walls. I can't help you with physical coordination."
            )
            "ZEN" -> listOf(
                "Your mission: Take five deep breaths before you start your first task.",
                "Mission for today: Smile at a stranger. Observe the ripples of kindness.",
                "Today's practice: Find one moment of complete silence. Just one.",
                "Walk mindfully today. Your journey is the destination."
            )
            "HYPEMAN" -> listOf(
                "MISSION: HIGH-FIVE THE MIRROR! You're a legend! Let's go!",
                "TODAY'S GOAL: Do 10 jumping jacks right now! PUMP IT UP!",
                "YOUR MISSION: CRUSH YOUR HARDEST TASK FIRST! TOTAL VICTORY!",
                "LET'S GET IT! Make today the best day of the year! BOOM!"
            )
            else -> listOf(
                "Your mission: Set one clear goal and achieve it by noon.",
                "Mission for today: Compliment a colleague or friend. Energy is contagious.",
                "Today's goal: Learn something new, no matter how small.",
                "Make today count. You've got this."
            )
        }
        return missions.random()
    }
}
