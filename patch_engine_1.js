const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/util/AyvaTalkEngine.kt';
let code = fs.readFileSync(path, 'utf8');

const targetStr = `        // --- 3. Semantic & Fuzzy Scoring ---
        var bestTopic: SpecificTopic? = null
        var highestScore = 0

        val queryWords = expandTokens(cleanQuery) // Using NLP Expanded Tokens

        for (topic in getTopicsDatabase(context)) {
            var score = 0
            val titleLower = topic.title.lowercase()
            val idLower = topic.id.lowercase()

            // Exact phrase match
            if (cleanQuery.contains(titleLower) || titleLower.contains(cleanQuery)) {
                score += 80
            }

            // Keyword hits
            for (kw in topic.keywords) {
                if (cleanQuery.contains(kw)) {
                    score += 45
                } else if (kw.contains(cleanQuery) && cleanQuery.length >= 4) {
                    score += 30
                } else {
                    // Check fuzzy match on expanded NLP query words vs keyword words
                    for (qWord in queryWords) {
                        for (kwWord in kw.split(" ")) {
                            if (isFuzzyMatchWord(qWord, kwWord)) {
                                score += 20
                            }
                        }
                    }
                }
            }

            // Expanded Word token match
            for (word in queryWords) {
                if (titleLower.contains(word)) score += 15
                if (idLower.contains(word)) score += 10
                if (isFuzzyMatchWord(word, idLower)) score += 8
            }

            if (score > highestScore) {
                highestScore = score
                bestTopic = topic
            }
        }`;

const replacementStr = `        // --- 3. Semantic & Fuzzy Scoring ---
        val scoredTopics = mutableListOf<Pair<SpecificTopic, Int>>()
        val queryWords = expandTokens(cleanQuery)

        for (topic in getTopicsDatabase(context)) {
            var score = 0
            val titleLower = topic.title.lowercase()
            val idLower = topic.id.lowercase()

            if (cleanQuery.contains(titleLower) || titleLower.contains(cleanQuery)) {
                score += 80
            }

            for (kw in topic.keywords) {
                if (cleanQuery.contains(kw)) {
                    score += 45
                } else if (kw.contains(cleanQuery) && cleanQuery.length >= 4) {
                    score += 30
                } else {
                    for (qWord in queryWords) {
                        for (kwWord in kw.split(" ")) {
                            if (isFuzzyMatchWord(qWord, kwWord)) {
                                score += 20
                            }
                        }
                    }
                }
            }

            for (word in queryWords) {
                if (titleLower.contains(word)) score += 15
                if (idLower.contains(word)) score += 10
                if (isFuzzyMatchWord(word, idLower)) score += 8
            }

            if (score > 0) {
                scoredTopics.add(Pair(topic, score))
            }
        }
        
        scoredTopics.sortByDescending { it.second }
        var bestTopic = scoredTopics.firstOrNull()?.first
        var highestScore = scoredTopics.firstOrNull()?.second ?: 0
        
        // --- 3.5 Disambiguation ---
        // If the score is weak or there are multiple close candidates
        if (bestTopic != null && highestScore < 50 && scoredTopics.size > 1) {
            val secondScore = scoredTopics[1].second
            if (highestScore - secondScore < 20) {
                val suggestions = scoredTopics.take(3).map { it.first }
                val actions = suggestions.map { TalkAction.AskQuery(it.title) }
                return TalkResponse(
                    formattedText = "🤔 I found a few settings that sound similar. Which one did you mean?",
                    actions = actions
                )
            }
        }`;

code = code.replace(targetStr, replacementStr);
fs.writeFileSync(path, code);
