package com.elroi.lemurloop.domain.generator

import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BriefingScriptBuilder @Inject constructor() {

    fun buildLocalBriefing(
        persona: String,
        location: String,
        weather: String,
        calendar: String,
        funFact: String,
        locale: Locale = Locale.getDefault()
    ): String {
        val hebrew = isHebrew(locale)
        val greeting = getGreeting(persona, hebrew)
        val timeContext = getTimeContext(hebrew, locale)
        val weatherIntro = if (weather.isNotBlank()) getWeatherIntro(persona, location, weather, hebrew) else ""
        val calendarAdvice = if (calendar.isNotBlank()) getCalendarAdvice(persona, calendar, hebrew) else ""
        val factTransition = if (funFact.isNotBlank()) getFactTransition(persona, funFact, hebrew) else ""
        val mission = getMission(persona, hebrew)

        val parts = listOf(greeting, timeContext, weatherIntro, calendarAdvice, factTransition, mission)
            .filter { it.isNotBlank() }
        
        return parts.joinToString("\n\n")
    }

    private fun isHebrew(locale: Locale) = locale.language == "he" || locale.language == "iw"

    private fun getTimeContext(hebrew: Boolean, locale: Locale): String {
        val now = java.time.LocalDateTime.now()
        val dateStr = now.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", locale))
        val hour = now.hour
        if (hebrew) {
            val timeStr = when {
                hour < 6 -> "עדיין מוקדם מאוד, העולם שקט."
                hour < 9 -> "הבוקר טרי ומלא הבטחה."
                hour < 12 -> "היום כבר בעיצומו."
                else -> "התחלה מאוחרת, אבל יש עוד זמן להשפיע."
            }
            return "היום $dateStr. $timeStr"
        }
        val timeStr = when {
            hour < 6 -> "It's still very early, the world is quiet."
            hour < 9 -> "The morning is fresh and full of promise."
            hour < 12 -> "The day is well underway now."
            else -> "A late start, but plenty of time to make an impact."
        }
        return "Today is $dateStr. $timeStr"
    }

    private fun getGreeting(persona: String, hebrew: Boolean = false): String {
        if (hebrew) return getGreetingHe(persona)
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

    private fun getGreetingHe(persona: String): String {
        val greetings = when (persona) {
            "COMEDIAN" -> listOf(
                "אה, בן האנוש התעורר. נס של ממש.",
                "תשומת לב: אדם חשוב ומעט מנומנם חזר לחיים.",
                "קום והתנער! או סתם קום. הרף נמוך בשעה כזו.",
                "הודעתי לתקשורת. אתה ער. אל תאכזב."
            )
            "ZEN" -> listOf(
                "השמש זורחת ומסע חדש מתחיל. נשום עמוק.",
                "שלום עליך. העולם מחכה לנוכחות המודעת שלך.",
                "התעורר בכוונה. הנח לשקט הבוקר להנחות אותך.",
                "נשום את היום החדש. אתה בדיוק איפה שצריך."
            )
            "HYPEMAN" -> listOf(
                "בוא ננצח! יום חדש, יעדים חדשים! בום!",
                "התעורר אלוף! העולם לא מוכן לאנרגיה שלך היום!",
                "בום! קום ותתחיל! זמן לעשות היסטוריה!",
                "בוא נכבוש הכל! האגדה התעוררה!"
            )
            else -> listOf(
                "קום והארץ! זמן לכבוש את העולם.",
                "בוקר טוב! הפוטנציאל שלך בלתי מוגבל היום.",
                "התעורר אלוף! הניצחון אוהב את מי שזז.",
                "הדלק מנועים! היום הולך להיות מדהים.",
                "היי! העולם טוב יותר איתך.",
                "ער ומוכן! בוא נע�שה את היום משמעותי.",
                "בוקר טוב! אנרגיה רעננה, הזדמנויות חדשות. קדימה!"
            )
        }
        return greetings.random()
    }

    private fun getWeatherIntro(persona: String, location: String, weather: String, hebrew: Boolean = false): String {
        if (hebrew) return getWeatherIntroHe(persona, location, weather)
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

    private fun getWeatherIntroHe(persona: String, location: String, weather: String): String {
        val intros = when (persona) {
            "COMEDIAN" -> listOf(
                "ב$location יש $weather. יפה? אני בוט, אין לי מושג.",
                "כרגע ב$location: $weather. או מה שאני קורא לו 'סיבה 42 להישאר במיטה'.",
                "האטמוספירה ב$location: $weather. תכנן את ה outfit בהתאם."
            )
            "ZEN" -> listOf(
                "האטמוספירה ב$location משקפת $weather. התאם עצמך אליה.",
                "ב$location השמיים מספרים $weather. כל מזג אוויר הוא מורה.",
                "היסודות ב$location בשלווה, $weather. צעד בעדינות היום."
            )
            "HYPEMAN" -> listOf(
                "תבדוק את הוויב! ב$location יש $weather! מזג מושלם למנצח!",
                "וואו! $location מביא $weather היום! נשתמש באנרגיה!",
                "בום! דוח מזג אוויר: $location — $weather! בלי תירוצים!"
            )
            else -> listOf(
                "כרגע $weather ב$location. התחלה טובה ליום טוב.",
                "התחזית ל$location: $weather. תנאים מושלמים להצלחה.",
                "ב$location היום מתחיל עם $weather. בוא ננצל את זה."
            )
        }
        return intros.random()
    }

    private fun getCalendarAdvice(persona: String, calendar: String, hebrew: Boolean = false): String {
        if (hebrew) return getCalendarAdviceHe(persona, calendar)
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

    private fun getCalendarAdviceHe(persona: String, calendar: String): String {
        val isBusy = !calendar.contains("clear") && !calendar.contains("No events")
        val advice = when (persona) {
            "COMEDIAN" -> if (isBusy) listOf(
                "היומן שלך נראה כמו טטריס ברמה 99. $calendar. נסה לא לבכות.",
                "יש לך התחייבויות. $calendar. לעבוד יש.",
                "היומן שופט אותך. $calendar. עדיף להתחיל עם קפה."
            ) else listOf(
                "היומן ריק. סוף סוף תוכל להתמקד בעצמך.",
                "אין פגישות? אני בהלם. תהנה מהחופש.",
                "יומן ריק. אל תמלא אותו ב-productivity."
            )
            "ZEN" -> if (isBusy) listOf(
                "הדרך מלאה היום. $calendar. זרום כמו מים.",
                "הרבה הזדמנויות להיות נוכח. $calendar. כל אירוע הוא נשימה.",
                "היומן מראה פעילות. $calendar. מצא את השקט במרכז."
            ) else listOf(
                "היקום נותן מרחב פנוי. השתמש בו למצוא את המרכז.",
                "יום של שקט ביומן. התבונן בדממה.",
                "אין דרישות מתוזמנות. תן לאינטואיציה להנחות."
            )
            "HYPEMAN" -> if (isBusy) listOf(
                "היומן מתפוצץ! $calendar. מצב שליטה!",
                "מלא! $calendar. אתה הולך לשלוט בכל פגישה!",
                "תסתכל על הלו\"ז! $calendar. ה highlight reel מחכה!"
            ) else listOf(
                "קנבס ריק! היום תבנה את האימפריה!",
                "חופש מוחלט! השתמש ביום לעשות משהו גדול!",
                "אין פגישות? יותר זמן לניצחון! קדימה!"
            )
            else -> if (isBusy) listOf(
                "הלו\"ז להיום: $calendar. בוא נטפל בזה בריכוז.",
                "יום פרודוקטיבי לפניך. $calendar. תישאר יעיל.",
                "אג\'נדה מלאה. $calendar. צעד אחר צעד, תצליח."
            ) else listOf(
                "היומן פתוח. הזדמנות להתקדם.",
                "אין אירועים היום. יום של התקדמות מכוונת.",
                "לו\"ז פנוי היום. התמקד ביעדים העיקריים."
            )
        }
        return advice.random()
    }

    private fun getFactTransition(persona: String, fact: String, hebrew: Boolean = false): String {
        if (hebrew) return getFactTransitionHe(persona, fact)
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

    private fun getFactTransitionHe(persona: String, fact: String): String {
        val transitions = when (persona) {
            "COMEDIAN" -> listOf(
                "אגב, ידעת? $fact. ידע זה כוח, כנראה.",
                "הנה עובדה מיותרת למוח: $fact. בבקשה.",
                "מצאתי את זה בפח של האינטרנט: $fact. נהדר, לא?"
            )
            "ZEN" -> listOf(
                "חתיכת חוכמת עולם: $fact. הכל מחובר.",
                "במארג החיים, שקול: $fact. פלאים קטנים בכל מקום.",
                "תצפית מודעת: $fact. הרהר במקום שלה בעולם."
            )
            "HYPEMAN" -> listOf(
                "המוח מתפוצץ! ידעת ש$fact?! מטורף!",
                "טריוויה מטורפת! $fact. תשתמש בזה להרשים היום!",
                "תקשיב! $fact. לומדים משהו חדש כל יום בדרך לפסגה!"
            )
            else -> listOf(
                "עובדה מעניינת להיום: $fact. קצת חוכמה לבוקר.",
                "ידעת? $fact. משהו לחשוב עליו כשמתחילים את היום.",
                "הטריוויה של הבוקר: $fact. ידע הוא המפתח להצלחה."
            )
        }
        return transitions.random()
    }

    private fun getMission(persona: String, hebrew: Boolean = false): String {
        if (hebrew) return getMissionHe(persona)
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

    private fun getMissionHe(persona: String): String {
        val missions = when (persona) {
            "COMEDIAN" -> listOf(
                "המשימה: תשבח את הנעליים של זר. הם יהיו מבולבלים, אתה מוקסם.",
                "משימה להיום: לשתות מים לפני קפה. הגוף יודה לי, המוח ישנא.",
                "המשימה: לא לבדוק אימייל עד שאכלת. תהיה מורד.",
                "הוראה אחרונה: נסה לא להיתקל בקירות."
            )
            "ZEN" -> listOf(
                "המשימה: חמש נשימות עמוקות לפני המשימה הראשונה.",
                "משימה להיום: לחייך לזר. התבונן בגלי החמלה.",
                "היום: מצא רגע אחד של דממה מוחלטת.",
                "לך במודעות היום. המסע הוא היעד."
            )
            "HYPEMAN" -> listOf(
                "משימה: HIGH-FIVE למראה! אתה אגדה! קדימה!",
                "היעד להיום: 10 jumping jacks עכשיו! התעורר!",
                "המשימה: לרסק את המשימה הכי קשה ראשון! ניצחון!",
                "בוא ננצח! תעשה את היום הכי טוב בשנה! בום!"
            )
            else -> listOf(
                "המשימה: להציב יעד אחד ברור ולהשיג אותו עד הצהריים.",
                "משימה להיום: לשבח עמית או חבר. אנרגיה מדבקת.",
                "היעד להיום: ללמוד משהו חדש, ולו קטן.",
                "תעשה את היום משמעותי. אתה יכול."
            )
        }
        return missions.random()
    }
}
