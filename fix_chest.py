with open("app/src/main/java/com/focusbyrj/app/ui/components/DuolingoMysteryChestDialog.kt", "r") as f:
    content = f.read()

import re

# Balance Chests
content = content.replace("goldEarned = 1000\n                        AptitudeManager.activateXpBoost(durationMinutes = 15, multiplier = 2.0f)", "goldEarned = 50\n                        AptitudeManager.activateXpBoost(durationMinutes = 15, multiplier = 2.0f)")
content = content.replace("xpEarned = maxOf(1000, (currentTotalXp * 0.05f).toInt())\n                        goldEarned = 10000", "xpEarned = maxOf(100, (currentTotalXp * 0.02f).toInt())\n                        goldEarned = 250")
content = content.replace("bonusGold = 1000", "bonusGold = 100")
content = content.replace("xpEarned = maxOf(2000, (currentTotalXp * 0.10f).toInt())\n                        goldEarned = 50000", "xpEarned = maxOf(250, (currentTotalXp * 0.05f).toInt())\n                        goldEarned = 1000")
content = content.replace("bonusGold = 2000", "bonusGold = 250")
content = content.replace("xpEarned = maxOf(5000, (currentTotalXp * 0.20f).toInt())\n                        goldEarned = 100000", "xpEarned = maxOf(1000, (currentTotalXp * 0.10f).toInt())\n                        goldEarned = 5000")
content = content.replace("bonusGold = 5000", "bonusGold = 500")

# Fix Double XP and economy issue
old_economy = """                // Save to economy
                if (xpEarned > 0) {
                    AptitudeManager.addAptitudeXp(xpEarned)
                }
                FocusEconomyManager.addRewards(baseXp = xpEarned, baseGold = finalGold)"""

new_economy = """                // Save EXACT rewards to pending so UI matches the earned amount
                FocusEconomyManager.addExactRewards(exactXp = xpEarned, exactGold = finalGold)"""

content = content.replace(old_economy, new_economy)

with open("app/src/main/java/com/focusbyrj/app/ui/components/DuolingoMysteryChestDialog.kt", "w") as f:
    f.write(content)
